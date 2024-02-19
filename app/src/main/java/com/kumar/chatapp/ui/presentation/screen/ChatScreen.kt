package com.kumar.chatapp.ui.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kumar.chatapp.ui.data.enums.ConnectionStatus
import com.kumar.chatapp.ui.data.enums.MessagePosition
import com.kumar.chatapp.ui.data.model.ChatMessage
import com.kumar.chatapp.ui.presentation.viewmodel.ChatScreenViewModel

data class ChatScreenUiState(
    var messages: List<ChatMessage> = listOf(),
    val userId: String = "",
    val message: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.NOT_STARTED
)

@Composable
fun ChatScreen(
    viewModel: ChatScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.observeAsState(ChatScreenUiState())
    val currentFocus = LocalFocusManager.current
    val lazyListState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "Connection Status: ${uiState.connectionStatus.name}")
        Text(text = "User Id: ${uiState.userId}")
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            state = lazyListState
        ) {
            items(uiState.messages) { message ->
                MessageItem(
                    message = message,
                    if (message.fromUserId == uiState.userId) MessagePosition.RIGHT
                    else MessagePosition.LEFT
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.message, onValueChange = viewModel::onMessageChange,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Rounded.Send,
                contentDescription = "Send Button",
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        viewModel.sendMessage(messageSent = {
                            currentFocus.clearFocus()
                        })
                    })
        }
    }
    // Ask user to enter a unique userId before using the chat functionality
    AnimatedVisibility(visible = uiState.userId.isEmpty()) {
        UserIdPrompt() {
            viewModel.setUserId(it)
        }
    }

    // When ever a new message is added to the list we want to scroll to the latest message
    LaunchedEffect(key1 = uiState.messages) {
        lazyListState.animateScrollToItem(uiState.messages.size)
    }
}

@Composable
fun UserIdPrompt(onStart: (String) -> Unit) {
    var userId by remember { mutableStateOf("") }
    Dialog(onDismissRequest = { }) {
        Card {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(value = userId, onValueChange = { userId = it },
                    label = { Text(text = "User Id") })
                Button(onClick = { onStart(userId) }) {
                    Text(text = "Start")
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: ChatMessage, messagePosition: MessagePosition) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (messagePosition == MessagePosition.LEFT) 0.dp else 24.dp,
                end = if (messagePosition == MessagePosition.RIGHT) 0.dp else 24.dp
            ),
        contentAlignment = if (messagePosition == MessagePosition.LEFT) Alignment.TopStart else Alignment.BottomEnd
    ) {
        Card {
            Column(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text(
                    text = message.fromUserId,
                    modifier = Modifier.align(if (messagePosition == MessagePosition.LEFT) Alignment.Start else Alignment.End),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewMessageItem() {
    MessageItem(message = ChatMessage("Hello", "Kumar"), MessagePosition.LEFT)
}