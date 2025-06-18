package com.example.africanshipping25

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View // Import View for View.GONE, View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView // Import LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView

    // New Lottie elements
    private lateinit var lottieLoadingAnimation: LottieAnimationView
    private lateinit var loginContentLayout: View // Reference to your LinearLayout holding login content
    private lateinit var loginScrollView: View // Reference to your ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // --- ALWAYS initialize UI elements FIRST ---
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        tvSignUp = findViewById(R.id.tv_signup)

        // Initialize Lottie and content views
        lottieLoadingAnimation = findViewById(R.id.lottie_loading_animation)
        loginContentLayout = findViewById(R.id.login_content_layout)
        loginScrollView = findViewById(R.id.login_scroll_view)
        // --- END UI Initialization ---


        // --- Check for existing user session ---
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: ${currentUser.uid}")
            if (currentUser.isEmailVerified) {
                // User is signed in and email is verified, show loading, then navigate
                showLoading(true) // Show loading animation immediately
                // Small delay to allow Lottie to show, then navigate
                loginContentLayout.postDelayed({ // Use postDelayed on a view to ensure it's on UI thread
                    navigateToMainActivity()
                }, 500) // 500ms delay, adjust as needed
                return // Exit onCreate as we're navigating away
            } else {
                // User is signed in but email is NOT verified.
                Toast.makeText(baseContext,
                    "Your email is not verified. Please check your inbox and log in again after verifying.",
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut() // Sign out the unverified user
                // Do NOT return here. Let onCreate continue so the login screen is ready for them.
            }
        }
        // --- END Existing User Check ---

        // Set up the login button click listener
        btnLogin.setOnClickListener {
            loginUser()
        }

        // Set up the "Forgot Password?" TextView click listener
        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
            // You would typically navigate to a reset password activity here
            // startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Set up the "Sign Up" TextView click listener
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, sign_up::class.java))
            finish() // Optionally finish the login activity
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

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

        // --- Show loading animation and hide content while checking credentials ---
        showLoading(true)


        // Attempt to sign in the user with email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // --- Hide loading animation regardless of success or failure ---
                showLoading(false)

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser

                    // Check if the email is verified after successful login
                    if (user?.isEmailVerified == true) {
                        // Email is verified, proceed to the main activity
                        Toast.makeText(baseContext, "Login successful.", Toast.LENGTH_SHORT).show()
                        navigateToMainActivity()
                    } else {
                        // Login was successful, but email is NOT verified.
                        auth.signOut() // Sign out the unverified user
                        Toast.makeText(
                            baseContext,
                            "Login successful, but your email is not verified. Please check your inbox and click the verification link. Then, log in again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    // Login failed (e.g., wrong password, user not found)
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loginContentLayout.visibility = View.GONE // Hide the main login content
            loginScrollView.visibility = View.GONE // Hide the scroll view if it covers the content
            lottieLoadingAnimation.visibility = View.VISIBLE // Show the Lottie animation
            lottieLoadingAnimation.playAnimation() // Ensure animation is playing
        } else {
            loginContentLayout.visibility = View.VISIBLE // Show the main login content
            loginScrollView.visibility = View.VISIBLE // Show the scroll view
            lottieLoadingAnimation.visibility = View.GONE // Hide the Lottie animation
            lottieLoadingAnimation.pauseAnimation() // Pause animation when hidden
        }
    }


    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}