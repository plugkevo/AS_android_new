package com.example.africanshipping25

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var messaging: FirebaseMessaging
    private lateinit var sharedPreferences: SharedPreferences

    // Translation components
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

    // UI Elements
    private lateinit var profilePicture: CircleImageView
    private lateinit var editProfilePicture: ImageView
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var userRole: TextView
    private lateinit var totalShipments: TextView
    private lateinit var activeShipments: TextView
    private lateinit var deliveredShipments: TextView
    private lateinit var notificationsSwitch: SwitchCompat
    private lateinit var currentLanguage: TextView
    private lateinit var currentTheme: TextView

    private var currentPhotoPath: String? = null

    companion object {
        private const val TAG = "ProfileFragment"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    // Permission launcher for notifications (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableNotifications()
        } else {
            // Reset switch if permission denied
            notificationsSwitch.isChecked = false
            showTranslatedToast("Notification permission is required to enable notifications")
        }
    }

    // Permission launcher for camera and storage
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        if (cameraGranted && storageGranted) {
            showImagePickerDialog()
        } else {
            showTranslatedToast("Permissions required to access camera and gallery")
        }
    }

    // Image picker launchers
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                displayImage(it)
                uploadImageToFirebase(it)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                val uri = Uri.fromFile(file)
                displayImage(uri)
                uploadImageToFirebase(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Handle any arguments passed to the fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        messaging = FirebaseMessaging.getInstance()
        sharedPreferences = requireContext().getSharedPreferences("app_preferences", 0)

        // Initialize translation
        translationManager = GoogleTranslationManager(requireContext())
        translationHelper = GoogleTranslationHelper(translationManager)

        // Initialize UI elements
        initializeViews(view)

        // Load user data
        loadUserData()

        // Set up click listeners
        setupClickListeners()

        // Load preferences
        loadPreferences()

        // Initialize FCM
        initializeFirebaseMessaging()

        // Translate UI elements based on current language
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translateUIElements(currentLanguage)
    }

    private fun initializeViews(view: View) {
        profilePicture = view.findViewById(R.id.iv_profile_picture)
        editProfilePicture = view.findViewById(R.id.iv_edit_profile_picture)
        userName = view.findViewById(R.id.tv_user_name)
        userEmail = view.findViewById(R.id.tv_user_email)
        userRole = view.findViewById(R.id.tv_user_role)

        notificationsSwitch = view.findViewById(R.id.switch_notifications)
        currentLanguage = view.findViewById(R.id.tv_current_language)
        currentTheme = view.findViewById(R.id.tv_current_theme)
    }

    private fun translateUIElements(targetLanguage: String) {
        view?.let { v ->
            // Translate section headers (you'll need to add these IDs to your XML)
            v.findViewById<TextView>(R.id.tv_account_section_header)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Account Information", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_settings_section_header)?.let { textView ->
                translationHelper.translateAndSetText(textView, "App Settings", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_support_section_header)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Support & Information", targetLanguage)
            }

            // Translate menu items (you'll need to add these IDs to your XML)
            v.findViewById<TextView>(R.id.tv_edit_profile_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Edit Profile", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_change_password_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Change Password", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_address_book_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Address Book", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_notifications_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Notifications", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_language_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Language", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_theme_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Theme", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_help_support_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Help & Support", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_privacy_policy_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Privacy Policy", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_terms_service_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Terms of Service", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_about_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "About", targetLanguage)
            }

            // Translate descriptions
            v.findViewById<TextView>(R.id.tv_edit_profile_desc)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Update your personal information", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_change_password_desc)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Update your account password", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_address_book_desc)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Manage your saved addresses", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_notifications_desc)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Manage notification preferences", targetLanguage)
            }

            // Translate shipment stats labels
            v.findViewById<TextView>(R.id.tv_total_shipments_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Total Shipments", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_active_shipments_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Active", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_delivered_shipments_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Delivered", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_version_desc)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Version 1.0.0", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_app_version)?.let { textView ->
                translationHelper.translateAndSetText(textView, "African Shipping v1.0.0", targetLanguage)
            }
        }
    }

    private fun setupClickListeners() {
        // Profile picture edit
        editProfilePicture.setOnClickListener {
            checkPermissionsAndShowImagePicker()
        }

        // Profile picture click
        profilePicture.setOnClickListener {
            checkPermissionsAndShowImagePicker()
        }

        // Edit Profile - Launch the Edit Profile Dialog
        view?.findViewById<View>(R.id.layout_edit_profile)?.setOnClickListener {
            val editProfileDialog = EditProfileDialogFragment()
            editProfileDialog.setOnProfileUpdatedListener(object : EditProfileDialogFragment.OnProfileUpdatedListener {
                override fun onProfileUpdated() {
                    loadUserData()
                    showTranslatedToast("Profile updated successfully!")
                }
            })
            editProfileDialog.show(parentFragmentManager, "EditProfileDialog")
        }

        // Change Password
        view?.findViewById<View>(R.id.layout_change_password)?.setOnClickListener {
            showChangePasswordDialog()
        }

        // Address Book
        view?.findViewById<View>(R.id.layout_address_book)?.setOnClickListener {
            showTranslatedToast("Address Book clicked")
        }

        // Notifications toggle - ENHANCED with FCM integration
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleNotificationToggle(isChecked)
        }

        // Language
        view?.findViewById<View>(R.id.layout_language)?.setOnClickListener {
            showLanguageDialog()
        }

        // Theme
        view?.findViewById<View>(R.id.layout_theme)?.setOnClickListener {
            showThemeDialog()
        }

        // Help & Support
        view?.findViewById<View>(R.id.layout_help)?.setOnClickListener {
            showHelpAndSupportDialog()
        }

        // Privacy Policy
        view?.findViewById<View>(R.id.layout_privacy)?.setOnClickListener {
            showPrivacyPolicyDialog()
        }

        // Terms of Service
        view?.findViewById<View>(R.id.layout_terms)?.setOnClickListener {
            showTranslatedToast("Terms of Service clicked")
        }

        // About
        view?.findViewById<View>(R.id.layout_about)?.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun initializeFirebaseMessaging() {
        // Get FCM token
        messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Registration Token: $token")

            // Save token to Firestore for the current user
            saveTokenToFirestore(token)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            userRef.update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving FCM token", e)
                }
        }
    }

    private fun handleNotificationToggle(isChecked: Boolean) {
        if (isChecked) {
            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Request notification permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
            enableNotifications()
        } else {
            disableNotifications()
        }
    }

    private fun enableNotifications() {
        // Subscribe to general notifications topic
        messaging.subscribeToTopic("general_notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to general notifications")

                    // Subscribe to user-specific topics
                    val currentUser = auth.currentUser
                    currentUser?.let { user ->
                        messaging.subscribeToTopic("user_${user.uid}")
                            .addOnCompleteListener { userTask ->
                                if (userTask.isSuccessful) {
                                    Log.d(TAG, "Subscribed to user-specific notifications")
                                }
                            }
                    }

                    // Save preference
                    saveNotificationPreference(true)

                    // Update Firestore
                    updateNotificationPreferenceInFirestore(true)

                    showTranslatedToast("Notifications enabled")
                } else {
                    Log.e(TAG, "Failed to subscribe to notifications", task.exception)
                    notificationsSwitch.isChecked = false
                    showTranslatedToast("Failed to enable notifications")
                }
            }
    }

    private fun disableNotifications() {
        // Unsubscribe from general notifications topic
        messaging.unsubscribeFromTopic("general_notifications")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from general notifications")

                    // Unsubscribe from user-specific topics
                    val currentUser = auth.currentUser
                    currentUser?.let { user ->
                        messaging.unsubscribeFromTopic("user_${user.uid}")
                            .addOnCompleteListener { userTask ->
                                if (userTask.isSuccessful) {
                                    Log.d(TAG, "Unsubscribed from user-specific notifications")
                                }
                            }
                    }

                    // Save preference
                    saveNotificationPreference(false)

                    // Update Firestore
                    updateNotificationPreferenceInFirestore(false)

                    showTranslatedToast("Notifications disabled")
                } else {
                    Log.e(TAG, "Failed to unsubscribe from notifications", task.exception)
                    notificationsSwitch.isChecked = true
                    showTranslatedToast("Failed to disable notifications")
                }
            }
    }

    private fun updateNotificationPreferenceInFirestore(enabled: Boolean) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            userRef.update("notificationsEnabled", enabled)
                .addOnSuccessListener {
                    Log.d(TAG, "Notification preference updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating notification preference", e)
                }
        }
    }

    private fun checkPermissionsAndShowImagePicker() {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val storagePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)

        val permissionsToRequest = mutableListOf<String>()

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())

        translationHelper.translateText("Select Profile Picture", currentLanguage) { translatedTitle ->
            builder.setTitle(translatedTitle)
        }

        // Translate options
        val translatedOptions = arrayOfNulls<String>(3)
        var translationCount = 0

        translationHelper.translateText("Take Photo", currentLanguage) { translated ->
            translatedOptions[0] = translated
            translationCount++
            if (translationCount == 3) showDialogWithTranslatedOptions(builder, translatedOptions)
        }

        translationHelper.translateText("Choose from Gallery", currentLanguage) { translated ->
            translatedOptions[1] = translated
            translationCount++
            if (translationCount == 3) showDialogWithTranslatedOptions(builder, translatedOptions)
        }

        translationHelper.translateText("Cancel", currentLanguage) { translated ->
            translatedOptions[2] = translated
            translationCount++
            if (translationCount == 3) showDialogWithTranslatedOptions(builder, translatedOptions)
        }
    }

    private fun showDialogWithTranslatedOptions(builder: androidx.appcompat.app.AlertDialog.Builder, options: Array<String?>) {
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> openCamera()
                1 -> openGallery()
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.let {
                val photoURI = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(intent)
            }
        } else {
            showTranslatedToast("Camera not available")
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = File(requireContext().getExternalFilesDir(null), "Pictures")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val image = File.createTempFile(imageFileName, ".jpg", storageDir)
            currentPhotoPath = image.absolutePath
            image
        } catch (e: Exception) {
            e.printStackTrace()
            showTranslatedToast("Error creating image file")
            null
        }
    }

    private fun displayImage(uri: Uri) {
        try {
            Glide.with(this)
                .load(uri)
                .transform(CircleCrop())
                .placeholder(R.drawable.default_profile_picture)
                .error(R.drawable.default_profile_picture)
                .into(profilePicture)
        } catch (e: Exception) {
            e.printStackTrace()
            showTranslatedToast("Error loading image")
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val storageRef = storage.reference
            val profileImagesRef = storageRef.child("profile_images/${user.uid}.jpg")

            // Show loading
            showTranslatedToast("Uploading image...")

            profileImagesRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    // Get download URL
                    profileImagesRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Save URL to Firestore
                        val userRef = firestore.collection("users").document(user.uid)
                        userRef.update("profilePictureUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                showTranslatedToast("Profile picture updated!")
                            }
                            .addOnFailureListener { e ->
                                showTranslatedToast("Error saving image URL: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    showTranslatedToast("Upload failed: ${e.message}")
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    // You can show progress here if needed
                }
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            userName.text = user.displayName ?: "User Name"
            userEmail.text = user.email ?: "user@example.com"

            // Load additional user data from Firestore
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userRole.text = document.getString("role") ?: "Customer"

                        // Update display name if we have first and last name
                        val firstName = document.getString("firstName")
                        val lastName = document.getString("lastName")
                        if (!firstName.isNullOrEmpty() && !lastName.isNullOrEmpty()) {
                            userName.text = "$firstName $lastName"
                        }

                        // Load profile picture
                        val profilePictureUrl = document.getString("profilePictureUrl")
                        if (!profilePictureUrl.isNullOrEmpty()) {
                            loadProfileImage(profilePictureUrl)
                        } else {
                            // Set default image
                            profilePicture.setImageResource(R.drawable.default_profile_picture)
                        }

                        // Load notification preference from Firestore
                        val notificationsEnabled = document.getBoolean("notificationsEnabled") ?: true
                        // Update local preference if different
                        if (notificationsEnabled != sharedPreferences.getBoolean("notifications_enabled", true)) {
                            saveNotificationPreference(notificationsEnabled)
                            notificationsSwitch.isChecked = notificationsEnabled
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    showTranslatedToast("Error loading profile: ${exception.message}")
                    profilePicture.setImageResource(R.drawable.default_profile_picture)
                }
        }

        loadShipmentStats()
    }

    private fun loadProfileImage(imageUrl: String) {
        try {
            Glide.with(this)
                .load(imageUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.default_profile_picture)
                .error(R.drawable.default_profile_picture)
                .into(profilePicture)
        } catch (e: Exception) {
            e.printStackTrace()
            profilePicture.setImageResource(R.drawable.default_profile_picture)
        }
    }

    private fun loadShipmentStats() {
        firestore.collection("shipments")
            .get()
            .addOnSuccessListener { documents ->
                var total = 0
                var active = 0
                var delivered = 0

                for (document in documents) {
                    total++
                    when (document.getString("status")) {
                        "In Transit", "Processing", "Picked Up", "Pending", "Out for Delivery" -> active++
                        "Delivered" -> delivered++
                    }
                }

                // Find the TextViews and update them
                view?.findViewById<TextView>(R.id.tv_total_shipments)?.text = total.toString()
                view?.findViewById<TextView>(R.id.tv_active_shipments)?.text = active.toString()
                view?.findViewById<TextView>(R.id.tv_delivered_shipments)?.text = delivered.toString()
            }
            .addOnFailureListener { exception ->
                // Handle error case
                view?.findViewById<TextView>(R.id.tv_total_shipments)?.text = "0"
                view?.findViewById<TextView>(R.id.tv_active_shipments)?.text = "0"
                view?.findViewById<TextView>(R.id.tv_delivered_shipments)?.text = "0"

                showTranslatedToast("Error loading shipment stats: ${exception.message}")
            }
    }

    private fun loadPreferences() {
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        notificationsSwitch.isChecked = notificationsEnabled

        val language = sharedPreferences.getString("language", "English")
        currentLanguage.text = language

        val theme = sharedPreferences.getString("theme", "Light")
        currentTheme.text = theme
    }

    private fun saveNotificationPreference(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("notifications_enabled", enabled)
            .apply()
    }

    private fun showChangePasswordDialog() {
        val dialog = ChangePasswordDialogFragment()
        dialog.show(parentFragmentManager, "ChangePasswordDialog")
    }

    private fun showLanguageDialog() {
        val languageDialog = LanguageSelectionDialogFragment()
        languageDialog.setOnLanguageSelectedListener(object : LanguageSelectionDialogFragment.OnLanguageSelectedListener {
            override fun onLanguageSelected(language: String) {
                // Update the UI
                currentLanguage.text = language

                // Clear translation cache
                translationHelper.clearCache()

                // Retranslate UI elements
                translateUIElements(language)

                // Refresh the profile to reflect changes
                refreshProfile()
            }
        })
        languageDialog.show(parentFragmentManager, "LanguageSelectionDialog")
    }

    private fun showThemeDialog() {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        val themes = arrayOf("Light", "Dark", "System Default")
        val currentTheme = sharedPreferences.getString("theme", "System Default")
        var selectedIndex = themes.indexOf(currentTheme)

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())

        translationHelper.translateText("Select Theme", currentLanguage) { translatedTitle ->
            builder.setTitle(translatedTitle)
        }

        // Translate theme options
        val translatedThemes = arrayOfNulls<String>(3)
        var translationCount = 0

        translationHelper.translateText("Light", currentLanguage) { translated ->
            translatedThemes[0] = translated
            translationCount++
            if (translationCount == 3) showThemeDialogWithTranslations(builder, translatedThemes, themes, selectedIndex)
        }

        translationHelper.translateText("Dark", currentLanguage) { translated ->
            translatedThemes[1] = translated
            translationCount++
            if (translationCount == 3) showThemeDialogWithTranslations(builder, translatedThemes, themes, selectedIndex)
        }

        translationHelper.translateText("System Default", currentLanguage) { translated ->
            translatedThemes[2] = translated
            translationCount++
            if (translationCount == 3) showThemeDialogWithTranslations(builder, translatedThemes, themes, selectedIndex)
        }
    }

    private fun showThemeDialogWithTranslations(
        builder: androidx.appcompat.app.AlertDialog.Builder,
        translatedThemes: Array<String?>,
        originalThemes: Array<String>,
        selectedIndex: Int
    ) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        var currentSelectedIndex = selectedIndex

        builder.setSingleChoiceItems(translatedThemes, selectedIndex) { _, which ->
            currentSelectedIndex = which
        }

        translationHelper.translateText("OK", currentLanguage) { okText ->
            translationHelper.translateText("Cancel", currentLanguage) { cancelText ->
                builder.setPositiveButton(okText) { dialog, _ ->
                    val selectedTheme = originalThemes[currentSelectedIndex]
                    this.currentTheme.text = selectedTheme
                    sharedPreferences.edit()
                        .putString("theme", selectedTheme)
                        .apply()

                    // Apply theme immediately
                    applyTheme(selectedTheme)

                    showTranslatedToast("Theme changed to $selectedTheme")
                }
                builder.setNegativeButton(cancelText, null)
                builder.show()
            }
        }
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "Light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "Dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            "System Default" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun loadSavedTheme() {
        val savedTheme = sharedPreferences.getString("theme", "System Default")
        savedTheme?.let { applyTheme(it) }
    }

    private fun showHelpAndSupportDialog() {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        val options = arrayOf("FAQ", "Contact Support", "User Guide", "Report a Problem")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())

        translationHelper.translateText("Help & Support", currentLanguage) { translatedTitle ->
            builder.setTitle(translatedTitle)
        }

        // Translate options
        val translatedOptions = arrayOfNulls<String>(4)
        var translationCount = 0

        translationHelper.translateText("FAQ", currentLanguage) { translated ->
            translatedOptions[0] = translated
            translationCount++
            if (translationCount == 4) showHelpDialogWithTranslations(builder, translatedOptions)
        }

        translationHelper.translateText("Contact Support", currentLanguage) { translated ->
            translatedOptions[1] = translated
            translationCount++
            if (translationCount == 4) showHelpDialogWithTranslations(builder, translatedOptions)
        }

        translationHelper.translateText("User Guide", currentLanguage) { translated ->
            translatedOptions[2] = translated
            translationCount++
            if (translationCount == 4) showHelpDialogWithTranslations(builder, translatedOptions)
        }

        translationHelper.translateText("Report a Problem", currentLanguage) { translated ->
            translatedOptions[3] = translated
            translationCount++
            if (translationCount == 4) showHelpDialogWithTranslations(builder, translatedOptions)
        }
    }

    private fun showHelpDialogWithTranslations(builder: androidx.appcompat.app.AlertDialog.Builder, options: Array<String?>) {
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showTranslatedToast("FAQ selected")
                1 -> showContactSupportDialog()
                2 -> showTranslatedToast("User Guide selected")
                3 -> showReportProblemDialog()
            }
        }
        builder.show()
    }

    private fun showContactSupportDialog() {
        val dialog = SendEmailDialogFragment()
        dialog.show(parentFragmentManager, "SendEmailDialog")
    }

    private fun showPrivacyPolicyDialog() {
        val dialog = PrivacyPolicyDialogFragment()
        dialog.show(parentFragmentManager, "PrivacyPolicyDialog")
    }

    private fun showReportProblemDialog() {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())

        translationHelper.translateText("Report a Problem", currentLanguage) { translatedTitle ->
            builder.setTitle(translatedTitle)
        }

        translationHelper.translateText("Please describe the issue you're experiencing:", currentLanguage) { translatedMessage ->
            builder.setMessage(translatedMessage)
        }

        val input = android.widget.EditText(requireContext())
        translationHelper.translateText("Describe the problem...", currentLanguage) { translatedHint ->
            input.hint = translatedHint
        }
        input.minLines = 3
        builder.setView(input)

        translationHelper.translateText("Submit", currentLanguage) { submitText ->
            translationHelper.translateText("Cancel", currentLanguage) { cancelText ->
                builder.setPositiveButton(submitText) { _, _ ->
                    val problemDescription = input.text.toString()
                    if (problemDescription.isNotEmpty()) {
                        showTranslatedToast("Problem report submitted. Thank you!")
                    } else {
                        showTranslatedToast("Please describe the problem")
                    }
                }
                builder.setNegativeButton(cancelText, null)
                builder.show()
            }
        }
    }

    private fun showAboutDialog() {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())

        translationHelper.translateText("About African Shipping", currentLanguage) { translatedTitle ->
            builder.setTitle(translatedTitle)
        }

        val aboutMessage = """
            African Shipping App
            Version 1.4.0
            
            Your trusted partner for shipping and logistics across Africa.
            
            © 2024 African Shipping Company
            All rights reserved.
            
            Built with ❤️ for Africa
        """.trimIndent()

        translationHelper.translateText(aboutMessage, currentLanguage) { translatedMessage ->
            builder.setMessage(translatedMessage)
        }

        translationHelper.translateText("OK", currentLanguage) { okText ->
            builder.setPositiveButton(okText, null)
            builder.show()
        }
    }

    // Helper method to translate toast messages
    private fun showTranslatedToast(message: String) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translationHelper.translateText(message, currentLanguage) { translatedMessage ->
            Toast.makeText(context, translatedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshProfile() {
        loadUserData()
        loadPreferences()
    }

    override fun onResume() {
        super.onResume()
        refreshProfile()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::translationManager.isInitialized) {
            translationManager.cleanup()
        }
    }
}