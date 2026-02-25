package com.ezio.unishare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import android.util.Log
import okhttp3.ResponseBody


@Composable
fun RentalScreen(userEmail: String, modifier: Modifier = Modifier) {
    var itemsIRented by remember { mutableStateOf<List<RentalRequest>>(emptyList()) }
    var itemsIRentedOut by remember { mutableStateOf<List<RentalItem>>(emptyList()) }

    var isLoadingRented by remember { mutableStateOf(true) }
    var isLoadingRentedOut by remember { mutableStateOf(true) }

    var historyItems by remember { mutableStateOf<List<RentalRequest>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(true) }

    var refreshTrigger by remember { mutableStateOf(0) }

    // Fetch items I'm renting
    LaunchedEffect(userEmail) {
        RetrofitClient.instance.getMyRentals(userEmail).enqueue(object : Callback<List<RentalRequest>> {
            override fun onResponse(call: Call<List<RentalRequest>>, response: Response<List<RentalRequest>>) {
                if (response.isSuccessful) {
                    itemsIRented = response.body() ?: emptyList()
                }
                isLoadingRented = false
            }
            override fun onFailure(call: Call<List<RentalRequest>>, t: Throwable) {
                isLoadingRented = false
            }
        })
    }

    // Fetch items I listed
    LaunchedEffect(userEmail) {
        RetrofitClient.instance.getMyListedItems(userEmail).enqueue(object : Callback<List<RentalItem>> {
            override fun onResponse(call: Call<List<RentalItem>>, response: Response<List<RentalItem>>) {
                if (response.isSuccessful) {
                    itemsIRentedOut = response.body() ?: emptyList()
                }
                isLoadingRentedOut = false
            }
            override fun onFailure(call: Call<List<RentalItem>>, t: Throwable) {
                isLoadingRentedOut = false
            }
        })
    }

    //History
    LaunchedEffect(userEmail) {
        RetrofitClient.instance.getRentalHistory(userEmail).enqueue(object : Callback<List<RentalRequest>> {
            override fun onResponse(call: Call<List<RentalRequest>>, response: Response<List<RentalRequest>>) {
                if (response.isSuccessful) {
                    historyItems = response.body() ?: emptyList()
                }
                isLoadingHistory = false
            }
            override fun onFailure(call: Call<List<RentalRequest>>, t: Throwable) {
                isLoadingHistory = false
            }
        })
    }

    // Updated LaunchedEffect to use your existing variables
    LaunchedEffect(userEmail, refreshTrigger) {
        RetrofitClient.instance.getMyListedItems(userEmail).enqueue(object : Callback<List<RentalItem>> {
            override fun onResponse(call: Call<List<RentalItem>>, response: Response<List<RentalItem>>) {
                if (response.isSuccessful) {
                    // Use 'itemsIRentedOut' which is already defined at the top of your screen
                    itemsIRentedOut = response.body() ?: emptyList()
                }
                isLoadingRentedOut = false
            }
            override fun onFailure(call: Call<List<RentalItem>>, t: Throwable) {
                isLoadingRentedOut = false
            }
        })

        // Also re-fetch history so it stays in sync
        RetrofitClient.instance.getRentalHistory(userEmail).enqueue(object : Callback<List<RentalRequest>> {
            override fun onResponse(call: Call<List<RentalRequest>>, response: Response<List<RentalRequest>>) {
                if (response.isSuccessful) {
                    historyItems = response.body() ?: emptyList()
                }
                isLoadingHistory = false
            }
            override fun onFailure(call: Call<List<RentalRequest>>, t: Throwable) {
                isLoadingHistory = false
            }
        })
    }

    val tabs = listOf("Renting", "My Listings", "History")
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "My Rentals",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (isLoadingRented) {
                    LoadingState()
                } else {
                    RentingTabContent(
                        items = itemsIRented,
                        emptyMessage = "You haven't requested anything yet",
                        emptySubMessage = "Browse items and start renting!"
                    )
                }
            }

            1 -> {
                if (isLoadingRentedOut) {
                    LoadingState()
                } else {
                    MyListingsTabContent(
                        rentalItems = itemsIRentedOut,
                        emptyMessage = "You haven't listed any items yet",
                        emptySubMessage = "Tap + on the home screen to list an item!",
                        onDeleteItem = { itemId ->
                            // Your existing Delete logic
                            val body = mapOf("item_id" to itemId.toString(), "email" to userEmail)
                            RetrofitClient.instance.deleteItem(body).enqueue(object : Callback<Map<String, String>> {
                                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                                    if (response.isSuccessful) {
                                        itemsIRentedOut = itemsIRentedOut.filter { it.item_id != itemId }
                                    }
                                }
                                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                                    Log.e("DELETE", "Error: ${t.message}")
                                }
                            })
                        },
                        onReturnItem = { itemId ->
                            val body = mapOf("item_id" to itemId.toString())

                            RetrofitClient.instance.returnItem(body).enqueue(object : Callback<Map<String, String>> {
                                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                                    if (response.isSuccessful) {
                                        // 1. Increment the master switch to force a re-fetch of all lists
                                        refreshTrigger++

                                        // 2. Log for debugging
                                        Log.d("RETURN", "Database updated successfully. Refreshing lists...")
                                    } else {
                                        Log.e("RETURN", "Server error: ${response.code()}")
                                    }
                                }

                                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                                    Log.e("RETURN", "Network Error: ${t.message}")
                                }
                            })
                        } ,
                        onReturnSuccess = { refreshTrigger++ }
                    )
                }
            }

            2 -> { // NEW HISTORY TAB
                if (isLoadingHistory) {
                    LoadingState()
                } else {
                    RentingTabContent(
                        items = historyItems,
                        emptyMessage = "No history yet",
                        emptySubMessage = "Past rentals will show up here."
                    )
                }
            }
        }
    }
}
@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun RentingTabContent(items: List<RentalRequest>, emptyMessage: String, emptySubMessage: String) {
    if (items.isEmpty()) {
        EmptyStateMessage(emptyMessage, emptySubMessage)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                MyRentalCard(item = item)
            }
        }
    }
}

