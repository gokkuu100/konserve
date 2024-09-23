package com.example.konserve

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.FirebaseException
import com.google.type.Date

class FirebaseManager(private val auth: FirebaseAuth, val firestore: FirebaseFirestore) {

    fun registerUser(email: String, password: String, fullName:String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val userData = hashMapOf(
                            "userId" to userId,
                            "email" to email,
                            "fullName" to fullName,
                            "created_at" to System.currentTimeMillis()
                        )
                        firestore.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                onComplete(true, null)
                            }
                            .addOnFailureListener { e ->
                                onComplete(false, e.localizedMessage)
                            }
                    } else {
                        onComplete(false, "User ID is null")
                    }
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

    fun addMemo(userId: String, date: String, title: String, content: String, callback: (Boolean, String?) -> Unit) {
        val memoData = hashMapOf(
            "title" to title,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(userId)
            .collection("memos")
            .document(date)
            .collection("memoList")
            .add(memoData)
            .addOnSuccessListener {
                Log.d("Memo", "Memo added successfully!")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                callback(false, e.localizedMessage)
            }
    }

    fun getMemosForDate(userId: String, date: String, callback: (List<Memo>, String?) -> Unit): ListenerRegistration {
        return firestore.collection("users")
            .document(userId)
            .collection("memos")
            .document(date)
            .collection("memoList")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    callback(emptyList(), error.localizedMessage)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val memoList = snapshots.documents.map { document ->
                        val title = document.getString("title") ?: ""
                        val content = document.getString("content") ?: ""
                        val timestamp = document.getLong("timestamp") ?: 0L
                        Memo(title, content, timestamp)
                    }
                    callback(memoList, null)
                } else {
                    callback(emptyList(), null)
                }
            }
    }
}
