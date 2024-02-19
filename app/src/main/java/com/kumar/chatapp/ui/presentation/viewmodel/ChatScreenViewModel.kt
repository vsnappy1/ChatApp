package com.kumar.chatapp.ui.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.kumar.chatapp.BuildConfig
import com.kumar.chatapp.ui.data.enums.ConnectionStatus
import com.kumar.chatapp.ui.data.model.ChatMessage
import com.kumar.chatapp.ui.data.model.toJsonString
import com.kumar.chatapp.ui.domain.service.ChatService
import com.kumar.chatapp.ui.presentation.screen.ChatScreenUiState
import com.tinder.scarlet.Message
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.WebSocket.Event.*
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class ChatScreenViewModel : ViewModel() {
    private val TAG = "ChatScreenViewModel"

    private val _uiState = MutableLiveData(ChatScreenUiState())
    val uiState: LiveData<ChatScreenUiState> = _uiState

    private var chatService: ChatService = Scarlet.Builder()
        .webSocketFactory(
            OkHttpClient.Builder().build()
                .newWebSocketFactory("wss://free.blr2.piesocket.com/v3/1?api_key=${BuildConfig.websocketApiKey}&notify_self=1")
        )
        .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
        .build().create<ChatService>()

    init {
        observerConnection()
    }

    fun sendMessage(messageSent: () -> Unit) {
        val message = message()
        if (message.message.isEmpty()) return
        chatService.sendMessage(message.toJsonString())
            .also {
                messageSent()
            }
        addMessage(message)
        clearMessage()
    }

    fun setUserId(userId: String) {
        _uiState.postValue(_uiState.value?.copy(userId = userId))
    }

    fun onMessageChange(message: String) {
        _uiState.postValue(_uiState.value?.copy(message = message))
    }

    private fun observerConnection() {
        Log.d(TAG, "Observing Connection")
        updateConnectionStatus(ConnectionStatus.CONNECTING)
        chatService.observeConnection().subscribe(
            { response ->
                Log.d(TAG, response.toString())
                onResponseReceived(response)
            },
            { error ->
                error.localizedMessage?.let { Log.e(TAG, it) }
            })
    }

    private fun onResponseReceived(response: WebSocket.Event) {
        when (response) {
            is OnConnectionOpened<*> ->
                updateConnectionStatus(ConnectionStatus.OPENED)

            is OnConnectionClosed ->
                updateConnectionStatus(ConnectionStatus.CLOSED)

            is OnConnectionClosing ->
                updateConnectionStatus(ConnectionStatus.CLOSING)

            is OnConnectionFailed ->
                updateConnectionStatus(ConnectionStatus.FAILED)

            is OnMessageReceived ->
                handleOnMessageReceived(response.message)
        }
    }

    private fun handleOnMessageReceived(message: Message) {
        Log.d(TAG, "handleOnMessageReceived: $message")
        try {
            val value = (message as Message.Text).value
            val chatMessage = Gson().fromJson(value, ChatMessage::class.java)
            if (chatMessage.fromUserId != null && chatMessage.fromUserId != uiState.value?.userId) {
                addMessage(chatMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleOnMessageReceived: ", e)
        }
    }

    private fun updateConnectionStatus(connectionStatus: ConnectionStatus) {
        _uiState.postValue(_uiState.value?.copy(connectionStatus = connectionStatus))
    }

    private fun addMessage(message: ChatMessage) {
        Log.d(TAG, "addMessage: $message")
        val messages = uiState.value?.messages?.toMutableList()
        messages?.add(message)
        _uiState.postValue(messages?.let { _uiState.value?.copy(messages = it) })
    }

    private fun clearMessage() {
        viewModelScope.launch {
            delay(50)
            _uiState.postValue(_uiState.value?.copy(message = ""))
        }
    }

    private fun message(): ChatMessage {
        return _uiState.value?.let {
            ChatMessage(message = it.message, fromUserId = it.userId)
        } ?: ChatMessage("", "")
    }
}