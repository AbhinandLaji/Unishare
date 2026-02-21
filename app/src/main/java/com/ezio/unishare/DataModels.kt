package com.ezio.unishare

import com.google.gson.annotations.SerializedName

// Matches your Python 'jsonify' structure
data class ApiResponse(
    val message: String? = null,
    val error: String? = null,
    val user_name: String? = null
)

// Authentication Request Models
data class RegisterRequest(val email: String)
data class VerifyRequest(val email: String, val otp: String)
data class LoginRequest(val email: String, val password: String)

// Marketplace Item Model
data class RentalItem(
    @SerializedName("item_id") val item_id: Int,
    val name: String,
    @SerializedName("price_per_day") val price: String,
    val description: String,
    val category: String,
    @SerializedName("owner_email") val ownerEmail: String,
    @SerializedName("image_url") val imageUrl: String,
    val rating: Float = 4.5f,
    @SerializedName("is_available") val available: Boolean = true
)