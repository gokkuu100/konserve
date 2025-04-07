package com.example.konserve

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var supabaseManager: SupabaseManager
    private lateinit var oneTapClient: SignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleGoogleSignInResult(result.data)
        } else {
            Toast.makeText(this, "Google Sign-Up cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supabaseManager = SupabaseManager(this)
        // Initialize Google Sign-In
        supabaseManager.initGoogleSignIn(this)
        oneTapClient = Identity.getSignInClient(this)

        val fullNameEditText = findViewById<TextInputEditText>(R.id.fullNameEditText)
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val googleSignUpButton = findViewById<Button>(R.id.googleSignUpButton) // Add this button to your layout
        val redirect = findViewById<TextView>(R.id.signinredirect)

        registerButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString()
            val email = emailEditText.text.toString().trim().lowercase()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validate input fields
            when {
                fullName.isEmpty() -> {
                    fullNameEditText.error = "Full name is required"
                    fullNameEditText.requestFocus()
                }
                email.isEmpty() -> {
                    emailEditText.error = "Email is required"
                    emailEditText.requestFocus()
                }
                password.isEmpty() -> {
                    passwordEditText.error = "Password is required"
                    passwordEditText.requestFocus()
                }
                password.length < 6 -> {
                    passwordEditText.error = "Password should be at least 6 characters long"
                    passwordEditText.requestFocus()
                }
                password != confirmPassword -> {
                    confirmPasswordEditText.error = "Passwords do not match"
                    confirmPasswordEditText.requestFocus()
                }
                else -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        supabaseManager.registerUser(email, password, fullName) { success, message ->
                            if (success) {
                                Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@RegisterActivity, message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
        
        // Add Google Sign-Up button listener
        googleSignUpButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    supabaseManager.signInWithGoogle(this@RegisterActivity) { success, error ->
                        if (!success) {
                            Toast.makeText(this@RegisterActivity, error ?: "Google Sign-Up error", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Google Sign-Up error: ${e.message}")
                    Toast.makeText(this@RegisterActivity, "Sign-Up error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        redirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
    
    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken

            if (idToken != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    supabaseManager.handleGoogleSignInResult(idToken) { success, error ->
                        if (success) {
                            Toast.makeText(this@RegisterActivity, "Registration with Google successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, error ?: "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "No ID token found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignUp", "Sign-Up result error: ${e.statusCode} ${e.message}")
            Toast.makeText(this, "Sign-Up error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SupabaseManager.REQUEST_CODE_GOOGLE_SIGN_IN) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        supabaseManager.handleGoogleSignInResult(idToken) { success, error ->
                            if (success) {
                                Toast.makeText(this@RegisterActivity, "Registration with Google successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@RegisterActivity, error ?: "Registration failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.e("GoogleSignUp", "ID token is null")
                }
            } catch (e: Exception) {
                Log.e("GoogleSignUp", "Error getting sign-in credential: ${e.message}")
            }
        }
    }
}