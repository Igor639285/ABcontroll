package com.abcontroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BotViewModel(
    private val repository: BotRepository = BotRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BotUiState())
    val uiState: StateFlow<BotUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var lastUpdateId: Long? = null

    fun updateToken(token: String) {
        _uiState.value = _uiState.value.copy(token = token)
    }

    fun connect() {
        pollingJob?.cancel()
        lastUpdateId = null
        _uiState.value = _uiState.value.copy(isConnected = true)
        pollingJob = viewModelScope.launch {
            while (true) {
                val token = _uiState.value.token
                if (token.isBlank()) {
                    _uiState.value = _uiState.value.copy(isConnected = false)
                    return@launch
                }
                val updates = repository.getUpdates(token, lastUpdateId?.plus(1))
                if (updates.isNotEmpty()) {
                    lastUpdateId = updates.maxOf { it.updateId }
                    val newMessages = updates.mapNotNull { it.message?.toChatMessage(it.updateId) }
                    if (newMessages.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            messages = (newMessages + _uiState.value.messages).distinctBy { it.messageId }
                        )
                    }
                }
                delay(1500)
            }
        }
    }

    fun selectMessage(message: ChatMessage) {
        _uiState.value = _uiState.value.copy(selectedMessage = message)
    }

    fun sendReply(text: String) {
        val target = _uiState.value.selectedMessage ?: return
        val token = _uiState.value.token
        if (token.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(
                token = token,
                chatId = target.chatId,
                text = text,
                replyToMessageId = target.messageId
            )
        }
    }
}

private fun TelegramMessage.toChatMessage(updateId: Long): ChatMessage {
    val authorName = listOfNotNull(from?.firstName, from?.lastName)
        .joinToString(" ")
        .ifBlank { from?.username ?: chat.title ?: "Пользователь" }

    return ChatMessage(
        updateId = updateId,
        chatId = chat.id,
        messageId = messageId,
        author = authorName,
        text = text ?: "(медиа сообщение)",
        timestamp = date
    )
}


data class BotUiState(
    val token: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val selectedMessage: ChatMessage? = null,
    val isConnected: Boolean = false
)
