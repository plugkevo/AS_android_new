package com.example.africanshipping25

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Assuming your XML layout file is named activity_login.xml

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize UI elements
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        tvSignUp = findViewById(R.id.tv_signup)

        // Set up the login button click listener
        btnLogin.setOnClickListener {
            loginUser()
        }

        // Set up the "Forgot Password?" TextView click listener
        tvForgotPassword.setOnClickListener {
            // Implement forgot password functionality here
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
            // You would typically navigate to a reset password activity
        }

        // Set up the "Sign Up" TextView click listener
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, sign_up::class.java))
            finish() // Optionally finish the login activity
        }

        // You would also handle the Google Login button here.
        // For Google Login, you'll need to integrate the Google Sign-In SDK and Firebase Authentication.
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

        // Sign in with email and password
        // In your LoginActivity's loginUser() function:

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        // Email is verified, proceed to the next activity
                        Toast.makeText(baseContext, "Login successful.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Email is not verified
                        Toast.makeText(
                            baseContext,
                            "Email not verified. Please check your inbox and click the verification link.",
                            Toast.LENGTH_LONG
                        ).show()
                        // Optionally, you can provide a button to resend the verification email:
                        // user?.sendEmailVerification()?.addOnCompleteListener { /* ... */ }
                    }
                } else {
                    // Login failed
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}