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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up) // Assuming your XML layout file is named activity_sign_up.xml

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize UI elements
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSignUp = findViewById(R.id.btn_signup)
        tv_login = findViewById(R.id.tv_login)

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

        // Basic input validation (same as before)
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

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success, send verification email
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { sendTask ->
                            if (sendTask.isSuccessful) {
                                Toast.makeText(
                                    baseContext,
                                    "Verification email sent. Please check your inbox.",
                                    Toast.LENGTH_LONG
                                ).show()

                                startActivity(Intent(this, login::class.java))

                                // Optionally, you can disable the sign-up button or show a message
                                // indicating that the user needs to verify their email.
                                // You should NOT proceed to the next activity here.
                            } else {
                                Toast.makeText(
                                    baseContext,
                                    "Failed to send verification email: ${sendTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                // You might want to handle this error, possibly by allowing the user to resend the email.
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
                    // Optionally, you can update UI to display error messages
                }
            }
    }

    // You will need to handle the email verification in your next activity or when the user tries to log in.
    // A common approach is to check `auth.currentUser?.isEmailVerified` in your login activity.

    companion object {
        private const val TAG = "SignUpActivity"
    }
}