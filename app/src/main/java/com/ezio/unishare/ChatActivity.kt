package com.ezio.unishare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezio.unishare.ui.theme.PeerRentTheme
import com.google.firebase.database.*

data class ChatSession(
    val otherUserEmail: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0
)

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUserEmail = intent.getStringExtra("CURRENT_USER_EMAIL") ?: ""
        setContent {
            PeerRentTheme {
                ChatListScreen(currentUserEmail)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(currentUserEmail: String) {
    val chatSessions = remember { mutableStateListOf<ChatSession>() }
    val context = LocalContext.current

    // Fetch chat sessions from Firebase
    LaunchedEffect(currentUserEmail) {
        val currentUserChatId = currentUserEmail.replace(".", "-").replace("@", "-")
        val databaseRef = FirebaseDatabase.getInstance().getReference("chats").child(currentUserChatId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessions = mutableListOf<ChatSession>()
                for (childSnapshot in snapshot.children) {
                    val session = childSnapshot.getValue(ChatSession::class.java)
                    session?.let { sessions.add(it) }
                }
                chatSessions.clear()
                chatSessions.addAll(sessions.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        databaseRef.addValueEventListener(listener)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Chats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (chatSessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("You have no active chats.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(8.dp)
            ) {
                items(chatSessions) { session ->
                    ChatListItem(session = session) {
                        val intent = Intent(context, ConversationActivity::class.java).apply {
                            putExtra("CURRENT_USER_EMAIL", currentUserEmail)
                            putExtra("OTHER_USER_EMAIL", session.otherUserEmail)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun UserNameFromEmail(email: String, style: androidx.compose.ui.text.TextStyle) {
    var name by remember { mutableStateOf(email.substringBefore('@')) }

    LaunchedEffect(email) {
        val userKey = email.replace(".", "_")
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userKey)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java)
                if (firstName != null) {
                    name = firstName
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Keep the default name (substring)
            }
        })
    }
    Text(text = name, style = style)
}

@Composable
fun ChatListItem(session: ChatSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Person, contentDescription = "User Icon", modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                UserNameFromEmail(email = session.otherUserEmail, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = session.lastMessage,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}