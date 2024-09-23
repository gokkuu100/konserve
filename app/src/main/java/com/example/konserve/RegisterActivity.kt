package com.example.konserve

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        firebaseManager = FirebaseManager(auth, firestore)

        val fullNameEditText = findViewById<TextInputEditText>(R.id.fullNameEditText)
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val redirect = findViewById<TextView>(R.id.signinredirect)


        registerButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString()
            val email = emailEditText.text.toString()
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
                    firebaseManager.registerUser(email, password, fullName) { success, message ->
                        if (success) {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        redirect.setOnClickListener{
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}