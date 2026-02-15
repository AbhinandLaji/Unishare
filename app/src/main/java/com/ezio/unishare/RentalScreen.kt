package com.ezio.unishare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun RentalScreen(modifier: Modifier = Modifier) {
    // Sample data - replace with your actual data from Firebase/ViewModel
    val itemsIRented = listOf(
        RentalItem(
            "1", "Calculus Textbook", "₹50/week",
            "Essential calculus textbook", "Books",
            "John Doe", "john@university.edu",
            "https://picsum.photos/seed/calculus/400"
        ),
        RentalItem(
            "2", "Scientific Calculator", "₹30/week",
            "TI-84 Plus calculator", "Electronics",
            "Sarah Smith", "sarah@university.edu",
            "https://picsum.photos/seed/calculator/400"
        ),
        RentalItem(
            "3", "Laptop Charger", "₹20/week",
            "Universal laptop charger", "Electronics",
            "Mike Johnson", "mike@university.edu",
            "https://picsum.photos/seed/charger/400"
        ),
        RentalItem(
            "4", "Physics Lab Kit", "₹100/week",
            "Complete physics lab equipment", "Books",
            "Emily Brown", "emily@university.edu",
            "https://picsum.photos/seed/labkit/400"
        )
    )

    val itemsIRentedOut = listOf(
        RentalItem(
            "5", "DSLR Camera", "₹200/day",
            "Professional Canon DSLR", "Electronics",
            "Alex Wilson", "alex@university.edu",
            "https://picsum.photos/seed/camera/400"
        ),
        RentalItem(
            "6", "Camping Tent", "₹150/weekend",
            "4-person camping tent", "Sports",
            "Chris Lee", "chris@university.edu",
            "https://picsum.photos/seed/tent/400"
        ),
        RentalItem(
            "7", "Mountain Bike", "₹80/day",
            "21-speed mountain bike", "Sports",
            "Jamie Taylor", "jamie@university.edu",
            "https://picsum.photos/seed/bike/400"
        ),
        RentalItem(
            "8", "Acoustic Guitar", "₹100/week",
            "Yamaha acoustic guitar", "Music",
            "Pat Anderson", "pat@university.edu",
            "https://picsum.photos/seed/guitar/400"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
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
            .height(180.dp),
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

@Preview(showBackground = true)
@Composable
fun RentalScreenPreview() {
    MaterialTheme {
        RentalScreen()
    }
}