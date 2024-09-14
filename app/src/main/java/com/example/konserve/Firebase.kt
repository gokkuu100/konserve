package com.example.konserve

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseManager(private val auth: FirebaseAuth, val firestore: FirebaseFirestore) {

    fun registerUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Registration failed")
                }
            }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message ?: "Login failed")
                }
            }
    }

    fun getUserData(userId: String, onComplete: (Map<String, Any>?, String?) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    onComplete(document.data, null)
                } else {
                    onComplete(null, "No such user")
                }
            }
            .addOnFailureListener { e ->
                onComplete(null, e.message)
            }
    }
}
