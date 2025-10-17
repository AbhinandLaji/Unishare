package com.ezio.unishare

data class ActiveRental(
    val requestId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val ownerEmail: String = "",
    val renterEmail: String = "",
    val rentalStartTime: Long = 0L,
    val rentalEndTime: Long = 0L,
    val returned: Boolean = false
)