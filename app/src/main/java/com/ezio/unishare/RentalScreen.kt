package com.ezio.unishare

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

@Composable
fun RentalScreen(rentalItems: List<RentalItem>, userEmail: String) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Renting Out", "My Bookings")

    Column(modifier = Modifier.fillMaxSize()) {
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

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "tab_animation"
        ) { targetTab ->
            when (targetTab) {
                0 -> RentingOutTab(rentalItems = rentalItems, userEmail = userEmail)
                1 -> MyBookingsTab(userEmail = userEmail)
            }
        }
    }
}

@Composable
fun RentingOutTab(rentalItems: List<RentalItem>, userEmail: String) {
    val myItems by remember {
        derivedStateOf {
            rentalItems.filter { it.ownerEmail == userEmail }
        }
    }
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()

    val activeRentals = remember { mutableStateMapOf<String, ActiveRental>() }
    var isLoadingRentals by remember { mutableStateOf(true) }

    DisposableEffect(myItems) {
        val listeners = mutableListOf<Pair<String, ValueEventListener>>()
        var loadedCount = 0
        val totalItems = myItems.size

        if (totalItems == 0) {
            isLoadingRentals = false
        }

        myItems.forEach { item ->
            val rentalRef = database.getReference("active_rentals")
                .orderByChild("itemId").equalTo(item.id)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val rental = child.getValue(ActiveRental::class.java)
                        if (rental != null) {
                            activeRentals[item.id] = rental
                        }
                    }
                    if (!snapshot.exists()) {
                        activeRentals.remove(item.id)
                    }

                    loadedCount++
                    if (loadedCount >= totalItems) {
                        isLoadingRentals = false
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    if (loadedCount >= totalItems) {
                        isLoadingRentals = false
                    }
                }
            }

            rentalRef.addValueEventListener(listener)
            listeners.add(Pair(item.id, listener))
        }

        onDispose {
            listeners.forEach { (itemId, listener) ->
                database.getReference("active_rentals")
                    .orderByChild("itemId").equalTo(itemId)
                    .removeEventListener(listener)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Items You're Renting Out",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        when {
            myItems.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("You haven't listed any items for rent yet.")
                    }
                }
            }
            isLoadingRentals -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(myItems) { item ->
                        OwnerRentalItemCard(
                            item = item,
                            activeRental = activeRentals[item.id],
                            onDelete = {
                                database.getReference("rentals").child(item.id).removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun MyBookingsTab(userEmail: String) {
    val database = FirebaseDatabase.getInstance()
    val myBookings = remember { mutableStateListOf<Pair<ActiveRental, RentalItem?>>() }
    var isLoading by remember { mutableStateOf(true) }
    var minLoadTimePassed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        minLoadTimePassed = true
    }

    DisposableEffect(userEmail) {
        val bookingsRef = database.getReference("active_rentals")
            .orderByChild("renterEmail").equalTo(userEmail)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookingsList = mutableListOf<ActiveRental>()
                for (child in snapshot.children) {
                    val rental = child.getValue(ActiveRental::class.java)
                    // FILTER OUT RETURNED ITEMS HERE
                    if (rental != null && rental.returned != true) {
                        bookingsList.add(rental)
                    }
                }

                myBookings.clear()
                bookingsList.forEach { rental ->
                    database.getReference("rentals").child(rental.itemId).get()
                        .addOnSuccessListener { itemSnapshot ->
                            val item = itemSnapshot.getValue(RentalItem::class.java)
                            myBookings.add(Pair(rental, item))
                        }
                }
                isLoading = false
            }
            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        }

        bookingsRef.addValueEventListener(listener)

        onDispose {
            bookingsRef.removeEventListener(listener)
        }
    }

    // Rest of the function stays the same...
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Items You're Renting",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        when {
            isLoading || !minLoadTimePassed -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            myBookings.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("You haven't rented any items yet.")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(myBookings) { (rental, item) ->
                        if (item != null) {
                            RenterBookingCard(rental = rental, item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerRentalItemCard(
    item: RentalItem,
    activeRental: ActiveRental?,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showReturnDialog by remember { mutableStateOf(false) }

    LaunchedEffect(activeRental) {
        if (activeRental != null && activeRental.returned != true) {
            while (true) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    // Confirmation dialog
    if (showReturnDialog && activeRental != null) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Confirm Item Return") },
            text = {
                Column {
                    Text("Are you sure ${activeRental.renterEmail.substringBefore('@')} has returned the item?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will mark the rental as completed and keep it in your history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Update the rental to mark as returned
                        val updates = mapOf(
                            "returned" to true,
                            "returnedAt" to System.currentTimeMillis()
                        )

                        database.getReference("active_rentals")
                            .child(activeRental.requestId)
                            .updateChildren(updates)
                            .addOnSuccessListener {
                                // Also update rent_requests status
                                database.getReference("rent_requests")
                                    .child(activeRental.requestId)
                                    .child("status")
                                    .setValue("returned")
                                    .addOnSuccessListener {
                                        // Optional: Copy to rental_history for permanent record
                                        database.getReference("rental_history")
                                            .child(activeRental.requestId)
                                            .setValue(activeRental.copy(returned = true))

                                        Toast.makeText(
                                            context,
                                            "✓ Item marked as returned!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showReturnDialog = false
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            context,
                                            "Failed to update status: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Failed to mark as returned: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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

    val isReturned = activeRental?.returned == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isReturned -> Color(0xFFE8F5E9) // Light green for returned
                activeRental != null -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Base64Image(
                    base64String = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        item.price,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (activeRental != null) {
                        Spacer(modifier = Modifier.height(4.dp))

                        if (isReturned) {
                            // Show returned status
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Returned",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Was rented to: ${activeRental.renterEmail.substringBefore('@')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        } else {
                            // Currently rented
                            Text(
                                "Rented to: ${activeRental.renterEmail.substringBefore('@')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Delete button only for available items (not currently rented and not in returned history)
                if (activeRental == null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (activeRental != null && !isReturned) {
                // Show timer and button only for active (not returned) rentals
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                val timeRemaining = activeRental.rentalEndTime - currentTime

                if (timeRemaining > 0) {
                    CountdownTimer(timeRemaining = timeRemaining)
                } else {
                    Text(
                        "Rental period ended",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                // MARK AS RETURNED BUTTON
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showReturnDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timeRemaining <= 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (timeRemaining <= 0) "Mark Returned (Overdue)" else "Mark as Returned",
                        fontWeight = FontWeight.Bold
                    )
                }
                // CONTACT RENTER BUTTON - ADD THIS
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(context, ConversationActivity::class.java).apply {
                            putExtra("CURRENT_USER_EMAIL", item.ownerEmail)
                            putExtra("OTHER_USER_EMAIL", activeRental.renterEmail)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Contact Renter",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }


            } else if (isReturned) {
                // Show rental completion info for returned items
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFC8E6C9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Rental completed successfully",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenterBookingCard(rental: ActiveRental, item: RentalItem) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Base64Image(
                    base64String = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.price,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Owner: ${item.ownerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            val timeRemaining = rental.rentalEndTime - currentTime

            if (timeRemaining > 0) {
                CountdownTimer(timeRemaining = timeRemaining)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Rental period ended",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Please return the item",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownTimer(timeRemaining: Long) {
    val days = timeRemaining / (24 * 60 * 60 * 1000)
    val hours = (timeRemaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
    val minutes = (timeRemaining % (60 * 60 * 1000)) / (60 * 1000)
    val seconds = (timeRemaining % (60 * 1000)) / 1000

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Time Remaining:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (days > 0) {
                TimeUnit(value = days, unit = "Days")
            }
            TimeUnit(value = hours, unit = "Hours")
            TimeUnit(value = minutes, unit = "Min")
            TimeUnit(value = seconds, unit = "Sec")
        }
    }
}

@Composable
fun TimeUnit(value: Long, unit: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}