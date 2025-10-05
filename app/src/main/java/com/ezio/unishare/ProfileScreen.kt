package com.ezio.unishare

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, userEmail: String) {
    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()
    val isDarkTheme = isSystemInDarkTheme()

    // State variables to hold user details
    var firstName by remember { mutableStateOf("Loading...") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("Loading...") }

    // State to control the visibility of edit dialogs
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }

    val userKey = userEmail.replace(".", "_")
    val usersRef = FirebaseDatabase.getInstance().getReference("users")

    // Fetch user data
    LaunchedEffect(key1 = userEmail) {
        if (userEmail.isNotBlank()) {
            usersRef.child(userKey).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    firstName = snapshot.child("firstName").getValue(String::class.java) ?: "User"
                    lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    phone = snapshot.child("phone").getValue(String::class.java) ?: "N/A"
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileScreen", "Failed to load user data.", error.toException())
                }
            })
        }
    }

    val fullName = if (firstName != "Loading..." && lastName.isNotEmpty()) "$firstName $lastName" else firstName
    val initials = if (firstName.isNotEmpty() && firstName != "Loading...") {
        if (lastName.isNotEmpty()) "${firstName.first()}${lastName.first()}" else firstName.first().toString()
    } else "UN"

    // --- DIALOGS FOR EDITING ---
    if (showEditNameDialog) {
        EditInfoDialog(
            title = "Change Name",
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newValues ->
                val newFirstName = newValues[0]
                val newLastName = newValues[1]
                val updates = mapOf("firstName" to newFirstName, "lastName" to newLastName)

                usersRef.child(userKey).updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        firstName = newFirstName // Update UI state
                        lastName = newLastName  // Update UI state
                        Toast.makeText(context, "Name updated successfully", Toast.LENGTH_SHORT).show()
                        showEditNameDialog = false
                    } else {
                        Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            initialValues = listOf(firstName, lastName),
            labels = listOf("First Name", "Last Name")
        )
    }

    if (showEditPhoneDialog) {
        EditInfoDialog(
            title = "Change Phone Number",
            onDismiss = { showEditPhoneDialog = false },
            onConfirm = { newValues ->
                val newPhone = newValues[0]
                val updates = mapOf("phone" to newPhone)

                usersRef.child(userKey).updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        phone = newPhone // Update UI state
                        Toast.makeText(context, "Phone updated successfully", Toast.LENGTH_SHORT).show()
                        showEditPhoneDialog = false
                    } else {
                        Toast.makeText(context, "Failed to update phone", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            initialValues = listOf(phone),
            labels = listOf("Phone Number")
        )
    }

    // --- UI CODE (THEME AND SCAFFOLD) ---
    val backgroundColor = if (isDarkTheme) Color(0xFF1C1C2D) else Color(0xFFF5F5F5)
    val cardBackground = if (isDarkTheme) Color(0xFF2D2D44) else Color.White
    val textPrimaryColor = if (isDarkTheme) Color.White else Color(0xFF1C1C2D)
    val textSecondaryColor = if (isDarkTheme) Color.Gray else Color(0xFF666666)
    val textTertiaryColor = if (isDarkTheme) Color.LightGray else Color(0xFF999999)
    val blueAccent = Color(0xFF4285F4)
    val topBarColor = if (isDarkTheme) Color(0xFF1C1C2D) else Color(0xFF4285F4)
    val topBarContentColor = Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = topBarContentColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = topBarContentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Avatar, Name, etc. (No changes here)
            Box(
                modifier = Modifier.size(100.dp).clip(CircleShape).background(blueAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(text = initials, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = fullName, color = textPrimaryColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            // Details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 4.dp else 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // --- MODIFIED ProfileDetailRow CALLS ---
                    ProfileDetailRow(
                        icon = Icons.Default.Person,
                        label = "Full Name",
                        value = fullName,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        iconColor = textTertiaryColor,
                        onEditClick = { showEditNameDialog = true } // Trigger name dialog
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailRow(
                        icon = Icons.Default.Email,
                        label = "College Email",
                        value = userEmail,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        iconColor = textTertiaryColor
                        // No edit button for email
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailRow(
                        icon = Icons.Default.Call,
                        label = "Phone Number",
                        value = phone,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        iconColor = textTertiaryColor,
                        onEditClick = { showEditPhoneDialog = true } // Trigger phone dialog
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout button (No changes here)
            Button(
                onClick = {
                    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                    Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent); (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = blueAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.ExitToApp, "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

// --- UPDATED ProfileDetailRow COMPOSABLE ---
@Composable
fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    iconColor: Color,
    onEditClick: (() -> Unit)? = null // Optional click listener for edit button
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { // Use weight to push icon to the end
            Text(text = label, color = textSecondaryColor, fontSize = 12.sp)
            Text(text = value, color = textPrimaryColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        // Show edit button only if onEditClick is provided
        if (onEditClick != null) {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit $label", tint = textSecondaryColor)
            }
        }
    }
}

// --- NEW REUSABLE EditInfoDialog COMPOSABLE ---
@Composable
fun EditInfoDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    initialValues: List<String>,
    labels: List<String>
) {
    val textStates = remember { initialValues.map { mutableStateOf(it) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                textStates.forEachIndexed { index, state ->
                    OutlinedTextField(
                        value = state.value,
                        onValueChange = { state.value = it },
                        label = { Text(labels[index]) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (index < textStates.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(textStates.map { it.value }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}