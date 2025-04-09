package com.example.konserve

import android.os.Bundle
import android.util.Log
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
import com.example.konserve.models.Message
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeFormatter

class ChatFragment : Fragment() {



    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var supabaseManager: SupabaseManager
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        supabaseManager = SupabaseManager(requireContext());

        messageEditText = view.findViewById(R.id.messageEditText)
        sendButton = view.findViewById(R.id.sendButton)
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)

        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        CoroutineScope(Dispatchers.Main).launch {
            val currentUserId = supabaseManager.getCurrentUser() ?: ""
            chatAdapter = ChatAdapter().apply {
                setCurrentUserId(currentUserId)
            }
            chatRecyclerView.adapter = chatAdapter
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadMessages()
        }

        loadMessages()
        setupRealtimeUpdates()

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
        val channel = supabaseManager.client.realtime.channel("public:messages")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Connect to Supabase Realtime
                supabaseManager.client.realtime.connect()

                // Listen for INSERT (New messages)
                launch {
                    channel.postgresChangeFlow<PostgresAction.Insert>("public") {
                        table = "messages"
                    }.collect { change ->
                        withContext(Dispatchers.Main) {
                            loadMessages()
                        }
                    }
                }

                // Listen for UPDATE (Edited messages)
                launch {
                    channel.postgresChangeFlow<PostgresAction.Update>("public") {
                        table = "messages"
                    }.collect { change ->
                        withContext(Dispatchers.Main) {
                            loadMessages()
                        }
                    }
                }

                // Listen for DELETE (Deleted messages)
                launch {
                    channel.postgresChangeFlow<PostgresAction.Delete>("public") {
                        table = "messages"
                    }.collect { change ->
                        withContext(Dispatchers.Main) {
                            loadMessages()
                        }
                    }
                }

                // Subscribe to the channel
                channel.subscribe()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error setting up real-time updates", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun sendMessage(messageText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentUserId = try {
                supabaseManager.getCurrentUser()
            } catch (e: Exception) {
                Log.e("ChatFragment", "Error retrieving current user ID", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error retrieving user ID", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            if (currentUserId == null) {
                Log.e("ChatFragment", "User not logged in.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                supabaseManager.getUserData(currentUserId) { userData, error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (error != null) {
                            Log.e("ChatFragment", "Failed to retrieve user data: $error")
                            Toast.makeText(requireContext(), "Failed to retrieve user data: $error", Toast.LENGTH_SHORT).show()
                        } else if (userData != null) {
                            Log.d("ChatFragment", "User data retrieved successfully: $userData")
                            val fullName = userData.full_name as? String ?: "Anonymous"

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val message = Message(
                                        timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                                        username = fullName,
                                        text = messageText,
                                        user_id = currentUserId
                                    )
                                    Log.d("ChatFragment", "Sending message: $message")
                                    supabaseManager.client.postgrest["messages"].insert(message)

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(requireContext(), "Message sent!", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("ChatFragment", "Error sending message", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(requireContext(), "Error sending message.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Log.e("ChatFragment", "User data is null.")
                            Toast.makeText(requireContext(), "Error: User data not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatFragment", "Error retrieving user data", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error retrieving user data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadMessages() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val messages: List<Message> = withContext(Dispatchers.IO) {
                    supabaseManager.client.postgrest["messages"]
                        .select {
                            order("timestamp", Order.ASCENDING)
                        }
                        .decodeList<Message>()  // Ensure Message is serializable
                }
                Log.d("ChatFragment", "Messages loaded: ${messages.size} messages")
                swipeRefreshLayout.isRefreshing = false
                chatAdapter.submitList(messages) {
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                swipeRefreshLayout.isRefreshing = false
                Log.e("ChatFragment", "Error loading messages: ${e.localizedMessage}", e)
                Toast.makeText(requireContext(), "Error loading messages!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            supabaseManager.client.realtime.disconnect()
        }
    }
}
