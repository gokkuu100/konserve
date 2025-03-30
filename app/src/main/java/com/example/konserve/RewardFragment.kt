package com.example.konserve

import com.example.konserve.adapters.RedeemedCodesAdapter
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RewardFragment : Fragment() {

    private lateinit var userNameTextView: TextView
    private lateinit var loyaltyPointsTextView: TextView
    private lateinit var codeEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var redeemedCodesRecyclerView: RecyclerView
    private lateinit var supabaseManager: SupabaseManager
    private lateinit var redeemedCodesAdapter: RedeemedCodesAdapter
    private var userPoints: Int = 0
    private val redeemedCodesList = mutableListOf<Pair<String, Int>>()
    private lateinit var withdrawButton: Button
    private var popupWindow: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rewards, container, false)

        // Initialize Supabase Manager
        supabaseManager = SupabaseManager(requireContext())

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
        CoroutineScope(Dispatchers.Main).launch {
            loadUserData()
            loadUserPoints()
            loadRedeemedCodes()
        }

        // Handle code submission
        submitButton.setOnClickListener {
            val code = codeEditText.text.toString().trim()
            if (code.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    checkIfCodeAlreadyRedeemed(code)
                }
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

    private suspend fun loadUserData() {
        val userId = withContext(Dispatchers.IO) { supabaseManager.getCurrentUser() } ?: return

        supabaseManager.getUserData(userId) { user, error ->
            CoroutineScope(Dispatchers.Main).launch {
                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading user data: $error", Toast.LENGTH_SHORT).show()
                } else if (user != null) {
                    // Directly access User properties
                    userNameTextView.text = user.full_name ?: "User"
                    userPoints = user.reward_points ?: 0
                    loyaltyPointsTextView.text = "$userPoints points"
                }
            }
        }
    }

    private suspend fun loadUserPoints() {
        val userId = withContext(Dispatchers.IO) { supabaseManager.getCurrentUser() } ?: return
        supabaseManager.fetchUserPoints(userId) { points, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Error loading user points: $error", Toast.LENGTH_SHORT).show()
            } else if (points != null) {
                userPoints = points
                loyaltyPointsTextView.text = "$userPoints points"
            }
        }
    }

    private suspend fun checkIfCodeAlreadyRedeemed(code: String) {
        val userId = withContext(Dispatchers.IO) { supabaseManager.getCurrentUser() } ?: return
        supabaseManager.fetchRedeemedCodes(userId) { redeemedCodes, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Error checking code: $error", Toast.LENGTH_SHORT).show()
            } else if (redeemedCodes != null) {
                if (redeemedCodes.any { it.first == code }) {
                    Toast.makeText(requireContext(), "Code already redeemed", Toast.LENGTH_SHORT).show()
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        validateCode(code)
                    }
                }
            }
        }
    }

    private suspend fun validateCode(code: String) {
        supabaseManager.client.postgrest["reward_codes"]
            .select {
                filter {
                    eq("code", code)
                }
            }
            .decodeSingle<Map<String, Any>>()
            .let { document ->
                val points = document["points"] as? Int ?: 0
                val expiresAt = document["expires_at"] as? Long
                val isActive = document["is_active"] as? Boolean ?: true

                if (!isActive) {
                    Toast.makeText(requireContext(), "This code is inactive", Toast.LENGTH_SHORT).show()
                    return@let
                }

                if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), "This code has expired", Toast.LENGTH_SHORT).show()
                    return@let
                }

                // Code is valid, proceed with updating points
                CoroutineScope(Dispatchers.Main).launch {
                    updateUserPoints(points, code)
                }
            }
    }

    private suspend fun updateUserPoints(points: Int, code: String) {
        val userId = withContext(Dispatchers.IO) { supabaseManager.getCurrentUser() } ?: return
        supabaseManager.updateUserPoints(userId, userPoints + points) { success, error ->
            if (success) {
                CoroutineScope(Dispatchers.Main).launch {
                    supabaseManager.redeemCode(userId, code, points) { redeemed, redeemError ->
                        if (redeemed) {
                            userPoints += points
                            codeEditText.text.clear()

                            // Update RecyclerView dynamically
                            redeemedCodesList.add(Pair(code, points))
                            redeemedCodesAdapter.updateData(redeemedCodesList)

                            CoroutineScope(Dispatchers.Main).launch {
                                loadUserData()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Error redeeming code: $redeemError", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Error updating points: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadRedeemedCodes() {
        val userId = withContext(Dispatchers.IO) { supabaseManager.getCurrentUser() } ?: return
        supabaseManager.fetchRedeemedCodes(userId) { redeemedCodes, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Error loading redeemed codes: $error", Toast.LENGTH_SHORT).show()
            } else if (redeemedCodes != null) {
                redeemedCodesAdapter.updateData(redeemedCodes)
            }
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
