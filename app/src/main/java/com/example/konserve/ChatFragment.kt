package com.example.konserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatFragment : Fragment() {

    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        firebaseManager = FirebaseManager(auth, firestore)

        // Initialize UI components
        messageEditText = view.findViewById(R.id.messageEditText)
        sendButton = view.findViewById(R.id.sendButton)
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)

        // Setup RecyclerView
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatAdapter = ChatAdapter().apply {
            setCurrentUserId(auth.currentUser?.uid ?: "")
        }
        chatRecyclerView.adapter = chatAdapter

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadMessages()
        }

        // Load messages
        loadMessages()

        // Setup real-time updates
        setupRealtimeUpdates()

        // Send button click listener
        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageEditText.text.clear()
            }
        }

        return view
    }

    private fun setupRealtimeUpdates() {
        firestore.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Error loading messages!", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                snapshots?.let { documents ->
                    val messages = documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)
                    }
                    chatAdapter.submitList(messages) {
                        chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun loadMessages() {
        firebaseManager.getMessages { messages, error ->
            swipeRefreshLayout.isRefreshing = false
            if (error != null) {
                Toast.makeText(requireContext(), "Error loading messages!", Toast.LENGTH_SHORT).show()
            } else {
                chatAdapter.submitList(messages) {
                    chatRecyclerView.scrollToPosition(messages?.size?.minus(1) ?: 0)
                }
            }
        }
    }

    private fun sendMessage(messageText: String) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            // Fetch the user's full name
            firebaseManager.getUserData(currentUserId) { userData, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to retrieve user data.", Toast.LENGTH_SHORT).show()
                } else if (userData != null) {
                    val fullName = userData["fullName"] as? String ?: "Anonymous"

                    // Create message object
                    val message = hashMapOf(
                        "username" to fullName,
                        "text" to messageText,
                        "timestamp" to System.currentTimeMillis()
                    )

                    // Save the message in Firestore
                    firebaseManager.sendMessage(message) { success, error ->
                        if (success) {
                            Toast.makeText(requireContext(), "Message sent!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Error sending message.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}
