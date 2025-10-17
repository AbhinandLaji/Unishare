package com.ezio.unishare

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController, currentUserEmail: String) {
    val requests = remember { mutableStateListOf<RentalRequest>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val database = FirebaseDatabase.getInstance()

    // Fetches all rental requests where the current user is the owner
    DisposableEffect(currentUserEmail) {
        val requestsRef = database.getReference("rent_requests")
            .orderByChild("ownerEmail").equalTo(currentUserEmail)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newRequests = mutableListOf<RentalRequest>()
                for (child in snapshot.children) {
                    child.getValue(RentalRequest::class.java)?.let { newRequests.add(it) }
                }
                requests.clear()
                // Show pending requests first, then others sorted by time
                requests.addAll(newRequests.sortedWith(compareBy({ it.status != "pending" }, { -it.timestamp })))
                isLoading = false
                errorMessage = null
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
                errorMessage = error.message
            }
        }

        requestsRef.addValueEventListener(listener)

        // Cleanup listener when composable is disposed
        onDispose {
            requestsRef.removeEventListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rental Requests") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Error loading requests",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                requests.isEmpty() -> {
                    Text("You have no rental requests.")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(requests) { request ->
                            RequestItemCard(request = request)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestItemCard(request: RentalRequest) {
    val context = LocalContext.current
    var isProcessing by remember { mutableStateOf(false) }

    fun rejectRequest() {
        isProcessing = true
        FirebaseDatabase.getInstance()
            .getReference("rent_requests")
            .child(request.requestId)
            .child("status")
            .setValue("rejected")
            .addOnSuccessListener {
                isProcessing = false
                Toast.makeText(context, "Request rejected!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isProcessing = false
                Toast.makeText(context, "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun acceptRequest() {
        isProcessing = true
        val database = FirebaseDatabase.getInstance()

        // 1. Update request status to accepted
        database.getReference("rent_requests")
            .child(request.requestId)
            .child("status")
            .setValue("accepted")
            .addOnSuccessListener {
                // 2. Parse duration and calculate end time
                val durationInMillis = parseDurationToMillis(request.duration)
                val startTime = System.currentTimeMillis()
                val endTime = startTime + durationInMillis

                // 3. Create ActiveRental object
                val activeRental = ActiveRental(
                    requestId = request.requestId,
                    itemId = request.itemId,
                    itemName = request.itemName,
                    ownerEmail = request.ownerEmail,
                    renterEmail = request.renterEmail,
                    rentalStartTime = startTime,
                    rentalEndTime = endTime
                )

                // 4. Save to active_rentals node
                database.getReference("active_rentals")
                    .child(request.requestId)
                    .setValue(activeRental)
                    .addOnSuccessListener {
                        isProcessing = false
                        Toast.makeText(
                            context,
                            "Request accepted! Rental timer started.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        isProcessing = false
                        // Rollback the status change
                        database.getReference("rent_requests")
                            .child(request.requestId)
                            .child("status")
                            .setValue("pending")

                        Toast.makeText(
                            context,
                            "Failed to create rental: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                isProcessing = false
                Toast.makeText(
                    context,
                    "Failed to accept request: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Request for: ${request.itemName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("From: ${request.renterName}")
            Text("Duration: ${request.duration}")
            Spacer(modifier = Modifier.height(16.dp))

            // Show different UI based on the request status
            when (request.status) {
                "pending" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { acceptRequest() },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Accept")
                            }
                        }
                        OutlinedButton(
                            onClick = { rejectRequest() },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing
                        ) {
                            Text("Reject")
                        }
                    }
                }
                "accepted" -> {
                    Text(
                        "You accepted this request. Timer started!",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                "rejected" -> {
                    Text(
                        "You rejected this request.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Parse duration string to milliseconds
 * Supports formats like:
 * - "3 days" or "3d"
 * - "2 weeks" or "2w"
 * - "5 hours" or "5h"
 * - "1 month" or "1m" or "1mo"
 */
fun parseDurationToMillis(duration: String?): Long {
    if (duration.isNullOrBlank()) return 24 * 60 * 60 * 1000 // Default 1 day

    val normalized = duration.lowercase().trim()

    // Try to extract number and unit
    val pattern = """(\d+)\s*(day|days|d|week|weeks|w|hour|hours|h|month|months|mo|m)""".toRegex()
    val matchResult = pattern.find(normalized)

    if (matchResult != null) {
        val number = matchResult.groupValues[1].toLongOrNull() ?: 1
        val unit = matchResult.groupValues[2]

        return when {
            unit.startsWith("d") -> number * 24 * 60 * 60 * 1000 // days
            unit.startsWith("w") -> number * 7 * 24 * 60 * 60 * 1000 // weeks
            unit.startsWith("h") -> number * 60 * 60 * 1000 // hours
            unit == "mo" || unit == "month" || unit == "months" -> number * 30 * 24 * 60 * 60 * 1000 // months
            unit == "m" -> {
                // Could be minutes or months - assume months if number is small
                if (number <= 12) number * 30 * 24 * 60 * 60 * 1000 // months
                else number * 60 * 1000 // minutes
            }
            else -> 24 * 60 * 60 * 1000 // default 1 day
        }
    }

    // Default to 1 day if parsing fails
    return 24 * 60 * 60 * 1000
}