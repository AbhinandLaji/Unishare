package com.ezio.unishare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgetActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
        private lateinit var emailInputLayout: TextInputLayout
            private lateinit var sendOtpButton: Button
                private lateinit var otpEditText: EditText
                    private lateinit var otpInputLayout: TextInputLayout
                        private lateinit var verifyOtpButton: Button

                            override fun onCreate(savedInstanceState: Bundle?) {
                                super.onCreate(savedInstanceState)
                                setContentView(R.layout.activity_forget)

                                emailEditText = findViewById(R.id.editTextEmail)
                                emailInputLayout = findViewById(R.id.textInputLayoutEmail)
                                sendOtpButton = findViewById(R.id.buttonSendOtp)
                                otpEditText = findViewById(R.id.editTextOtp)
                                otpInputLayout = findViewById(R.id.textInputLayoutOtp)
                                verifyOtpButton = findViewById(R.id.buttonVerifyOtp)

                                // Step 1: Request OTP from Python Backend
                                sendOtpButton.setOnClickListener {
                                    val email = emailEditText.text.toString().trim()
                                    if (email.isEmpty() || !email.endsWith("@tkmce.ac.in")) {
                                        emailInputLayout.error = "Valid TKMCE email required"
                                        return@setOnClickListener
                                    }

                                    sendOtpButton.isEnabled = false
                                    val data = mapOf("email" to email)

                                    RetrofitClient.instance.forgetPassword(data).enqueue(object : Callback<ApiResponse> {
                                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                                            if (response.isSuccessful) {
                                                Toast.makeText(this@ForgetActivity, "OTP Sent to Email", Toast.LENGTH_SHORT).show()
                                                emailInputLayout.visibility = View.GONE
                                                sendOtpButton.visibility = View.GONE
                                                otpInputLayout.visibility = View.VISIBLE
                                                verifyOtpButton.visibility = View.VISIBLE
                                            } else {
                                                sendOtpButton.isEnabled = true
                                                Toast.makeText(this@ForgetActivity, "Email not registered", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                            sendOtpButton.isEnabled = true
                                            Toast.makeText(this@ForgetActivity, "Server Offline", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                }

                                // Step 2: Pass OTP to the final screen
                                verifyOtpButton.setOnClickListener {
                                    val otp = otpEditText.text.toString().trim()
                                    val email = emailEditText.text.toString().trim()

                                    if (otp.length == 6) {
                                        val intent = Intent(this, SetNewPasswordActivity::class.java)
                                        intent.putExtra("EXTRA_EMAIL", email)
                                        intent.putExtra("EXTRA_OTP", otp)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            }
}
