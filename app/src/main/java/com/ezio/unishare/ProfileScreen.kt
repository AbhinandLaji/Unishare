package com.ezio.unishare

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // We keep firebaseAuth here in case you want to use it for other things, but it's not used for logout.
    val firebaseAuth = FirebaseAuth.getInstance()
    
    // State variables to hold user details
    var firstName by remember { mutableStateOf("Loading...") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("Loading...") }

    // This data fetching logic remains the same and is correct.
    LaunchedEffect(key1 = userEmail) {
        if (userEmail.isNotBlank()) {
            val database = FirebaseDatabase.getInstance()
            val usersRef = database.getReference("users")
            val userKey = userEmail.replace(".", "_")

            usersRef.child(userKey).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    firstName = snapshot.child("firstName").getValue(String::class.java) ?: "User"
                    lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    phone = snapshot.child("phone").getValue(String::class.java) ?: "N/A"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileScreen", "Failed to load user data.", error.toException())
                    Toast.makeText(context, "Failed to load user data.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    val fullName = if (firstName != "Loading..." && lastName.isNotEmpty()) "$firstName $lastName" else firstName
    val initials = if (firstName.isNotEmpty() && firstName != "Loading...") {
        if (lastName.isNotEmpty()) "${firstName.first()}${lastName.first()}" else firstName.first().toString()
    } else "UN"

    // UI code remains the same...
    val darkBackground = Color(0xFF1C1C2D)
    val cardBackground = Color(0xFF2D2D44)
    val blueAccent = Color(0xFF4285F4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ... (Avatar, Name, Details Card UI code is unchanged)
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(blueAccent), contentAlignment = Alignment.Center) {
                Text(text = initials, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = fullName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardBackground), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileDetailRow(Icons.Default.Person, "Full Name", fullName)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailRow(Icons.Default.Email, "College Email", userEmail)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailRow(Icons.Default.Call, "Phone Number", phone)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // --- THIS IS THE FIX ---

                    // 1. Get the SharedPreferences your app uses for session management.
                    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

                    // 2. Clear all saved session data (isLoggedIn = false).
                    sharedPref.edit().clear().apply()

                    // 3. Show a success message.
                    Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                    
                    // 4. Navigate to the Login Screen (MainActivity) and clear the activity stack.
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = blueAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun ProfileDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.LightGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
            Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}