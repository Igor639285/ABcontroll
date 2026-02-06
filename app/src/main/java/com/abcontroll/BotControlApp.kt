package com.abcontroll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotControlApp(viewModel: BotViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorResource(id = R.color.telegram_surface))
        ) {
            TokenSection(
                token = uiState.token,
                isConnected = uiState.isConnected,
                onTokenChange = viewModel::updateToken,
                onConnect = viewModel::connect
            )
            Divider()
            MessageList(
                messages = uiState.messages,
                onReply = viewModel::selectMessage
            )
            Divider()
            ReplySection(
                selected = uiState.selectedMessage,
                onSend = viewModel::sendReply
            )
        }
    }
}

@Composable
private fun TokenSection(
    token: String,
    isConnected: Boolean,
    onTokenChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isConnected) "Подключено" else "Введите токен бота",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                modifier = Modifier.weight(1f),
                label = { Text("API Token") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = onConnect, enabled = token.isNotBlank()) {
                Text(if (isConnected) "Обновить" else "Подключить")
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    onReply: (ChatMessage) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message ->
            MessageCard(message = message, onReply = { onReply(message) })
        }
        if (messages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Сообщений пока нет",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: ChatMessage, onReply: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.author,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
                Text(
                    text = message.formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onReply) {
                    Text("Ответить")
                }
            }
        }
    }
}

@Composable
private fun ReplySection(
    selected: ChatMessage?,
    onSend: (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = selected?.let { "Ответ для ${it.author}" } ?: "Выберите сообщение",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = selected != null,
            label = { Text("Сообщение") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onSend(replyText)
                replyText = ""
            },
            enabled = selected != null && replyText.isNotBlank(),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Отправить")
        }
    }
}

private val ChatMessage.formattedTime: String
    get() {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp * 1000L))
    }
