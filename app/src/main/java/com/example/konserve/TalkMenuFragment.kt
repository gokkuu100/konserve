package com.example.konserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TalkMenuFragment : Fragment() {
    private lateinit var backButton: ImageView
    private lateinit var feedbackEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var supabaseManager: SupabaseManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_talk_menu, container, false)

        // Initialize views
        backButton = view.findViewById(R.id.backButton)
        feedbackEditText = view.findViewById(R.id.feedbackEditText)
        submitButton = view.findViewById(R.id.submitButton)

        supabaseManager = SupabaseManager(requireContext())
        setupClickListeners()

        return view
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        submitButton.setOnClickListener {
            submitFeedback()
        }
    }

    private fun submitFeedback() {
        val feedback = feedbackEditText.text.toString()

        if (feedback.isEmpty()) {
            feedbackEditText.error = "Please enter your feedback"
            return
        }

        submitButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val userId = supabaseManager.getCurrentUser()
            if (userId == null) {
                withContext(Dispatchers.Main) {
                    submitButton.isEnabled = true
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val feedbackData = mapOf(
                "feedback" to feedback,
                "user_id" to userId,
                "timestamp" to System.currentTimeMillis()
            )

            val success = supabaseManager.saveReport(feedbackData)

            withContext(Dispatchers.Main) {
                submitButton.isEnabled = true
                if (success) {
                    Toast.makeText(context, "Feedback submitted successfully", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
