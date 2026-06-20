package com.kevann.africanshipping25.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.kevann.africanshipping25.R

class WhatsAppSupportDialogFragment : DialogFragment() {

    companion object {
        // Support WhatsApp number (with country code, no + or spaces)
        // Format: 2547XXXXXXXX for Kenya
        private const val SUPPORT_PHONE = "254715123456"  // Replace with actual support number
    }

    private val issueTypes = listOf(
        IssueType("💳 Billing Issue", "I'm having trouble with my billing. Can you help me with payment-related issues?"),
        IssueType("🔐 Password Issue", "I'm unable to login. Can you help me reset my password or fix login issues?"),
        IssueType("🌐 Internet Issue", "I'm experiencing connectivity problems with the app. Can you provide technical support?"),
        IssueType("📦 Shipment Issue", "I have a question about my shipment tracking and delivery status."),
        IssueType("❌ Bug Report", "I found a bug in the app. Let me report it to the team."),
        IssueType("📞 Other", "I need to speak with support about something else.")
    )

    data class IssueType(val title: String, val message: String)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_whatsapp_support, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonsContainer: LinearLayout = view.findViewById(R.id.buttons_container)

        // Create a button for each issue type
        issueTypes.forEach { issue ->
            val button = Button(requireContext()).apply {
                text = issue.title
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 12)
                }
                setOnClickListener {
                    sendWhatsAppMessage(issue.message)
                    dismiss()
                }
            }
            buttonsContainer.addView(button)
        }

        // Add close button
        val closeButton = view.findViewById<Button>(R.id.btn_close).apply {
            setOnClickListener { dismiss() }
        }
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun sendWhatsAppMessage(message: String) {
        try {
            // Check if WhatsApp is installed
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$SUPPORT_PHONE?text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                // WhatsApp not installed, try web version
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$SUPPORT_PHONE?text=${Uri.encode(message)}")
                }
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error opening WhatsApp: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
