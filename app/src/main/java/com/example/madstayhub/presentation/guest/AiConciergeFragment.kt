package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.databinding.FragmentAiConciergeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.madstayhub.data.remote.model.GeminiContent
import com.example.madstayhub.data.remote.model.GeminiPart

@AndroidEntryPoint
class AiConciergeFragment : Fragment() {
    private var _binding: FragmentAiConciergeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AiConciergeViewModel by viewModels()
    private lateinit var adapter: ChatMessageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAiConciergeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ChatMessageAdapter()
        binding.rvChat.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                viewModel.sendMessage(msg)
                binding.etMessage.setText("")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(viewModel.chatHistory, viewModel.streamingResponse) { history, streaming ->
                if (streaming.isNotEmpty()) {
                    history + GeminiContent(role = "model", parts = listOf(GeminiPart(streaming)))
                } else {
                    history
                }
            }.collect { fullList ->
                // Filter out the system prompt message from the visible chat
                val displayList = fullList.filter { content ->
                    val text = content.parts.firstOrNull()?.text ?: ""
                    !text.startsWith("You are StayHub's virtual butler, Aria.") && text != "Hello"
                }
                adapter.submitList(displayList) {
                    if (displayList.isNotEmpty()) {
                        binding.rvChat.scrollToPosition(displayList.size - 1)
                    }
                }
                
                // Show/hide typing indicator based on whether we are streaming a response
                val isStreaming = viewModel.streamingResponse.value.isNotEmpty()
                binding.tvTyping.visibility = if (isStreaming) View.VISIBLE else View.GONE
                if (isStreaming) {
                    binding.tvTyping.text = "Aria is typing..."
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}