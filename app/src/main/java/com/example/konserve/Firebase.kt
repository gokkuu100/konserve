package com.example.konserve

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.Date

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

    fun addMemo(userId: String, date: String, memo: String, onComplete: (Boolean, String?) -> Unit) {
        val memoData = hashMapOf(
            "userId" to userId,
            "date" to date,
            "memo" to memo
        )
        firestore.collection("users")
            .add(memoData)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }

    fun getMemosForDate(userId: String, date: String, onComplete: (List<String>, String?) -> Unit) {
        firestore.collection("users")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { result ->
                val memos = mutableListOf<String>()
                for (document in result) {
                    val memo = document.getString("memo")
                    if (memo != null) {
                        memos.add(memo)
                    }
                }
                onComplete(memos, null)
            }
            .addOnFailureListener { e -> onComplete(emptyList(), e.message) }
    }
}
