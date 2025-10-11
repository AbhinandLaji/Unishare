package com.ezio.unishare

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun RentalScreen(rentalItems: List<RentalItem> = emptyList(), userEmail: String, modifier: Modifier = Modifier) {
    // State variables for rental data
    var itemsIRented by remember { mutableStateOf<List<RentalItem>>(emptyList()) }
    var itemsIRentedOut by remember { mutableStateOf<List<RentalItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch rental requests from Firebase
    LaunchedEffect(userEmail) {
        if (userEmail.isEmpty()) {
            Log.e("RentalScreen", "No user email found")
            isLoading = false
            return@LaunchedEffect
        }

        Log.d("RentalScreen", "Fetching rentals for user: $userEmail")
        val rentRequestsRef = FirebaseDatabase.getInstance().getReference("rent_requests")

        rentRequestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("RentalScreen", "Snapshot exists: ${snapshot.exists()}, children: ${snapshot.childrenCount}")

                val rentedItems = mutableListOf<RentalItem>()
                val rentedOutItems = mutableListOf<RentalItem>()

                for (requestSnapshot in snapshot.children) {
                    try {
                        val request = requestSnapshot.getValue(RentalRequest::class.java)
                        Log.d("RentalScreen", "Processing request: ${request?.itemName}, status: ${request?.status}")

                        if (request != null && request.status == "accepted") {
                            // Items I'm renting (I am the renter)
                            if (request.renterEmail == userEmail) {
                                val rentalItem = RentalItem(
                                    id = request.itemId,
                                    name = request.itemName,
                                    price = request.duration,
                                    description = "Rented from ${request.ownerEmail}",
                                    category = "Rental",
                                    ownerName = request.ownerEmail.substringBefore("@"),
                                    ownerEmail = request.ownerEmail,
                                    imageUrl = "https://picsum.photos/seed/${request.itemId}/400"
                                )
                                rentedItems.add(rentalItem)
                                Log.d("RentalScreen", "Added to rentedItems: ${rentalItem.name}")
                            }

                            // Items I rented out (I am the owner)
                            if (request.ownerEmail == userEmail) {
                                val rentalItem = RentalItem(
                                    id = request.itemId,
                                    name = request.itemName,
                                    price = request.duration,
                                    description = "Rented to ${request.renterEmail}",
                                    category = "Rental",
                                    ownerName = request.renterName,
                                    ownerEmail = request.renterEmail,
                                    imageUrl = "https://picsum.photos/seed/${request.itemId}/400"
                                )
                                rentedOutItems.add(rentalItem)
                                Log.d("RentalScreen", "Added to rentedOutItems: ${rentalItem.name}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RentalScreen", "Error processing request: ${e.message}", e)
                    }
                }

                itemsIRented = rentedItems
                itemsIRentedOut = rentedOutItems
                isLoading = false

                Log.d("RentalScreen", "Final counts - Rented: ${rentedItems.size}, Rented Out: ${rentedOutItems.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RentalScreen", "Firebase error: ${error.message}", error.toException())
                isLoading = false
            }
        })
    }

    // UI Layout
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // Header
            Text(
                text = "My Rentals",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Items I'm Renting Section
            RentalSection(
                title = "Items I'm Renting",
                subtitle = "${itemsIRented.size} active rentals",
                items = itemsIRented,
                sectionColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Items I Rented Out Section
            RentalSection(
                title = "Items I Rented Out",
                subtitle = "${itemsIRentedOut.size} items generating income",
                items = itemsIRentedOut,
                sectionColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RentalSection(
    title: String,
    subtitle: String,
    items: List<RentalItem>,
    sectionColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = { /* Navigate to see all */ }) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "See all",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Scroller
        if (items.isEmpty()) {
            EmptyStateCard(sectionColor, modifier = Modifier.padding(horizontal = 20.dp))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    RentalItemCard(item, sectionColor)
                }
            }
        }
    }
}

@Composable
fun RentalItemCard(item: RentalItem, backgroundColor: Color) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(180.dp)
            .clickable { /* Handle click - navigate to details */ },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.price,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Product Image
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Owner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = item.ownerName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                    }
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(backgroundColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No items yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Start renting to see items here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f)
                )
            }
        }
    }
}