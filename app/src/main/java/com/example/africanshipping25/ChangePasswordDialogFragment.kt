package com.example.africanshipping25

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordDialogFragment : DialogFragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userEmailText: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnSendEmail: Button
    private lateinit var loadingLayout: LinearLayout
    private lateinit var buttonLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingMessage: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_change_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        userEmailText = view.findViewById(R.id.tv_user_email)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSendEmail = view.findViewById(R.id.btn_send_email)
        loadingLayout = view.findViewById(R.id.loading_layout)
        buttonLayout = view.findViewById(R.id.button_layout)
        progressBar = view.findViewById(R.id.progress_bar)
        loadingMessage = view.findViewById(R.id.tv_loading_message)

        // Load user email
        loadUserEmail()

        // Set up click listeners
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog responsive
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun loadUserEmail() {
        val currentUser = auth.currentUser
        userEmailText.text = currentUser?.email ?: "No email found"
    }

    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSendEmail.setOnClickListener {
            sendPasswordResetEmail()
        }
    }

    private fun showLoading() {
        // Fade out buttons
        val fadeOutButtons = ObjectAnimator.ofFloat(buttonLayout, "alpha", 1f, 0f)
        fadeOutButtons.duration = 300
        fadeOutButtons.interpolator = AccelerateDecelerateInterpolator()

        fadeOutButtons.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
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

        // Animate loading messages
        animateLoadingMessages()
    }

    private fun hideLoading() {
        // Fade out loading
        val fadeOutLoading = ObjectAnimator.ofFloat(loadingLayout, "alpha", 1f, 0f)
        fadeOutLoading.duration = 300
        fadeOutLoading.interpolator = AccelerateDecelerateInterpolator()

        fadeOutLoading.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
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
            "Preparing reset email...",
            "Sending to your email...",
            "Almost done...",
            "Finalizing request..."
        )

        var currentIndex = 0
        val handler = Handler(Looper.getMainLooper())

        val updateMessage = object : Runnable {
            override fun run() {
                if (loadingLayout.visibility == View.VISIBLE && currentIndex < messages.size) {
                    // Fade out current message
                    val fadeOut = ObjectAnimator.ofFloat(loadingMessage, "alpha", 1f, 0f)
                    fadeOut.duration = 200

                    fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            // Update text
                            loadingMessage.text = messages[currentIndex]

                            // Fade in new message
                            val fadeIn = ObjectAnimator.ofFloat(loadingMessage, "alpha", 0f, 1f)
                            fadeIn.duration = 200
                            fadeIn.start()
                        }
                    })

                    fadeOut.start()
                    currentIndex++

                    // Schedule next message update
                    if (currentIndex < messages.size) {
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }

        handler.postDelayed(updateMessage, 500)
    }

    private fun sendPasswordResetEmail() {
        val currentUser = auth.currentUser
        currentUser?.email?.let { email ->
            // Show loading animation
            showLoading()

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    // Hide loading after a minimum time for better UX
                    Handler(Looper.getMainLooper()).postDelayed({
                        hideLoading()

                        if (task.isSuccessful) {
                            showSuccessMessage(email)
                        } else {
                            showErrorMessage(task.exception?.message)
                        }
                    }, 2000) // Minimum 2 seconds loading time
                }
        } ?: run {
            Toast.makeText(context, "No email address found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessMessage(email: String) {
        // Create success animation
        val successToast = Toast.makeText(
            context,
            "✅ Password reset email sent to $email\nCheck your inbox and spam folder.",
            Toast.LENGTH_LONG
        )
        successToast.show()

        // Auto-dismiss dialog after showing success
        Handler(Looper.getMainLooper()).postDelayed({
            dismiss()
        }, 1000)
    }

    private fun showErrorMessage(errorMessage: String?) {
        val errorToast = Toast.makeText(
            context,
            "❌ Error sending email: ${errorMessage ?: "Unknown error"}",
            Toast.LENGTH_LONG
        )
        errorToast.show()
    }
}