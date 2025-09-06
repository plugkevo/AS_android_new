package com.example.africanshipping25

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

class PrivacyPolicyDialogFragment : DialogFragment() {

    private lateinit var btnClose: ImageView
    private lateinit var btnAccept: Button
    private lateinit var scrollView: ScrollView
    private lateinit var privacyContent: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_privacy_policy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        loadPrivacyPolicyContent()
    }

    override fun onStart() {
        super.onStart()
        // Set dialog size
        dialog?.window?.let { window ->
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.95).toInt()
            params.height = (resources.displayMetrics.heightPixels * 0.90).toInt()
            window.attributes = params

            // Ensure soft input doesn't interfere
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    private fun initializeViews(view: View) {
        btnClose = view.findViewById(R.id.btn_close)
        btnAccept = view.findViewById(R.id.btn_accept)
        scrollView = view.findViewById(R.id.scroll_view)
        privacyContent = view.findViewById(R.id.tv_privacy_content)
    }

    private fun setupClickListeners() {
        btnClose.setOnClickListener { dismiss() }
        btnAccept.setOnClickListener { dismiss() }
    }

    private fun loadPrivacyPolicyContent() {
        val lastUpdated = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())

        val privacyPolicyText = """
            <h2>Privacy Policy for African Shipping</h2>
            <p><strong>Last Updated:</strong> $lastUpdated</p>
            
            <h3>1. Introduction</h3>
            <p>Welcome to African Shipping ("we," "our," or "us"). We are committed to protecting your privacy and ensuring the security of your personal information. 
            This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our mobile application and services.</p>
            
            <h3>2. Information We Collect</h3>
            <h4>2.1 Personal Information</h4>
            <p>We may collect the following personal information:</p>
            <ul>
                <li>Name and contact information (email, phone number)</li>
                <li>Shipping and billing addresses</li>
                <li>Profile picture (optional)</li>
                <li>Account credentials</li>
                <li>Payment information (processed securely through third-party providers)</li>
            </ul>
            
            <h4>2.2 Shipment Information</h4>
            <p>We collect information related to your shipments:</p>
            <ul>
                <li>Sender and recipient details</li>
                <li>Package contents and dimensions</li>
                <li>Delivery preferences</li>
                <li>Tracking and delivery status</li>
            </ul>
            
            <h4>2.3 Device and Usage Information</h4>
            <p>We automatically collect certain information:</p>
            <ul>
                <li>Device type, operating system, and version</li>
                <li>App usage statistics and preferences</li>
                <li>Location data (with your permission)</li>
                <li>Log files and crash reports</li>
            </ul>
            
            <h3>3. How We Use Your Information</h3>
            <p>We use your information to:</p>
            <ul>
                <li>Provide and improve our shipping services</li>
                <li>Process and track your shipments</li>
                <li>Communicate with you about your orders</li>
                <li>Send notifications and updates</li>
                <li>Provide customer support</li>
                <li>Prevent fraud and ensure security</li>
                <li>Comply with legal obligations</li>
            </ul>
            
            <h3>4. Information Sharing and Disclosure</h3>
            <p>We may share your information with:</p>
            <ul>
                <li><strong>Service Providers:</strong> Third-party companies that help us operate our services</li>
                <li><strong>Shipping Partners:</strong> Logistics companies that handle your shipments</li>
                <li><strong>Payment Processors:</strong> Secure payment service providers</li>
                <li><strong>Legal Authorities:</strong> When required by law or to protect our rights</li>
            </ul>
            <p>We do not sell, rent, or trade your personal information to third parties for marketing purposes.</p>
            
            <h3>5. Data Security</h3>
            <p>We implement appropriate security measures to protect your information:</p>
            <ul>
                <li>Encryption of data in transit and at rest</li>
                <li>Regular security assessments and updates</li>
                <li>Access controls and authentication</li>
                <li>Secure data centers and infrastructure</li>
            </ul>
            
            <h3>6. Your Rights and Choices</h3>
            <p>You have the right to:</p>
            <ul>
                <li>Access and update your personal information</li>
                <li>Delete your account and associated data</li>
                <li>Opt-out of marketing communications</li>
                <li>Control location sharing permissions</li>
                <li>Request a copy of your data</li>
            </ul>
            
            <h3>7. Data Retention</h3>
            <p>We retain your information for as long as necessary to:</p>
            <ul>
                <li>Provide our services to you</li>
                <li>Comply with legal obligations</li>
                <li>Resolve disputes and enforce agreements</li>
                <li>Improve our services</li>
            </ul>
            
            <h3>8. International Data Transfers</h3>
            <p>Your information may be transferred to and processed in countries other than your own. We ensure appropriate safeguards are in place to protect your data during such transfers.</p>
            
            <h3>9. Children's Privacy</h3>
            <p>Our services are not intended for children under 13 years of age. We do not knowingly collect personal information from children under 13.</p>
            
            <h3>10. Changes to This Privacy Policy</h3>
            <p>We may update this Privacy Policy from time to time. We will notify you of any material changes by posting the new Privacy Policy in the app and updating the "Last Updated" date.</p>
            
            <h3>11. Contact Us</h3>
            <p>If you have any questions about this Privacy Policy, please contact us:</p>
            <ul>
                <li><strong>Email:</strong> support@africanshipping.com</li>
                <li><strong>Phone:</strong> +254 790875188</li>
                <li><strong>Address:</strong> African Shipping Company, Nairobi, Kenya</li>
            </ul>
            
            <h3>12. Consent</h3>
            <p>By using our app and services, you consent to the collection and use of your information as described in this Privacy Policy.</p>
            
            <hr>
            <p><em>This Privacy Policy is effective as of the date last updated above and will remain in effect except with respect to any changes in its provisions in the future, which will be in effect immediately after being posted on this page.</em></p>
        """.trimIndent()

        // Convert HTML to display text (you might want to use a WebView for better formatting)
        privacyContent.text = android.text.Html.fromHtml(privacyPolicyText, android.text.Html.FROM_HTML_MODE_COMPACT)
    }
}