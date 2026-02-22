package com.ezio.unishare

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

    var itemsIRented by remember { mutableStateOf<List<RentalItem>>(emptyList()) }
    var itemsIRentedOut by remember { mutableStateOf<List<RentalItem>>(emptyList()) }
    var isLoadingRented by remember { mutableStateOf(true) }
    var isLoadingRentedOut by remember { mutableStateOf(true) }

    // Fetch items I'm renting from database
    LaunchedEffect(userEmail) {
        RetrofitClient.instance.getMyRentals(userEmail).enqueue(object : Callback<List<RentalItem>> {
            override fun onResponse(call: Call<List<RentalItem>>, response: Response<List<RentalItem>>) {
                if (response.isSuccessful) {
                    itemsIRented = response.body() ?: emptyList()
                }
                isLoadingRented = false
            }
            override fun onFailure(call: Call<List<RentalItem>>, t: Throwable) {
                isLoadingRented = false
            }
        })
    }

    // Fetch items I listed/rented out from database
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

    val tabs = listOf("Renting", "Rented Out")
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
                    RentalTabContent(
                        items = itemsIRented,
                        emptyMessage = "You're not renting anything yet",
                        emptySubMessage = "Browse items and start renting!",
                        chipLabel = "Owner",
                        chipValueKey = { it.ownerEmail }
                    )
                }
            }
            1 -> {
                if (isLoadingRentedOut) {
                    LoadingState()
                } else {
                    RentalTabContent(
                        items = itemsIRentedOut,
                        emptyMessage = "You haven't listed any items yet",
                        emptySubMessage = "Tap + on the home screen to list an item!",
                        chipLabel = "Category",
                        chipValueKey = { it.category }
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
fun RentalTabContent(
    items: List<RentalItem>,
    emptyMessage: String,
    emptySubMessage: String,
    chipLabel: String,
    chipValueKey: (RentalItem) -> String
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emptyMessage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(emptySubMessage, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                RentalListCard(item = item, chipLabel = chipLabel, chipValue = chipValueKey(item))
            }
        }
    }
}

@Composable
fun RentalListCard(item: RentalItem, chipLabel: String, chipValue: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.price,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$chipLabel: $chipValue",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            AssistChip(
                onClick = {},
                label = { Text(item.category, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RentalScreenPreview() {
    MaterialTheme { RentalScreen(userEmail = "test@tkmce.ac.in") }
}