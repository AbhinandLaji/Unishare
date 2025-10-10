package com.ezio.unishare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezio.unishare.ui.theme.PeerRentTheme
import com.google.firebase.database.*

data class Message(
    val text: String = "",
    val senderEmail: String = "",
    val timestamp: Long = 0
)

class ConversationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUserEmail = intent.getStringExtra("CURRENT_USER_EMAIL") ?: ""
        val otherUserEmail = intent.getStringExtra("OTHER_USER_EMAIL") ?: ""

        setContent {
            PeerRentTheme {
                ConversationScreen(currentUserEmail, otherUserEmail)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(currentUserEmail: String, otherUserEmail: String) {
    val messages = remember { mutableStateListOf<Message>() }
    var newMessage by remember { mutableStateOf("") }
    var otherUserName by remember { mutableStateOf(otherUserEmail.substringBefore('@')) }


    // Create a unique, consistent chat ID for the two users
    val chatId = remember(currentUserEmail, otherUserEmail) {
        listOf(currentUserEmail, otherUserEmail).sorted().joinToString("_")
            .replace(".", "-").replace("@", "-")
    }

    val databaseRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId)
    val chatsRef = FirebaseDatabase.getInstance().getReference("chats")

    // Get other user's name
    LaunchedEffect(otherUserEmail) {
        val userKey = otherUserEmail.replace(".", "_")
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userKey)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java)
                if (firstName != null) {
                    otherUserName = firstName
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Listen for new messages
    LaunchedEffect(chatId) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newMessages = mutableListOf<Message>()
                for (childSnapshot in snapshot.children) {
                    val msg = childSnapshot.getValue(Message::class.java)
                    if (msg != null) {
                        newMessages.add(msg)
                    }
                }
                messages.clear()
                messages.addAll(newMessages)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        databaseRef.addValueEventListener(listener)
    }

    fun sendMessage() {
        if (newMessage.isNotBlank()) {
            val message = Message(
                text = newMessage,
                senderEmail = currentUserEmail,
                timestamp = System.currentTimeMillis()
            )
            databaseRef.push().setValue(message)

            // Update the chat session for both users
            val currentUserChatId = currentUserEmail.replace(".", "-").replace("@", "-")
            val otherUserChatId = otherUserEmail.replace(".", "-").replace("@", "-")

            val chatSessionForCurrentUser = ChatSession(
                otherUserEmail = otherUserEmail,
                lastMessage = newMessage,
                timestamp = message.timestamp
            )
            val chatSessionForOtherUser = ChatSession(
                otherUserEmail = currentUserEmail,
                lastMessage = newMessage,
                timestamp = message.timestamp
            )

            chatsRef.child(currentUserChatId).child(otherUserChatId).setValue(chatSessionForCurrentUser)
            chatsRef.child(otherUserChatId).child(currentUserChatId).setValue(chatSessionForOtherUser)

            newMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUserName) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            MessageInputBar(value = newMessage, onValueChange = { newMessage = it }, onSend = { sendMessage() })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message, isCurrentUser = message.senderEmail == currentUserEmail)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isCurrentUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                    bottomEnd = if (isCurrentUser) 0.dp else 16.dp
                ))
                .background(if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text,
                color = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
            }
        }
    }
}