package com.example.konserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class RewardFragment : Fragment() {

    private lateinit var pointsTextView: TextView
    private lateinit var codeEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var redeemedCodesRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var redeemedCodesAdapter: RedeemedCodesAdapter
    private var userPoints: Int = 0
    private val redeemedCodesList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rewards, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        pointsTextView = view.findViewById(R.id.pointsTextView)
        codeEditText = view.findViewById(R.id.codeEditText)
        submitButton = view.findViewById(R.id.submitButton)
        redeemedCodesRecyclerView = view.findViewById(R.id.redeemedCodesRecyclerView)

        // Set up RecyclerView
        redeemedCodesAdapter = RedeemedCodesAdapter(redeemedCodesList)
        redeemedCodesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        redeemedCodesRecyclerView.adapter = redeemedCodesAdapter

        // Load user points & redeemed codes
        loadUserPoints()
        loadRedeemedCodes()

        // Handle code submission
        submitButton.setOnClickListener {
            val code = codeEditText.text.toString().trim()
            if (code.isNotEmpty()) {
                checkIfCodeAlreadyRedeemed(code)
            } else {
                Toast.makeText(requireContext(), "Please enter a code", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadUserPoints() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    userPoints = document.getLong("points")?.toInt() ?: 0
                    pointsTextView.text = "$userPoints pts"
                }
            }
    }

    private fun checkIfCodeAlreadyRedeemed(code: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("redeemed_codes").document(code).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Toast.makeText(requireContext(), "Code already redeemed", Toast.LENGTH_SHORT).show()
                } else {
                    validateCode(code)
                }
            }
    }

    private fun validateCode(code: String) {
        firestore.collection("reward_codes").document(code).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val points = document.getLong("points")?.toInt() ?: 0
                    val expiresAt = document.getTimestamp("expires_at")
                    val isActive = document.getBoolean("is_active") ?: true

                    if (!isActive) {
                        Toast.makeText(requireContext(), "This code is inactive", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    if (expiresAt != null && expiresAt.toDate().before(java.util.Date())) {
                        Toast.makeText(requireContext(), "This code has expired", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Code is valid, proceed with updating points
                    updateUserPoints(points, code)
                } else {
                    Toast.makeText(requireContext(), "Invalid code", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUserPoints(points: Int, code: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)
        val redeemedCodeRef = userRef.collection("redeemed_codes").document(code)

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val currentPoints = userSnapshot.getLong("points") ?: 0
            val newTotalPoints = currentPoints + points

            transaction.update(userRef, "points", newTotalPoints)

            transaction.set(redeemedCodeRef, mapOf(
                "code" to code,
                "points" to points,
                "redeemed_at" to Timestamp.now()
            ))

            newTotalPoints
        }.addOnSuccessListener { updatedPoints ->
            userPoints = updatedPoints.toInt()
            pointsTextView.text = "$userPoints pts"
            codeEditText.text.clear()
            redeemedCodesList.add("$code - $points pts")
            redeemedCodesAdapter.notifyDataSetChanged()
        }
    }

    private fun loadRedeemedCodes() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("redeemed_codes").get()
            .addOnSuccessListener { documents ->
                redeemedCodesList.clear()
                for (document in documents) {
                    val code = document.getString("code") ?: ""
                    val points = document.getLong("points")?.toInt() ?: 0
                    redeemedCodesList.add("$code - $points pts")
                }
                redeemedCodesAdapter.notifyDataSetChanged()
            }
    }
}
