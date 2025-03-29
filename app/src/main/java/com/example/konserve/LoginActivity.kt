package com.example.konserve

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var signUpText: TextView
    private lateinit var supabaseManager: SupabaseManager

    private lateinit var oneTapClient: SignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleGoogleSignInResult(result.data)
        } else {
            Toast.makeText(this, "Google Sign-In cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        try {
            // Initialize Supabase Manager
            supabaseManager = SupabaseManager(this)
            // Initialize Google Sign-In
            supabaseManager.initGoogleSignIn(this)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error initializing: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Initialize UI components
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        googleSignInButton = findViewById(R.id.googleSignInButton)
        signUpText = findViewById(R.id.signUpText)

        oneTapClient = Identity.getSignInClient(this)

        // Set up login button click listener
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(email, password)) {
                lifecycleScope.launch {
                    performLogin(email, password)
                }
            }
        }

        // Set up Google Sign-In button click listener
        googleSignInButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    supabaseManager.signInWithGoogle(this@LoginActivity) { _, _ -> }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Google Sign-In error: ${e.message}")
                    Toast.makeText(this@LoginActivity, "Sign-In error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set up sign-up text click listener
        signUpText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }



    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            isValid = false
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            isValid = false
        }

        return isValid
    }

    private suspend fun performLogin(email: String, password: String) {
        supabaseManager.loginUser(email, password) { success, error ->
            if (success) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this@LoginActivity, error ?: "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken

            if (idToken != null) {
                lifecycleScope.launch {
                    supabaseManager.handleGoogleSignInResult(idToken) { success, error ->
                        if (success) {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, error ?: "Authentication failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "No ID token found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Sign-In result error: ${e.statusCode} ${e.message}")
            Toast.makeText(this, "Sign-In error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SupabaseManager.REQUEST_CODE_GOOGLE_SIGN_IN) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    lifecycleScope.launch {
                        supabaseManager.handleGoogleSignInResult(idToken) { success, error ->
                            if (success) {
                                Log.d("GoogleSignIn", "Sign-in successful")
                            } else {
                                Log.e("GoogleSignIn", "Error: $error")
                            }
                        }
                    }
                } else {
                    Log.e("GoogleSignIn", "ID token is null")
                }
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Error getting sign-in credential: ${e.message}")
            }
        }
    }
}
