package com.example.konserve

import com.example.konserve.adapters.RedeemedCodesAdapter
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
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.PopupWindow

class RewardFragment : Fragment() {

    private lateinit var userNameTextView: TextView
    private lateinit var loyaltyPointsTextView: TextView
    private lateinit var codeEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var redeemedCodesRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var redeemedCodesAdapter: RedeemedCodesAdapter
    private var userPoints: Int = 0
    private val redeemedCodesList = mutableListOf<Pair<String, Int>>()
    private lateinit var withdrawButton: Button
    private var popupWindow: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rewards, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadUserData()

        // Initialize UI components
        userNameTextView = view.findViewById(R.id.userName)
        loyaltyPointsTextView = view.findViewById(R.id.loyaltyView)
        codeEditText = view.findViewById(R.id.codeEditText)
        submitButton = view.findViewById(R.id.redeemBtn)
        redeemedCodesRecyclerView = view.findViewById(R.id.recentActivityRecyclerView)
        withdrawButton = view.findViewById(R.id.withdrawBtn)

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

        // Set up withdraw button click listener
        withdrawButton.setOnClickListener {
            showWithdrawPopup()
        }

        return view
    }

    private fun loadTotalLoyaltyPoints() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("redeemed_codes").get()
            .addOnSuccessListener { documents ->
                var totalPoints = 0
                for (document in documents) {
                    totalPoints += document.getLong("points")?.toInt() ?: 0
                }
                loyaltyPointsTextView.text = "$totalPoints points"
            }
    }

    private fun loadUserPoints() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    userPoints = document.getLong("points")?.toInt() ?: 0

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

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("fullName") ?: "User"
                    userPoints = document.getLong("points")?.toInt() ?: 0

                    // Update UI
                    userNameTextView.text = name
                    loyaltyPointsTextView.text = "$userPoints points"

                    loadTotalLoyaltyPoints()
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
            codeEditText.text.clear()

            // Update RecyclerView dynamically
            redeemedCodesList.add(Pair(code, points))
            redeemedCodesAdapter.updateData(redeemedCodesList)

            loadTotalLoyaltyPoints()
        }
    }

    private fun loadRedeemedCodes() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("redeemed_codes").get()
            .addOnSuccessListener { documents ->
                val tempList = mutableListOf<Pair<String, Int>>() // Create temp list to avoid modifying original
                for (document in documents) {
                    val code = document.getString("code") ?: ""
                    val points = document.getLong("points")?.toInt() ?: 0
                    tempList.add(Pair(code, points))
                }
                redeemedCodesAdapter.updateData(tempList) // Use tempList to update adapter
            }
    }

    private fun showWithdrawPopup() {
        // Inflate the popup layout
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.withdraw_popup, null)

        // Create the popup window
        popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            // Set animation style for bottom-up animation
            animationStyle = R.style.PopupAnimation
            
            // Enable hardware acceleration for smooth animation
            setIsClippedToScreen(true)
            
            // Make sure touches outside dismiss the popup
            isOutsideTouchable = true
            isFocusable = true
        }

        // Set up close button
        val closeButton = popupView.findViewById<ImageButton>(R.id.closePopup)
        closeButton.setOnClickListener {
            popupWindow?.dismiss()
        }

        // Show the popup window from bottom
        popupWindow?.showAtLocation(view, Gravity.BOTTOM, 0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Dismiss popup if it's showing when fragment is destroyed
        popupWindow?.dismiss()
    }
}
