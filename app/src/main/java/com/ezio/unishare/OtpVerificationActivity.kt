package com.ezio.unishare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpVerificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        val otpEdit = findViewById<EditText>(R.id.editTextOtp)
        val verifyBtn = findViewById<Button>(R.id.buttonVerifyOtp)

        verifyBtn.setOnClickListener {
            val otp = otpEdit.text.toString().trim()

            val data = mapOf("email" to email, "otp" to otp)
            RetrofitClient.instance.verifyOtp(data).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        // Success! Python has saved the user to MariaDB
                        Toast.makeText(this@OtpVerificationActivity, "Registration Complete!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@OtpVerificationActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@OtpVerificationActivity, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(this@OtpVerificationActivity, "Network Error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
