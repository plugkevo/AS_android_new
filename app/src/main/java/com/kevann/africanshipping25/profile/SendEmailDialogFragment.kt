package com.kevann.africanshipping25.profile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.kevann.africanshipping25.R  // Add this import


class SendEmailDialogFragment : DialogFragment() {

    companion object {
        private const val SUPPORT_EMAIL = "2205852@students.kcau.ac.ke"
        private const val COMPANY_NAME = "African Shipping"
    }

    private lateinit var auth: FirebaseAuth

    // UI Elements
    private lateinit var fromEmailText: TextView
    private lateinit var supportEmailText: TextView
    private lateinit var subjectEdit: EditText
    private lateinit var messageEdit: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var priorityGroup: RadioGroup
    private lateinit var charCountText: TextView
    private lateinit var deviceInfoCheckbox: CheckBox
    private lateinit var loadingLayout: LinearLayout
    private lateinit var buttonLayout: LinearLayout
    private lateinit var loadingMessage: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnSend: Button
    private lateinit var btnClose: ImageView

    private val maxCharacters = 1000

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_send_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        initializeViews(view)

        // Setup UI
        setupSpinner()
        setupTextWatcher()
        setupClickListeners()
        loadUserEmail()
        loadSupportEmail()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initializeViews(view: View) {
        fromEmailText = view.findViewById(R.id.tv_from_email)
        supportEmailText = view.findViewById(R.id.tv_support_email)
        subjectEdit = view.findViewById(R.id.et_subject)
        messageEdit = view.findViewById(R.id.et_message)
        categorySpinner = view.findViewById(R.id.spinner_category)
        priorityGroup = view.findViewById(R.id.rg_priority)
        charCountText = view.findViewById(R.id.tv_char_count)
        deviceInfoCheckbox = view.findViewById(R.id.cb_include_device_info)
        loadingLayout = view.findViewById(R.id.loading_layout)
        buttonLayout = view.findViewById(R.id.button_layout)
        loadingMessage = view.findViewById(R.id.tv_loading_message)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSend = view.findViewById(R.id.btn_send)
        btnClose = view.findViewById(R.id.btn_close)
    }

    private fun setupSpinner() {
        val categories = arrayOf(
            "General Inquiry",
            "Shipment Issue",
            "Account Problem",
            "Payment Issue",
            "Technical Support",
            "Feature Request",
            "Bug Report",
            "Other"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun setupTextWatcher() {
        messageEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                charCountText.text = "$length/$maxCharacters characters"

                // Change color based on character count
                when {
                    length > maxCharacters -> charCountText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    length > maxCharacters * 0.9 -> charCountText.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                    else -> charCountText.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                }

                // Enable/disable send button
                btnSend.isEnabled = length <= maxCharacters && length > 0 && subjectEdit.text.isNotEmpty()
            }
        })

        subjectEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val messageLength = messageEdit.text.length
                val subjectLength = s?.length ?: 0
                btnSend.isEnabled = messageLength <= maxCharacters && messageLength > 0 && subjectLength > 0
            }
        })
    }

    private fun setupClickListeners() {
        btnClose.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener { dismiss() }
        btnSend.setOnClickListener { sendEmail() }
    }

    private fun loadUserEmail() {
        val currentUser = auth.currentUser
        fromEmailText.text = currentUser?.email ?: "user@example.com"
    }

    private fun loadSupportEmail() {
        // Set the support email in the UI
        supportEmailText?.text = SUPPORT_EMAIL
    }

    private fun showLoading() {
        // Fade out buttons
        val fadeOutButtons = ObjectAnimator.ofFloat(buttonLayout, "alpha", 1f, 0f)
        fadeOutButtons.duration = 300
        fadeOutButtons.interpolator = AccelerateDecelerateInterpolator()

        fadeOutButtons.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                buttonLayout.visibility = View.GONE

                // Show loading layout
                loadingLayout.visibility = View.VISIBLE
                loadingLayout.alpha = 0f

                val fadeInLoading = ObjectAnimator.ofFloat(loadingLayout, "alpha", 0f, 1f)
                fadeInLoading.duration = 300
                fadeInLoading.interpolator = AccelerateDecelerateInterpolator()
                fadeInLoading.start()
            }
        })

        fadeOutButtons.start()
        animateLoadingMessages()
    }

    private fun hideLoading() {
        // Fade out loading
        val fadeOutLoading = ObjectAnimator.ofFloat(loadingLayout, "alpha", 1f, 0f)
        fadeOutLoading.duration = 300
        fadeOutLoading.interpolator = AccelerateDecelerateInterpolator()

        fadeOutLoading.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                loadingLayout.visibility = View.GONE

                // Show buttons again
                buttonLayout.visibility = View.VISIBLE
                buttonLayout.alpha = 0f

                val fadeInButtons = ObjectAnimator.ofFloat(buttonLayout, "alpha", 0f, 1f)
                fadeInButtons.duration = 300
                fadeInButtons.interpolator = AccelerateDecelerateInterpolator()
                fadeInButtons.start()
            }
        })

        fadeOutLoading.start()
    }

    private fun animateLoadingMessages() {
        val messages = arrayOf(
            "Preparing email...",
            "Gathering information...",
            "Sending to support...",
            "Almost done..."
        )

        var currentIndex = 0
        val handler = Handler(Looper.getMainLooper())

        val updateMessage = object : Runnable {
            override fun run() {
                if (loadingLayout.visibility == View.VISIBLE && currentIndex < messages.size) {
                    loadingMessage.text = messages[currentIndex]
                    currentIndex++

                    if (currentIndex < messages.size) {
                        handler.postDelayed(this, 800)
                    }
                }
            }
        }

        handler.postDelayed(updateMessage, 500)
    }

    private fun sendEmail() {
        val subject = subjectEdit.text.toString().trim()
        val message = messageEdit.text.toString().trim()

        if (subject.isEmpty()) {
            Toast.makeText(context, "Please enter a subject", Toast.LENGTH_SHORT).show()
            return
        }

        if (message.isEmpty()) {
            Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        if (message.length > maxCharacters) {
            Toast.makeText(context, "Message is too long", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()

        // Simulate sending delay for better UX
        Handler(Looper.getMainLooper()).postDelayed({
            val emailBody = buildEmailBody(subject, message)
            sendEmailIntent(subject, emailBody)
        }, 2500)
    }

    private fun buildEmailBody(subject: String, message: String): String {
        val currentUser = auth.currentUser
        val category = categorySpinner.selectedItem.toString()
        val priority = getPriority()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val emailBody = StringBuilder()
        emailBody.append("SUPPORT REQUEST\n")
        emailBody.append("================\n\n")
        emailBody.append("Category: $category\n")
        emailBody.append("Priority: $priority\n")
        emailBody.append("Date: $timestamp\n\n")
        emailBody.append("USER INFORMATION:\n")
        emailBody.append("Email: ${currentUser?.email ?: "Not available"}\n")
        emailBody.append("User ID: ${currentUser?.uid ?: "Not available"}\n\n")

        if (deviceInfoCheckbox.isChecked) {
            emailBody.append("DEVICE INFORMATION:\n")
            emailBody.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            emailBody.append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            emailBody.append("App Version: 1.0.0\n\n")
        }

        emailBody.append("MESSAGE:\n")
        emailBody.append("----------\n")
        emailBody.append(message)
        emailBody.append("\n\n")
        emailBody.append("Thank you for contacting $COMPANY_NAME Support!")

        return emailBody.toString()
    }

    private fun getPriority(): String {
        return when (priorityGroup.checkedRadioButtonId) {
            R.id.rb_low -> "Low"
            R.id.rb_high -> "High"
            else -> "Medium"
        }
    }

    private fun sendEmailIntent(subject: String, body: String) {
        try {
            // First try: Use ACTION_SENDTO with mailto (most reliable)
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL") // Correct way
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "[$subject] - $COMPANY_NAME App Support")
                putExtra(Intent.EXTRA_TEXT, body)
            }

            if (emailIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(emailIntent)
                showSuccessAndDismiss()
                return
            }

            // Second try: Use ACTION_SEND (broader compatibility)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "[$subject] - $COMPANY_NAME App Support")
                putExtra(Intent.EXTRA_TEXT, body)
            }

            if (sendIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(Intent.createChooser(sendIntent, "Send Email"))
                showSuccessAndDismiss()
                return
            }

            // Third try: Generic ACTION_SEND
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "[$subject] - $COMPANY_NAME App Support")
                putExtra(Intent.EXTRA_TEXT, body)
            }

            if (genericIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(Intent.createChooser(genericIntent, "Send Email"))
                showSuccessAndDismiss()
                return
            }

            // Last resort: Show manual options
            showManualEmailOptions(subject, body)

        } catch (e: Exception) {
            hideLoading()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            showManualEmailOptions(subject, body)
        }
    }

    private fun showSuccessAndDismiss() {
        Handler(Looper.getMainLooper()).postDelayed({
            hideLoading()
            Toast.makeText(context, "✅ Email opened successfully!", Toast.LENGTH_LONG).show()
            dismiss()
        }, 1000)
    }

    private fun showManualEmailOptions(subject: String, body: String) {
        hideLoading()

        val options = arrayOf(
            "Copy email details",
            "Try Gmail",
            "Try other apps",
            "Cancel"
        )

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("No Email App Found")
        builder.setMessage("We couldn't find an email app on your device. Choose an option:")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> copyEmailToClipboard(subject, body)
                1 -> tryGmailSpecifically(subject, body)
                2 -> tryOtherApps(subject, body)
                3 -> { /* Cancel - do nothing */ }
            }
        }
        builder.show()
    }

    private fun copyEmailToClipboard(subject: String, body: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val emailContent = """
        To: $SUPPORT_EMAIL
        Subject: [$subject] - $COMPANY_NAME App Support
        
        $body
    """.trimIndent()

        val clip = ClipData.newPlainText("Email Content", emailContent)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "📋 Email details copied to clipboard!\nYou can paste this in any email app.", Toast.LENGTH_LONG).show()

        // Show instructions
        showEmailInstructions()
    }

    private fun tryGmailSpecifically(subject: String, body: String) {
        try {
            val gmailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.google.android.gm")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "[$subject] - $COMPANY_NAME App Support")
                putExtra(Intent.EXTRA_TEXT, body)
            }

            startActivity(gmailIntent)
            showSuccessAndDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "Gmail not found. Please install Gmail or use another email app.", Toast.LENGTH_LONG).show()
            copyEmailToClipboard(subject, body)
        }
    }

    private fun tryOtherApps(subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "[$subject] - $COMPANY_NAME App Support")
                putExtra(Intent.EXTRA_TEXT, body)
            }

            val chooser = Intent.createChooser(intent, "Send Email Using...")
            startActivity(chooser)
            showSuccessAndDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "No compatible apps found.", Toast.LENGTH_SHORT).show()
            copyEmailToClipboard(subject, body)
        }
    }

    private fun showEmailInstructions() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("📧 Email Instructions")
        builder.setMessage("""
        The email details have been copied to your clipboard.
        
        To send the email:
        1. Open your preferred email app (Gmail, Outlook, etc.)
        2. Create a new email
        3. Paste the copied content
        4. Send the email
        
        Our support team will respond within 24 hours.
    """.trimIndent())
        builder.setPositiveButton("Got it") { _, _ ->
            dismiss()
        }
        builder.setNegativeButton("Try Again") { _, _ ->
            // Let user try the email process again
        }
        builder.show()
    }
}