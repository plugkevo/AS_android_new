package com.kevann.africanshipping25.profile

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.kevann.africanshipping25.R  // Add this import


class EditProfileDialogFragment : DialogFragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI Elements
    private lateinit var profilePicture: CircleImageView
    private lateinit var changePhoto: TextView
    private lateinit var firstNameInput: TextInputEditText
    private lateinit var lastNameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var dateOfBirthInput: TextInputEditText
    private lateinit var genderInput: AutoCompleteTextView
    private lateinit var streetAddressInput: TextInputEditText
    private lateinit var cityInput: TextInputEditText
    private lateinit var stateInput: TextInputEditText
    private lateinit var zipCodeInput: TextInputEditText
    private lateinit var countryInput: AutoCompleteTextView
    private lateinit var companyNameInput: TextInputEditText
    private lateinit var jobTitleInput: TextInputEditText
    private lateinit var btnClose: ImageButton
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null
    private val calendar = Calendar.getInstance()
    private val TAG = "EditProfileDialog"

    // Permission launcher
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
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                Log.d(TAG, "Gallery image selected: $it")
                displayImage(it)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                selectedImageUri = Uri.fromFile(file)
                selectedImageUri?.let {
                    Log.d(TAG, "Camera image captured: $it")
                    displayImage(it)
                }
            }
        }
    }

    interface OnProfileUpdatedListener {
        fun onProfileUpdated()
    }

    private var listener: OnProfileUpdatedListener? = null

    fun setOnProfileUpdatedListener(listener: OnProfileUpdatedListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI elements
        initializeViews(view)

        // Set up dropdowns
        setupDropdowns()

        // Load current user data
        loadCurrentUserData()

        // Set up click listeners
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full screen
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun initializeViews(view: View) {
        profilePicture = view.findViewById(R.id.iv_profile_picture_edit)
        changePhoto = view.findViewById(R.id.tv_change_photo)
        firstNameInput = view.findViewById(R.id.et_first_name)
        lastNameInput = view.findViewById(R.id.et_last_name)
        emailInput = view.findViewById(R.id.et_email)
        phoneInput = view.findViewById(R.id.et_phone)
        dateOfBirthInput = view.findViewById(R.id.et_date_of_birth)
        genderInput = view.findViewById(R.id.et_gender)
        streetAddressInput = view.findViewById(R.id.et_street_address)
        cityInput = view.findViewById(R.id.et_city)
        stateInput = view.findViewById(R.id.et_state)
        zipCodeInput = view.findViewById(R.id.et_zip_code)
        countryInput = view.findViewById(R.id.et_country)
        companyNameInput = view.findViewById(R.id.et_company_name)
        jobTitleInput = view.findViewById(R.id.et_job_title)
        btnClose = view.findViewById(R.id.btn_close)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSave = view.findViewById(R.id.btn_save)
    }

    private fun setupDropdowns() {
        // Gender dropdown
        val genderOptions = arrayOf("Male", "Female" ,"other", "Prefer not to say")
        val genderAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        genderInput.setAdapter(genderAdapter)

        val countryOptions = arrayOf(
            "Kenya",
            "United States", "Canada", "United Kingdom", "Germany", "France",
            "Spain", "Italy", "Netherlands", "Belgium", "Switzerland", "Australia",
            "New Zealand", "Japan", "South Korea", "Singapore", "India", "China",
            "Brazil", "Mexico", "South Africa", "Nigeria", "Ghana", "Egypt",
            "Turkey", "United Arab Emirates", "Saudi Arabia", "Argentina", "Chile",
            "Sweden", "Norway", "Denmark", "Finland", "Poland", "Portugal",
            "Greece", "Ireland", "Austria", "Czech Republic", "Hungary", "Russia",
            "Thailand", "Malaysia", "Philippines", "Vietnam", "Indonesia",
            "Colombia", "Peru", "Pakistan", "Bangladesh",
            "Other"
        )
        val countryAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryOptions)
        countryInput.setAdapter(countryAdapter)

        val countryCodes = arrayOf(
            "+254 (Kenya)", "+1 (United States)", "+1 (Canada)", "+44 (United Kingdom)",
            "+49 (Germany)", "+33 (France)", "+34 (Spain)", "+39 (Italy)", "+31 (Netherlands)",
            "+32 (Belgium)", "+41 (Switzerland)", "+61 (Australia)", "+64 (New Zealand)",
            "+81 (Japan)", "+82 (South Korea)", "+65 (Singapore)", "+91 (India)", "+86 (China)",
            "+55 (Brazil)", "+52 (Mexico)", "+27 (South Africa)", "+234 (Nigeria)",
            "+233 (Ghana)", "+20 (Egypt)", "+90 (Turkey)", "+971 (UAE)", "+966 (Saudi Arabia)",
            "+54 (Argentina)", "+56 (Chile)", "+46 (Sweden)", "+47 (Norway)", "+45 (Denmark)",
            "+358 (Finland)", "+48 (Poland)", "+351 (Portugal)", "+30 (Greece)", "+353 (Ireland)",
            "+43 (Austria)", "+420 (Czech Republic)", "+36 (Hungary)", "+7 (Russia)",
            "+66 (Thailand)", "+60 (Malaysia)", "+63 (Philippines)", "+84 (Vietnam)", "+62 (Indonesia)",
            "+57 (Colombia)", "+51 (Peru)", "+92 (Pakistan)", "+880 (Bangladesh)", "Other"
        )
        val codeAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryCodes)
        val countryCodeInput: AutoCompleteTextView = requireView().findViewById(R.id.et_country_code)
        countryCodeInput.setAdapter(codeAdapter)
    }

    private fun setupClickListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        changePhoto.setOnClickListener {
            checkPermissionsAndShowImagePicker()
        }

        // Also allow clicking on the profile picture itself
        profilePicture.setOnClickListener {
            checkPermissionsAndShowImagePicker()
        }

        dateOfBirthInput.setOnClickListener {
            showDatePicker()
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
        val builder = AlertDialog.Builder(requireContext())
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
            Log.d(TAG, "Displaying image: $uri")
            if (isAdded && !isDetached) {
                Glide.with(this)
                    .load(uri)
                    .apply(
                        RequestOptions()
                        .transform(CircleCrop())
                        .placeholder(R.drawable.default_profile_picture)
                        .error(R.drawable.default_profile_picture)
                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(profilePicture)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying image", e)
            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImage(imageUrl: String) {
        try {
            Log.d(TAG, "Loading profile image from URL: $imageUrl")
            if (isAdded && !isDetached) {
                Glide.with(this)
                    .load(imageUrl)
                    .apply(
                        RequestOptions()
                        .transform(CircleCrop())
                        .placeholder(R.drawable.default_profile_picture)
                        .error(R.drawable.default_profile_picture)
                        .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(profilePicture)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile image", e)
            profilePicture.setImageResource(R.drawable.default_profile_picture)
        }
    }

    private fun loadCurrentUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            emailInput.setText(user.email)

            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        firstNameInput.setText(document.getString("firstName") ?: "")
                        lastNameInput.setText(document.getString("lastName") ?: "")
                        phoneInput.setText(document.getString("phone") ?: "")
                        dateOfBirthInput.setText(document.getString("dateOfBirth") ?: "")
                        genderInput.setText(document.getString("gender") ?: "", false)
                        streetAddressInput.setText(document.getString("streetAddress") ?: "")
                        cityInput.setText(document.getString("city") ?: "")
                        stateInput.setText(document.getString("state") ?: "")
                        zipCodeInput.setText(document.getString("zipCode") ?: "")
                        countryInput.setText(document.getString("country") ?: "", false)
                        companyNameInput.setText(document.getString("companyName") ?: "")
                        jobTitleInput.setText(document.getString("jobTitle") ?: "")

                        // Load profile picture
                        val profilePictureUrl = document.getString("profilePictureUrl")
                        Log.d(TAG, "Profile picture URL from Firestore: $profilePictureUrl")
                        if (!profilePictureUrl.isNullOrEmpty()) {
                            loadProfileImage(profilePictureUrl)
                        } else {
                            profilePicture.setImageResource(R.drawable.default_profile_picture)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error loading profile data", exception)
                    Toast.makeText(context, "Error loading profile data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                dateOfBirthInput.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun uploadImageToFirebase(uri: Uri, callback: (String?) -> Unit) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val storageRef = storage.reference
            val profileImagesRef = storageRef.child("profile_images/${user.uid}.jpg")

            Log.d(TAG, "Starting image upload to Firebase Storage")

            profileImagesRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    Log.d(TAG, "Image uploaded successfully")
                    // Get download URL
                    profileImagesRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        Log.d(TAG, "Download URL obtained: $downloadUri")
                        callback(downloadUri.toString())
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Error getting download URL", e)
                        callback(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Image upload failed", e)
                    callback(null)
                }
        } ?: run {
            callback(null)
        }
    }

    private fun saveProfile() {
        // Validate required fields
        if (!validateInputs()) {
            return
        }

        // Show loading state
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val currentUser = auth.currentUser
        currentUser?.let { user ->

            // Function to save profile data
            fun saveProfileData(profilePictureUrl: String? = null) {
                val profileData = hashMapOf(
                    "firstName" to firstNameInput.text.toString().trim(),
                    "lastName" to lastNameInput.text.toString().trim(),
                    "phone" to phoneInput.text.toString().trim(),
                    "dateOfBirth" to dateOfBirthInput.text.toString().trim(),
                    "gender" to genderInput.text.toString().trim(),
                    "streetAddress" to streetAddressInput.text.toString().trim(),
                    "city" to cityInput.text.toString().trim(),
                    "state" to stateInput.text.toString().trim(),
                    "zipCode" to zipCodeInput.text.toString().trim(),
                    "country" to countryInput.text.toString().trim(),
                    "companyName" to companyNameInput.text.toString().trim(),
                    "jobTitle" to jobTitleInput.text.toString().trim(),
                    "updatedAt" to Timestamp.now()
                )

                // Add profile picture URL if provided
                profilePictureUrl?.let {
                    profileData["profilePictureUrl"] = it
                }

                // Save to Firestore
                firestore.collection("users").document(user.uid)
                    .set(profileData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Profile data saved to Firestore")
                        // Update Firebase Auth display name
                        val displayName = "${firstNameInput.text.toString().trim()} ${lastNameInput.text.toString().trim()}"
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()

                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { task ->
                                btnSave.isEnabled = true
                                btnSave.text = "Save Changes"

                                if (task.isSuccessful) {
                                    Log.d(TAG, "Profile updated successfully")
                                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                    listener?.onProfileUpdated()
                                    dismiss()
                                } else {
                                    Log.e(TAG, "Error updating display name", task.exception)
                                    Toast.makeText(context, "Profile saved but error updating display name", Toast.LENGTH_SHORT).show()
                                    listener?.onProfileUpdated()
                                    dismiss()
                                }
                            }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error saving profile to Firestore", exception)
                        btnSave.isEnabled = true
                        btnSave.text = "Save Changes"
                        Toast.makeText(context, "Error saving profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            // Check if user selected a new image
            selectedImageUri?.let { imageUri ->
                Log.d(TAG, "New image selected, uploading to Firebase Storage")
                Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()

                uploadImageToFirebase(imageUri) { downloadUrl ->
                    if (downloadUrl != null) {
                        Log.d(TAG, "Image upload successful, saving profile with new URL")
                        saveProfileData(downloadUrl)
                    } else {
                        Log.e(TAG, "Image upload failed")
                        Toast.makeText(context, "Error uploading image, saving profile without image update", Toast.LENGTH_LONG).show()
                        saveProfileData()
                    }
                }
            } ?: run {
                // No new image selected, just save profile data
                Log.d(TAG, "No new image selected, saving profile data only")
                saveProfileData()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (firstNameInput.text.toString().trim().isEmpty()) {
            firstNameInput.error = "First name is required"
            isValid = false
        }

        if (lastNameInput.text.toString().trim().isEmpty()) {
            lastNameInput.error = "Last name is required"
            isValid = false
        }

        val phone = phoneInput.text.toString().trim()
        if (phone.isNotEmpty() && phone.length < 9) {
            phoneInput.error = "Please enter a valid phone number"
            isValid = false
        }

        return isValid
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear Glide to prevent memory leaks
        if (isAdded) {
            Glide.with(this).clear(profilePicture)
        }
    }
}