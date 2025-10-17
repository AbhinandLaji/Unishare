package com.ezio.unishare

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRentalsScreen(navController: NavController, currentUserEmail: String) {
    val myRentals = remember { mutableStateListOf<ActiveRental>() }
    val rentedByMe = remember { mutableStateListOf<ActiveRental>() }
    val database = FirebaseDatabase.getInstance()
    val context = LocalContext.current

    LaunchedEffect(currentUserEmail) {
        val ownerRef = database.getReference("active_rentals")
        ownerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ownerRentals = mutableListOf<ActiveRental>()
                val renterRentals = mutableListOf<ActiveRental>()

                for (child in snapshot.children) {
                    val rental = child.getValue(ActiveRental::class.java)
                    if (rental != null) {
                        if (rental.ownerEmail == currentUserEmail) {
                            ownerRentals.add(rental)
                        }
                        if (rental.renterEmail == currentUserEmail) {
                            renterRentals.add(rental)
                        }
                    }
                }

                myRentals.clear()
                myRentals.addAll(ownerRentals.sortedByDescending { it.rentalStartTime })

                rentedByMe.clear()
                rentedByMe.addAll(renterRentals.sortedByDescending { it.rentalStartTime })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ActiveRentals", "Error: ${error.message}")
                Toast.makeText(context, "Error loading rentals", Toast.LENGTH_SHORT).show()
            }
        })
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Rented Out", "My Rentals")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Rentals") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> RentalsList(
                    rentals = myRentals,
                    emptyMessage = "No items currently rented out",
                    currentUserEmail = currentUserEmail
                )
                1 -> RentalsList(
                    rentals = rentedByMe,
                    emptyMessage = "You haven't rented any items",
                    currentUserEmail = currentUserEmail
                )
            }
        }
    }
}

@Composable
fun RentalsList(
    rentals: List<ActiveRental>,
    emptyMessage: String,
    currentUserEmail: String
) {
    if (rentals.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(emptyMessage, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rentals) { rental ->
                RentalItemCard(
                    rental = rental,
                    currentUserEmail = currentUserEmail
                )
            }
        }
    }
}

@Composable
fun RentalItemCard(
    rental: ActiveRental,
    currentUserEmail: String
) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()
    var showReturnDialog by remember { mutableStateOf(false) }

    val isOwner = rental.ownerEmail.trim().equals(currentUserEmail.trim(), ignoreCase = true)

    // LOG IT
    Log.d("RentalCard", "Card for ${rental.itemName}: isOwner=$isOwner (owner='${rental.ownerEmail}' current='$currentUserEmail')")

    var timeRemaining by remember { mutableStateOf(rental.rentalEndTime - System.currentTimeMillis()) }
    val isExpired = timeRemaining <= 0

    LaunchedEffect(rental.requestId) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining = rental.rentalEndTime - System.currentTimeMillis()
        }
    }

    val days = TimeUnit.MILLISECONDS.toDays(timeRemaining).coerceAtLeast(0)
    val hours = (TimeUnit.MILLISECONDS.toHours(timeRemaining) % 24).coerceAtLeast(0)
    val minutes = (TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60).coerceAtLeast(0)
    val seconds = (TimeUnit.MILLISECONDS.toSeconds(timeRemaining) % 60).coerceAtLeast(0)

    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Confirm Item Return") },
            text = { Text("Are you sure the item has been returned by the renter?") },
            confirmButton = {
                Button(
                    onClick = {
                        database.getReference("active_rentals")
                            .child(rental.requestId)
                            .removeValue()
                            .addOnSuccessListener {
                                database.getReference("rent_requests")
                                    .child(rental.requestId)
                                    .child("status")
                                    .setValue("returned")
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "✓ Item marked as returned!", Toast.LENGTH_SHORT).show()
                                        showReturnDialog = false
                                    }
                            }
                    }
                ) {
                    Text("Yes, Mark as Returned")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = rental.itemName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isOwner) "Rented to: ${rental.renterEmail}" else "Rented from: ${rental.ownerEmail}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            // MEGA DEBUG BOX
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Yellow)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🔥 DEBUG 🔥", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    Text("isOwner = $isOwner", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isOwner) Color.Green else Color.Red)
                    Text("owner: '${rental.ownerEmail}'", fontSize = 12.sp, color = Color.Black)
                    Text("current: '$currentUserEmail'", fontSize = 12.sp, color = Color.Black)
                    Text("Match? ${rental.ownerEmail == currentUserEmail}", fontSize = 14.sp, color = Color.Blue)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(24.dp), tint = if (isExpired) Color(0xFFD32F2F) else Color(0xFF1976D2))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExpired) "Time Expired!" else "Time Remaining:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpired) Color(0xFFD32F2F) else Color.Black
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (!isExpired) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TimeBox(value = days.toString().padStart(2, '0'), label = "Days")
                    TimeBox(value = hours.toString().padStart(2, '0'), label = "Hours")
                    TimeBox(value = minutes.toString().padStart(2, '0'), label = "Min")
                    TimeBox(value = seconds.toString().padStart(2, '0'), label = "Sec")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // SUPER OBVIOUS BUTTON TEST
            Text("🟢 BUTTON AREA START 🟢", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Green, modifier = Modifier.fillMaxWidth())

            if (isOwner) {
                Text("✅ IS OWNER TRUE - BUTTONS SHOULD APPEAR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Green)

                Button(
                    onClick = { showReturnDialog = true },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("🔴 MARK AS RETURNED 🔴", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { Toast.makeText(context, "Contact button clicked!", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("💬 CONTACT RENTER 💬", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("❌ IS OWNER FALSE - RENTER VIEW", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)

                Button(
                    onClick = { Toast.makeText(context, "Contact owner clicked!", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Magenta)
                ) {
                    Text("💬 CONTACT OWNER 💬", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text("🔴 BUTTON AREA END 🔴", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun TimeBox(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
