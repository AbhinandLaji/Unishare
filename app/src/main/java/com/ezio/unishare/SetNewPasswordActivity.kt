package com.ezio.unishare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SetNewPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_new_password)

        val newPasswordEdit = findViewById<EditText>(R.id.editTextNewPassword)
        val confirmPasswordEdit = findViewById<EditText>(R.id.editTextConfirmPassword)
        val submitBtn = findViewById<Button>(R.id.buttonSetNewPassword)

        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        val otp = intent.getStringExtra("EXTRA_OTP") ?: ""

        submitBtn.setOnClickListener {
            val pass = newPasswordEdit.text.toString()
            val confirm = confirmPasswordEdit.text.toString()

            if (pass != confirm || pass.length < 6) {
                Toast.makeText(this, "Passwords must match and be 6+ chars", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitBtn.isEnabled = false
            val data = mapOf(
                "email" to email,
                "otp" to otp,
                "new_password" to pass
            )

            // Submit update to Python Backend
            RetrofitClient.instance.resetPassword(data).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@SetNewPasswordActivity, "Success! Please Login", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@SetNewPasswordActivity, MainActivity::class.java))
                        finish()
                    } else {
                        submitBtn.isEnabled = true
                        Toast.makeText(this@SetNewPasswordActivity, "Reset Failed: Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    submitBtn.isEnabled = true
                    Toast.makeText(this@SetNewPasswordActivity, "Connection Error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
