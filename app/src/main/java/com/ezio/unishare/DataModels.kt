package com.ezio.unishare

// Matches your Python 'jsonify' structure
data class ApiResponse(
    val message: String? = null,
    val error: String? = null,
    val user_name: String? = null
)

// The data your app sends to the server
data class RegisterRequest(val email: String)
data class VerifyRequest(val email: String, val otp: String)
data class LoginRequest(val email: String, val password: String)
