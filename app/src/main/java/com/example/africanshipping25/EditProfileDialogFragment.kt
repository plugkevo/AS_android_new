package com.example.africanshipping25

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class EditProfileDialogFragment : DialogFragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

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
    private val calendar = Calendar.getInstance()

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                profilePicture.setImageURI(it)
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
        val genderOptions = arrayOf("Male", "Female", "Other", "Prefer not to say")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        genderInput.setAdapter(genderAdapter)

        // Country dropdown
        val countryOptions = arrayOf(
            "United States", "Canada", "United Kingdom", "Germany", "France",
            "Spain", "Italy", "Netherlands", "Belgium", "Switzerland", "Australia",
            "New Zealand", "Japan", "South Korea", "Singapore", "Other"
        )
        val countryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countryOptions)
        countryInput.setAdapter(countryAdapter)
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
            openImagePicker()
        }

        dateOfBirthInput.setOnClickListener {
            showDatePicker()
        }
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

    // Update the loadCurrentUserData method to include image loading
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
                        if (!profilePictureUrl.isNullOrEmpty()) {
                            loadProfileImage(profilePictureUrl)
                        }
                    }
                }
        }
    }



    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
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
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            // Save to Firestore
            firestore.collection("users").document(user.uid)
                .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    // Update Firebase Auth display name
                    val displayName = "${firstNameInput.text.toString().trim()} ${lastNameInput.text.toString().trim()}"
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { task ->
                            btnSave.isEnabled = true
                            btnSave.text = "Save Changes"

                            if (task.isSuccessful) {
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                listener?.onProfileUpdated()
                                dismiss()
                            } else {
                                Toast.makeText(context, "Error updating display name", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    btnSave.isEnabled = true
                    btnSave.text = "Save Changes"
                    Toast.makeText(context, "Error saving profile: ${exception.message}", Toast.LENGTH_SHORT).show()
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
        if (phone.isNotEmpty() && phone.length < 10) {
            phoneInput.error = "Please enter a valid phone number"
            isValid = false
        }

        return isValid
    }
}