package com.example.konserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TalkMenuFragment : Fragment() {
    private lateinit var backButton: ImageView
    private lateinit var feedbackEditText: EditText
    private lateinit var submitButton: Button

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

        val feedbackData = hashMapOf(
            "feedback" to feedback,
            "userId" to FirebaseAuth.getInstance().currentUser?.uid,
            "timestamp" to Date()
        )

        FirebaseFirestore.getInstance()
            .collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                Toast.makeText(context, "Feedback submitted successfully", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                submitButton.isEnabled = true
                Toast.makeText(context, "Failed to submit feedback: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}