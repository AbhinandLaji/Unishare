package com.ezio.unishare

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.Call

interface ApiService {
    @POST("register")
    fun register(@Body body: Map<String, String>): Call<ApiResponse>

    @POST("verify-otp")
    fun verifyOtp(@Body body: Map<String, String>): Call<ApiResponse>

    @POST("login")
    fun login(@Body body: LoginRequest): Call<ApiResponse>

    @POST("set_new_password") // Matches your Python backend route
    fun resetPassword(@Body body: Map<String, String>): Call<ApiResponse>

    @POST("forget_password")
    fun forgetPassword(@Body body: Map<String, String>): Call<ApiResponse>

    @POST("forget_password_otp_verify")
    fun verifyForgetOtp(@Body body: Map<String, String>): Call<ApiResponse>

    @POST("item_details")
    fun addItem(@Body body: Map<String, String>): Call<ApiResponse>

    @GET("display_available")
    fun getAvailableItems(): Call<List<RentalItem>>

    @POST("request_item")
    fun requestItem(@Body body: Map<String, String>): Call<ApiResponse>

    @POST("confirmation_toggle")
    fun toggleRequest(@Body body: Map<String, String>): Call<ApiResponse>
}

object RetrofitClient {
    // Replace XX with your Fedora laptop's actual IP address
    private const val BASE_URL = "http://10.0.2.2:5000/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}