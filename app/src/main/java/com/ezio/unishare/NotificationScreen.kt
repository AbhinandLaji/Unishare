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
    val database = FirebaseDatabase.getInstance()

    // Fetches all rental requests where the current user is the owner
    LaunchedEffect(currentUserEmail) {
        val requestsRef = database.getReference("rent_requests")
            .orderByChild("ownerEmail").equalTo(currentUserEmail)

        requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newRequests = mutableListOf<RentalRequest>()
                for (child in snapshot.children) {
                    child.getValue(RentalRequest::class.java)?.let { newRequests.add(it) }
                }
                requests.clear()
                // Show pending requests first, then others sorted by time
                requests.addAll(newRequests.sortedWith(compareBy({ it.status != "pending" }, { -it.timestamp })))
            }
            override fun onCancelled(error: DatabaseError) {
                // You can add error handling here if needed
            }
        })
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
        if (requests.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("You have no rental requests.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
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

@Composable
fun RequestItemCard(request: RentalRequest) {
    val context = LocalContext.current

    fun updateRequestStatus(newStatus: String) {
        FirebaseDatabase.getInstance().getReference("rent_requests")
            .child(request.requestId).child("status").setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(context, "Request ${newStatus}!", Toast.LENGTH_SHORT).show()
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
                        Button(onClick = { updateRequestStatus("accepted") }, modifier = Modifier.weight(1f)) {
                            Text("Accept")
                        }
                        OutlinedButton(onClick = { updateRequestStatus("rejected") }, modifier = Modifier.weight(1f)) {
                            Text("Reject")
                        }
                    }
                }
                "accepted" -> {
                    Text("You accepted this request.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                "rejected" -> {
                    Text("You rejected this request.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}