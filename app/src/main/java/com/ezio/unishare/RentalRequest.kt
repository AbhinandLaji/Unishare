package com.ezio.unishare

/**
 * This data class holds all the information about a single rental request.
 * It's the structure for the data we save in the "rent_requests" node in Firebase.
 */
data class RentalRequest(
    val requestId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val ownerEmail: String = "",
    val renterEmail: String = "",
    val renterName: String = "",    // To show the owner who is requesting
    val duration: String = "",      // e.g., "3 days"
    val status: String = "pending", // Can be "pending", "accepted", "rejected"
    val timestamp: Long = 0
)