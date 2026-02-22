package com.ezio.unishare

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun RentalRequestsScreen(userEmail: String) {
    var requests by remember { mutableStateOf<List<RentalRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    fun loadRequests() {
        isLoading = true
        RetrofitClient.instance.getPendingRequests(userEmail)
            .enqueue(object : Callback<List<RentalRequest>> {
                override fun onResponse(
                    call: Call<List<RentalRequest>>,
                    response: Response<List<RentalRequest>>
                ) {
                    if (response.isSuccessful) {
                        requests = response.body() ?: emptyList()
                    }
                    isLoading = false
                }
                override fun onFailure(call: Call<List<RentalRequest>>, t: Throwable) {
                    isLoading = false
                    Toast.makeText(context, "Failed to load requests", Toast.LENGTH_SHORT).show()
                }
            })
    }

    LaunchedEffect(userEmail) { loadRequests() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Rental Requests",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No pending requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "You'll see requests from renters here",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = requests,
                    key = { it.rental_id }
                ) { request ->
                    RentalRequestCard(
                        request = request,
                        onAccept = {
                            val body = mapOf(
                                "rental_id" to request.rental_id.toString(),
                                "status" to "accepted"
                            )
                            RetrofitClient.instance.toggleRequest(body)
                                .enqueue(object : Callback<ApiResponse> {
                                    override fun onResponse(call: Call<ApiResponse>, r: Response<ApiResponse>) {
                                        if (r.isSuccessful) {
                                            Toast.makeText(context, "Accepted! Item removed from listings.", Toast.LENGTH_SHORT).show()
                                            // Remove card instantly - item is now unavailable on home screen too
                                            requests = requests.filter { it.rental_id != request.rental_id }
                                        } else {
                                            Toast.makeText(context, "Failed to accept", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        },
                        onReject = {
                            val body = mapOf(
                                "rental_id" to request.rental_id.toString(),
                                "status" to "rejected"
                            )
                            RetrofitClient.instance.toggleRequest(body)
                                .enqueue(object : Callback<ApiResponse> {
                                    override fun onResponse(call: Call<ApiResponse>, r: Response<ApiResponse>) {
                                        if (r.isSuccessful) {
                                            Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show()
                                            // Remove card instantly
                                            requests = requests.filter { it.rental_id != request.rental_id }
                                        } else {
                                            Toast.makeText(context, "Failed to reject", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RentalRequestCard(
    request: RentalRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = request.image_url,
                    contentDescription = request.name,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("₹${request.price_per_day}/day", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Requested by", fontSize = 11.sp, color = Color.Gray)
                    Text(request.renter_email, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Duration", fontSize = 11.sp, color = Color.Gray)
                    Text("${request.rental_days} days", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept")
                }
            }
        }
    }
}