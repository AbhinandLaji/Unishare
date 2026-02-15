package com.ezio.unishare

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, userEmail: String) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    // --- SESSION DATA ---
    // Instead of Firebase, we pull the saved name from SharedPreferences
    val sharedPref = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    val userName = sharedPref.getString("userName", "User") ?: "User"

    // --- UI THEME COLORS ---
    val backgroundColor = if (isDarkTheme) Color(0xFF1C1C2D) else Color(0xFFF5F5F5)
    val cardBackground = if (isDarkTheme) Color(0xFF2D2D44) else Color.White
    val textPrimaryColor = if (isDarkTheme) Color.White else Color(0xFF1C1C2D)
    val textSecondaryColor = if (isDarkTheme) Color.Gray else Color(0xFF666666)
    val textTertiaryColor = if (isDarkTheme) Color.LightGray else Color(0xFF999999)
    val blueAccent = Color(0xFF4285F4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = Color.White) },
                      navigationIcon = {
                          IconButton(onClick = { navController.navigateUp() }) {
                              Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                          }
                      },
                      colors = TopAppBarDefaults.topAppBarColors(containerColor = blueAccent)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp, vertical = 16.dp),
               horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Avatar
            Box(
                modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(blueAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(2).uppercase(),
                     color = Color.White,
                     fontSize = 40.sp,
                     fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = userName, color = textPrimaryColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            // Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                 shape = RoundedCornerShape(16.dp),
                 colors = CardDefaults.cardColors(containerColor = cardBackground),
                 elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileDetailRow(
                        icon = Icons.Default.Person,
                        label = "Full Name",
                        value = userName,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        iconColor = textTertiaryColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileDetailRow(
                        icon = Icons.Default.Email,
                        label = "College Email",
                        value = userEmail,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        iconColor = textTertiaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // FIXED LOGOUT BUTTON
            Button(
                onClick = {
                    // 1. Clear Local Session
                    sharedPref.edit().clear().apply()

                    // 2. Redirect to Login Screen
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)

                    // 3. Kill HomeActivity stack
                    (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                   shape = RoundedCornerShape(12.dp),
                   colors = ButtonDefaults.buttonColors(containerColor = blueAccent)
            ) {
                Icon(Icons.Default.ExitToApp, "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    iconColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, color = textSecondaryColor, fontSize = 12.sp)
            Text(text = value, color = textPrimaryColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
