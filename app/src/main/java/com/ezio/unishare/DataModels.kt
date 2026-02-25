package com.ezio.unishare

import com.google.gson.annotations.SerializedName

// Matches your Python 'jsonify' structure


// Authentication Request Models
data class RegisterRequest(val email: String)
data class VerifyRequest(val email: String, val otp: String)
data class LoginRequest(val email: String, val password: String)

// Marketplace Item Model
data class RentalItem(
    @SerializedName("item_id") val item_id: Int,
    val name: String,
    val rental_id: Int,
    val price: String,
    val description: String,
    val category: String,
    val owner_email: String,
    val imageUrl: String,
    val rating: Float = 4.5f,
    val is_available: Boolean = true,
    val accepted_at: String? = null,
    val rental_days: Int? = null
)

data class RentalRequest(
    val rental_id: Int,
    val renter_email: String,
    val rental_days: Int,        // Changed from String to Int for math
    val status: String,
    val name: String,
    val item_id: Int,
    val price_per_day: String,
    val image_url: String,
    val accepted_at: String?     // Add this missing field
)

data class ApiResponse(
    val message: String? = null,
    val error: String? = null,
    val user_name: String? = null,
    val count: Int? = null    // ADD THIS
)

data class MyRental(
    val rental_id: Int,
    val item_id: Int,
    val status: String
)