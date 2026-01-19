package com.kevann.africanshipping25

import android.app.AlertDialog // Import AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater // Import LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText // Import TextInputEditText
import com.google.android.material.textfield.TextInputLayout // Import TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.cardview.widget.CardView // Import CardView

class login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView
    private lateinit var lottieLoadingAnimation: LottieAnimationView
    private lateinit var loginContentLayout: View
    private lateinit var loginScrollView: View

    // --- Google Sign-In specific declarations ---
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnGoogleLogin: CardView // Changed from Button to CardView
    private val RC_GOOGLE_SIGN_IN = 9001 // Request code for Google Sign-In
    // --- End Google Sign-In specific declarations ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // --- Initialize UI elements ---
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        tvSignUp = findViewById(R.id.tv_signup)
        lottieLoadingAnimation = findViewById(R.id.lottie_loading_animation)
        loginContentLayout = findViewById(R.id.login_content_layout)
        loginScrollView = findViewById(R.id.login_scroll_view)

        // Initialize Google Sign-In button
        btnGoogleLogin = findViewById(R.id.btn_google_login) // Link to your CardView
        // --- End UI Initialization ---

        // --- Configure Google Sign-In options ---
        // requestIdToken is crucial for authenticating with Firebase
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // This comes from google-services.json
            .requestEmail() // Request the user's email address
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // --- End Google Sign-In Configuration ---

        // --- Check for existing user session (Firebase) ---
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: ${currentUser.uid}")
            // Check if the current user is signed in with Google and if their email is verified
            // This is a more robust check since they could have logged in with Google earlier
            val isGoogleUser = currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            if (currentUser.isEmailVerified || isGoogleUser) { // Consider Google users verified by default
                // User is signed in and verified (or signed in via Google)
                showLoading(true)
                loginContentLayout.postDelayed({
                    navigateToMainActivity()
                }, 500)
                return
            } else {
                // User is signed in with email/password but email is NOT verified.
                Toast.makeText(baseContext,
                    "Your email is not verified. Please check your inbox and log in again after verifying.",
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut() // Sign out the unverified user
            }
        }
        // --- END Existing User Check ---

        // Set up click listeners
        btnLogin.setOnClickListener {
            loginUser()
        }

        tvForgotPassword.setOnClickListener {
            // Call the new function to show the forgot password dialog
            showForgotPasswordDialog()
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, sign_up::class.java))
            finish()
        }

        // --- Google Sign-In Button Listener ---
        btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }
        // --- End Google Sign-In Button Listener ---
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

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

        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false) // Hide loading regardless of success or failure
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        Toast.makeText(baseContext, "Login successful.", Toast.LENGTH_SHORT).show()
                        navigateToMainActivity()
                    } else {
                        auth.signOut()
                        Toast.makeText(
                            baseContext,
                            "Login successful, but your email is not verified. Please check your inbox and click the verification link. Then, log in again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // --- New Google Sign-In methods ---
    private fun signInWithGoogle() {
        showLoading(true) // Show loading when initiating Google Sign-In
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, now authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "Google Sign-In successful. ID Token: ${account.idToken}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed
                showLoading(false) // Hide loading if Google Sign-In itself fails
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google Sign In Failed: ${e.statusCode} - ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false) // Hide loading regardless of Firebase auth success/failure
                if (task.isSuccessful) {
                    // Sign in success. User is now signed in to Firebase with Google.
                    Log.d(TAG, "Firebase signInWithCredential success")
                    val user = auth.currentUser
                    Toast.makeText(this, "Signed in with Google as ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "Firebase Google Auth failed", task.exception)
                    Toast.makeText(this, "Authentication with Google Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // --- New function for Forgot Password dialog ---
    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val emailField = dialogView.findViewById<TextInputEditText>(R.id.editTextResetEmail)
        val emailInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutResetEmail)
        val buttonSendResetEmail = dialogView.findViewById<Button>(R.id.buttonSendResetEmail)
        val buttonCancelReset = dialogView.findViewById<Button>(R.id.buttonCancelReset)

        // Pre-fill email if available from login field
        emailField.setText(etEmail.text.toString().trim())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        buttonSendResetEmail.setOnClickListener {
            val email = emailField.text.toString().trim()
            emailInputLayout.error = null // Clear previous error

            if (email.isEmpty()) {
                emailInputLayout.error = "Email is required"
                emailField.requestFocus()
                return@setOnClickListener
            }

            // Send password reset email
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset email sent to $email. Please check your inbox.", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        val errorMessage = task.exception?.message ?: "Failed to send reset email."
                        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Failed to send password reset email: $errorMessage", task.exception)
                    }
                }
        }

        buttonCancelReset.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    // --- End new function ---

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loginContentLayout.visibility = View.GONE
            loginScrollView.visibility = View.GONE
            lottieLoadingAnimation.visibility = View.VISIBLE
            lottieLoadingAnimation.playAnimation()
        } else {
            loginContentLayout.visibility = View.VISIBLE
            loginScrollView.visibility = View.VISIBLE
            lottieLoadingAnimation.visibility = View.GONE
            lottieLoadingAnimation.pauseAnimation()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // These flags clear the back stack so the user cannot navigate back to the login screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}