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

class LoginActivity: AppCompatActivity() {
    private lateinit var firebaseManager: FirebaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        firebaseManager = FirebaseManager(auth, firestore)

        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpText = findViewById<TextView>(R.id.signUpText)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            when {
                email.isEmpty() -> {
                    emailEditText.error = "Email is required"
                    emailEditText.requestFocus()
                }
                password.isEmpty() -> {
                    passwordEditText.error = "Password is required"
                    passwordEditText.requestFocus()
                }
                else -> {
                    firebaseManager.loginUser(email, password) { success, message ->
                        if (success) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, message ?: "Authentication failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        signUpText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}