package com.ezio.unishare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

import com.ezio.unishare.ui.theme.PeerRentTheme
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

// Data class for a user
data class ChatUser(val email: String = "", val firstName: String = "", val lastName: String = "")

// Data class for a message
data class ChatMessage(
    val text: String = "",
    val senderEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class ChatActivity : ComponentActivity() {

    private val database = FirebaseDatabase.getInstance()
    private lateinit var currentUserEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUserEmail = intent.getStringExtra("CURRENT_USER_EMAIL") ?: "Unknown User"

        setContent {
            PeerRentTheme {
                ChatApp(
                    currentUserEmail = currentUserEmail,
                    database = database,
                    onFinishActivity = { finish() }
                )
            }
        }
    }
}

@Composable
fun ChatApp(currentUserEmail: String, database: FirebaseDatabase, onFinishActivity: () -> Unit) {
    var selectedUser by remember { mutableStateOf<ChatUser?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedVisibility(
            visible = selectedUser == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            UserSearchScreen(
                database = database,
                currentUserEmail = currentUserEmail,
                onUserSelected = { user -> selectedUser = user },
                onBack = onFinishActivity
            )
        }

        AnimatedVisibility(
            visible = selectedUser != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedUser?.let { user ->
                ChatScreen(
                    currentUserEmail = currentUserEmail,
                    recipient = user,
                    database = database,
                    onBack = { selectedUser = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    database: FirebaseDatabase,
    currentUserEmail: String,
    onUserSelected: (ChatUser) -> Unit,
    onBack: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var userList by remember { mutableStateOf<List<ChatUser>>(emptyList()) }

    LaunchedEffect(Unit) {
        val usersRef = database.getReference("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull {
                    // Manually map fields to handle inconsistencies like "collegeMail" vs "email"
                    val data = it.value as? Map<*, *>
                    if (data != null) {
                        ChatUser(
                            email = data["collegeMail"] as? String ?: "",
                            firstName = data["firstName"] as? String ?: "",
                            lastName = data["lastName"] as? String ?: ""
                        )
                    } else {
                        null
                    }
                }
                userList = users.filter { it.email.isNotEmpty() && it.email != currentUserEmail }
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        })
    }

    val filteredUsers = userList.filter {
        it.firstName.contains(searchText, ignoreCase = true) ||
        it.lastName.contains(searchText, ignoreCase = true) ||
        it.email.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Message") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search by name or email") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredUsers) { user ->
                    UserListItem(user = user, onClick = { onUserSelected(user) })
                }
            }
        }
    }
}

@Composable
fun UserListItem(user: ChatUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.firstName.firstOrNull()?.uppercase() ?: "",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUserEmail: String,
    recipient: ChatUser,
    database: FirebaseDatabase,
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var currentMessage by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val chatID = remember {
        listOf(currentUserEmail.replace(".", ""), recipient.email.replace(".", "")).sorted().joinToString("_")
    }

    DisposableEffect(chatID) {
        val messagesRef = database.getReference("chats").child(chatID)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
            }
            override fun onCancelled(error: DatabaseError) { /* Handle error */ }
        }
        messagesRef.addValueEventListener(listener)

        onDispose {
            messagesRef.removeEventListener(listener)
        }
    }

    fun sendMessage() {
        if (currentMessage.isNotBlank()) {
            val message = ChatMessage(
                text = currentMessage,
                senderEmail = currentUserEmail,
                timestamp = System.currentTimeMillis()
            )
            database.getReference("chats").child(chatID).push().setValue(message)
            currentMessage = ""
            keyboardController?.hide()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${recipient.firstName} ${recipient.lastName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list")
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                value = currentMessage,
                onValueChange = { currentMessage = it },
                onSend = { sendMessage() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(
                    message = message,
                    isCurrentUser = message.senderEmail == currentUserEmail
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isCurrentUser: Boolean) {
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    val time = sdf.format(Date(message.timestamp))

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp) // Set a max width for bubbles
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(12.dp),
            horizontalAlignment = if(isCurrentUser) Alignment.End else Alignment.Start
        ) {
            Text(text = message.text, color = textColor, style = MaterialTheme.typography.bodyLarge)
            Text(text = time, color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun MessageInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions { onSend() }
            )
            IconButton(onClick = onSend) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}