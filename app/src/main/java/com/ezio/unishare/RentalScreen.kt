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

@Composable
fun RentalScreen(userEmail: String, modifier: Modifier = Modifier) {
    // 1. Using RentalRequest for items the user is renting
    var itemsIRented by remember { mutableStateOf<List<RentalRequest>>(emptyList()) }
    // 2. Using RentalItem for items the user listed
    var itemsIRentedOut by remember { mutableStateOf<List<RentalItem>>(emptyList()) }

    var isLoadingRented by remember { mutableStateOf(true) }
    var isLoadingRentedOut by remember { mutableStateOf(true) }

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

    // Fetch items I listed/rented out
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

    val tabs = listOf("Renting", "My Listings")
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
                        items = itemsIRentedOut,
                        emptyMessage = "You haven't listed any items yet",
                        emptySubMessage = "Tap + on the home screen to list an item!"
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

// --- TAB 1: ITEMS I AM RENTING (Uses RentalRequest) ---
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
    // Dynamic colors based on request status
    val statusColor = when (item.status.lowercase()) {
        "pending" -> Color(0xFFFFA000) // Amber/Yellow
        "accepted" -> Color(0xFF388E3C) // Green
        "rejected" -> Color(0xFFD32F2F) // Red
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                // Uses price_per_day matching the RentalRequest model
                Text(text = "₹${item.price_per_day}/day", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // Status Badge
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
fun MyListingsTabContent(items: List<RentalItem>, emptyMessage: String, emptySubMessage: String) {
    if (items.isEmpty()) {
        EmptyStateMessage(emptyMessage, emptySubMessage)
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                MyListingCard(item = item)
            }
        }
    }
}

@Composable
fun MyListingCard(item: RentalItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.imageUrl, // Uses imageUrl matching RentalItem model
                contentDescription = item.name,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                // Uses price matching RentalItem model
                Text(text = "₹${item.price}/day", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))

                // Show availability status
                val availText = if (item.is_available) "Available" else "Rented Out"
                val availColor = if (item.is_available) Color.Gray else MaterialTheme.colorScheme.primary
                Text(text = "Status: $availText", style = MaterialTheme.typography.bodySmall, color = availColor)
            }

            AssistChip(onClick = {}, label = { Text(item.category, style = MaterialTheme.typography.labelSmall) })
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

@Preview(showBackground = true)
@Composable
fun RentalScreenPreview() {
    MaterialTheme { RentalScreen(userEmail = "test@tkmce.ac.in") }
}