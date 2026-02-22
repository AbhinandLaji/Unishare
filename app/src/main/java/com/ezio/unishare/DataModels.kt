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
    val price: String,
    val description: String,
    val category: String,
    val owner_email: String,
    val imageUrl: String,
    val rating: Float = 4.5f,
    val is_available: Boolean = true
)