@Composable
fun MyRentalCard(item: RentalRequest) {
    val statusColor = when (item.status.lowercase()) {
        "pending" -> Color(0xFFFFA000)
        "accepted" -> Color(0xFF388E3C)
        "rejected" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.image_url,
                contentDescription = item.name,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "₹${item.price_per_day}/day", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                if (item.status.lowercase() == "accepted" && !item.accepted_at.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RentalTimer(acceptedAt = item.accepted_at, totalDays = item.rental_days)
                }
            }

            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, statusColor)
            ) {
                Text(
                    text = item.status.replaceFirstChar { it.uppercase() },
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// --- TAB 2: ITEMS I HAVE LISTED (Uses RentalItem) ---
@Composable
fun MyListingsTabContent(
    rentalItems: List<RentalItem>,
    emptyMessage: String,
    emptySubMessage: String,
    onDeleteItem: (Int) -> Unit,
    onReturnItem: (Int) -> Unit ,// Added this parameter
    onReturnSuccess: () -> Unit
) {
    if (rentalItems.isEmpty()) {
        EmptyStateMessage(emptyMessage, emptySubMessage)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rentalItems) { rentalItem ->
                MyListingCard(
                    item = rentalItem,
                    onDeleteClick = { id -> onDeleteItem(id) },
                    onReturnClick = { id -> onReturnItem(id) } ,// Pass it to the card
                    onReturnSuccess = onReturnSuccess
                )
            }
        }
    }
}

@Composable
fun MyListingCard(
    item: RentalItem,
    onReturnSuccess: () -> Unit,
    onDeleteClick: (Int) -> Unit = {},
    onReturnClick: (Int) -> Unit = {} // Added this parameter

) {
    var isReturning by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "₹${item.price}/day", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                if (!item.is_available) {
                    // This section shows when the item is Rented Out
                    Text(text = "Status: Rented Out", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

                    Button(
                        onClick = {
                            isReturning = true // Start loading immediately
                            val requestData = mapOf(
                                "rental_id" to item.rental_id,
                                "item_id" to item.item_id
                            )

                            RetrofitClient.instance.markReturned(requestData).enqueue(object : Callback<ResponseBody> {
                                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                    if (response.isSuccessful) {
                                        isReturning = false
                                        onReturnSuccess()
                                        // Item will be removed by your screen refresh logic
                                    } else {
                                        isReturning = false // Stop spinner if server fails
                                    }
                                }

                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    isReturning = false // Stop spinner if network fails
                                }
                            })
                        },
                        enabled = !isReturning, // Disable button to prevent double-clicks
                        modifier = Modifier.padding(top = 4.dp).height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        )
                    ) {
                        if (isReturning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Mark Returned", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (item.accepted_at != null && item.rental_days != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        RentalTimer(acceptedAt = item.accepted_at!!, totalDays = item.rental_days!!)
                    }
                } else {
                    Text(text = "Status: Available", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (item.is_available) {
                    IconButton(
                        onClick = { onDeleteClick(item.item_id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                AssistChip(onClick = {}, label = { Text(item.category, style = MaterialTheme.typography.labelSmall) })
            }
        }
    }
}


@Composable
fun EmptyStateMessage(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun RentalTimer(acceptedAt: String, totalDays: Int) {
    var timeLeft by remember { mutableStateOf("Calculating...") }

    LaunchedEffect(acceptedAt) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        try {
            val startTime = sdf.parse(acceptedAt)?.time ?: 0L
            val expiryTime = startTime + (totalDays * 24 * 60 * 60 * 1000L)

            while (true) {
                val now = System.currentTimeMillis()
                val diff = expiryTime - now

                if (diff > 0) {
                    val hours = diff / (1000 * 60 * 60)
                    val minutes = (diff / (1000 * 60)) % 60
                    val seconds = (diff / 1000) % 60
                    timeLeft = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    timeLeft = "Expired"
                    break
                }
                kotlinx.coroutines.delay(1000)
            }
        } catch (e: Exception) {
            timeLeft = "Error"
        }
    }

    Text(
        text = "Time Left: $timeLeft",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = if (timeLeft == "Expired") Color.Red else MaterialTheme.colorScheme.primary
    )
}