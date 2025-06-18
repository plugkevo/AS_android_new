package com.example.africanshipping25

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Import View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView // Import LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class sign_up : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tv_login: TextView
    private lateinit var btnSignUp: Button

    // New Lottie elements
    private lateinit var lottieLoadingAnimation: LottieAnimationView
    private lateinit var signupContentLayout: View // Reference to your LinearLayout holding sign-up content
    private lateinit var signupScrollView: View // Reference to your ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize UI elements
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSignUp = findViewById(R.id.btn_signup)
        tv_login = findViewById(R.id.tv_login)

        // Initialize Lottie and content views
        lottieLoadingAnimation = findViewById(R.id.lottie_loading_animation_signup)
        signupContentLayout = findViewById(R.id.signup_content_layout)
        signupScrollView = findViewById(R.id.signup_scroll_view)

        // Set up the sign-up button click listener
        btnSignUp.setOnClickListener {
            signUpUser()
        }
        tv_login.setOnClickListener {
            startActivity(Intent(this, login::class.java))
            finish()
        }

        // You would also handle the Google Sign-up button and the "Already have an account? Login" TextView here.
    }

    private fun signUpUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Basic input validation
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password should be at least 6 characters long"
            etPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Confirm password is required"
            etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            etConfirmPassword.requestFocus()
            return
        }

        // --- Show loading animation and hide content while processing sign-up ---
        showLoading(true)


        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // --- Hide loading animation regardless of success or failure ---
                showLoading(false)

                if (task.isSuccessful) {
                    // Sign up success, send verification email
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { sendTask ->
                            if (sendTask.isSuccessful) {
                                Toast.makeText(
                                    baseContext,
                                    "Verification email sent. Please check your inbox and then log in.",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Navigate to login after sending verification email
                                startActivity(Intent(this, login::class.java))
                                finish() // Finish sign-up activity
                            } else {
                                Toast.makeText(
                                    baseContext,
                                    "Failed to send verification email: ${sendTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                // Handle this error: user might still be created but no email sent.
                                // You might offer to resend or direct them to login.
                            }
                        }
                } else {
                    // If sign up fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Sign up failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            signupContentLayout.visibility = View.GONE // Hide the main signup content
            signupScrollView.visibility = View.GONE // Hide the scroll view
            lottieLoadingAnimation.visibility = View.VISIBLE // Show the Lottie animation
            lottieLoadingAnimation.playAnimation() // Ensure animation is playing
        } else {
            signupContentLayout.visibility = View.VISIBLE // Show the main signup content
            signupScrollView.visibility = View.VISIBLE // Show the scroll view
            lottieLoadingAnimation.visibility = View.GONE // Hide the Lottie animation
            lottieLoadingAnimation.pauseAnimation() // Pause animation when hidden
        }
    }

    companion object {
        private const val TAG = "SignUpActivity"
    }
}