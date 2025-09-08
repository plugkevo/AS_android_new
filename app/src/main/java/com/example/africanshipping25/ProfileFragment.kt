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
            Toast.makeText(context, "Notification permission is required to enable notifications", Toast.LENGTH_LONG).show()
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
            Toast.makeText(context, "Permissions required to access camera and gallery", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Address Book clicked", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Terms of Service clicked", Toast.LENGTH_SHORT).show()
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

                    Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Failed to subscribe to notifications", task.exception)
                    notificationsSwitch.isChecked = false
                    Toast.makeText(context, "Failed to enable notifications", Toast.LENGTH_SHORT).show()
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

                    Toast.makeText(context, "Notifications disabled", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Failed to unsubscribe from notifications", task.exception)
                    notificationsSwitch.isChecked = true
                    Toast.makeText(context, "Failed to disable notifications", Toast.LENGTH_SHORT).show()
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
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Profile Picture")
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
            Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Error creating image file", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val storageRef = storage.reference
            val profileImagesRef = storageRef.child("profile_images/${user.uid}.jpg")

            // Show loading
            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()

            profileImagesRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    // Get download URL
                    profileImagesRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Save URL to Firestore
                        val userRef = firestore.collection("users").document(user.uid)
                        userRef.update("profilePictureUrl", downloadUri.toString())
                            .addOnSuccessListener {
                                Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error saving image URL: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
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

                Toast.makeText(context, "Error loading shipment stats: ${exception.message}", Toast.LENGTH_SHORT).show()
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
        val languages = arrayOf("English", "French", "Spanish", "Portuguese", "Arabic", "Swahili")
        val currentLanguage = sharedPreferences.getString("language", "English")
        var selectedIndex = languages.indexOf(currentLanguage)

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Language")
        builder.setSingleChoiceItems(languages, selectedIndex) { _, which ->
            selectedIndex = which
        }
        builder.setPositiveButton("OK") { _, _ ->
            val selectedLanguage = languages[selectedIndex]
            this.currentLanguage.text = selectedLanguage
            sharedPreferences.edit()
                .putString("language", selectedLanguage)
                .apply()
            Toast.makeText(context, "Language changed to $selectedLanguage", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Light", "Dark", "System Default")
        val currentTheme = sharedPreferences.getString("theme", "System Default")
        var selectedIndex = themes.indexOf(currentTheme)

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Theme")
        builder.setSingleChoiceItems(themes, selectedIndex) { _, which ->
            selectedIndex = which
        }
        builder.setPositiveButton("OK") { _, _ ->
            val selectedTheme = themes[selectedIndex]
            this.currentTheme.text = selectedTheme
            sharedPreferences.edit()
                .putString("theme", selectedTheme)
                .apply()

            // Apply theme immediately
            applyTheme(selectedTheme)

            Toast.makeText(context, "Theme changed to $selectedTheme", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
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
        val options = arrayOf("FAQ", "Contact Support", "User Guide", "Report a Problem")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Help & Support")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> Toast.makeText(context, "FAQ selected", Toast.LENGTH_SHORT).show()
                1 -> showContactSupportDialog()
                2 -> Toast.makeText(context, "User Guide selected", Toast.LENGTH_SHORT).show()
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
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Report a Problem")
        builder.setMessage("Please describe the issue you're experiencing:")

        val input = android.widget.EditText(requireContext())
        input.hint = "Describe the problem..."
        input.minLines = 3
        builder.setView(input)

        builder.setPositiveButton("Submit") { _, _ ->
            val problemDescription = input.text.toString()
            if (problemDescription.isNotEmpty()) {
                Toast.makeText(context, "Problem report submitted. Thank you!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Please describe the problem", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showAboutDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("About African Shipping")
        builder.setMessage("""
            African Shipping App
            Version 1.0.0
            
            Your trusted partner for shipping and logistics across Africa.
            
            © 2024 African Shipping Company
            All rights reserved.
            
            Built with ❤️ for Africa
        """.trimIndent())
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    fun refreshProfile() {
        loadUserData()
        loadPreferences()
    }

    override fun onResume() {
        super.onResume()
        refreshProfile()
    }
}