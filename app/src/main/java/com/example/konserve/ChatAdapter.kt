package com.example.konserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.konserve.models.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.hdodenhof.circleimageview.CircleImageView
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class ChatAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback) {
    private var currentUserId: String? = null

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        
        private object DiffCallback : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem.timestamp == newItem.timestamp
            }

            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
                return oldItem == newItem
            }
        }

        fun formatTimestamp(timestamp: String): String {
            return try {
                val zonedDateTime = ZonedDateTime.parse(timestamp)  // Parses the ISO 8601 string
                val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")  // Example: "30 Mar 2025, 04:08 PM"
                zonedDateTime.format(formatter)
            } catch (e: Exception) {
                "Invalid Date"  // Handle parsing errors
            }
        }
    }

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.username == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

        fun bind(message: Message) {
            messageTextView.text = message.text
            timestampTextView.text = formatTimestamp(message.timestamp)
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)

        fun bind(message: Message) {
            usernameTextView.text = message.username
            messageTextView.text = message.text
            timestampTextView.text = formatTimestamp(message.timestamp)
            // You can add profile image loading here using Glide
        }
    }
}
