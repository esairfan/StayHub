package com.example.madstayhub.presentation.guest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.madstayhub.data.remote.model.GeminiContent
import com.example.madstayhub.databinding.ItemChatMessageReceivedBinding
import com.example.madstayhub.databinding.ItemChatMessageSentBinding

class ChatMessageAdapter : ListAdapter<GeminiContent, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).role == "user") TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            UserViewHolder(ItemChatMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            AiViewHolder(ItemChatMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val content = getItem(position)
        val text = content.parts.firstOrNull()?.text ?: ""
        val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        if (holder is UserViewHolder) holder.bind(text, timeStr)
        else if (holder is AiViewHolder) holder.bind(text, timeStr)
    }

    class UserViewHolder(private val binding: ItemChatMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String, timeStr: String) { 
            binding.tvMessage.text = text 
            binding.tvTime.text = timeStr
        }
    }

    class AiViewHolder(private val binding: ItemChatMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String, timeStr: String) { 
            binding.tvMessage.text = text 
            binding.tvTime.text = timeStr
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<GeminiContent>() {
        override fun areItemsTheSame(oldItem: GeminiContent, newItem: GeminiContent) = oldItem == newItem
        override fun areContentsTheSame(oldItem: GeminiContent, newItem: GeminiContent) = oldItem.parts == newItem.parts
    }
}
