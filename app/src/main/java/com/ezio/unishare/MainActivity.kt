package com.ezio.unishare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Session Check: If already logged in, skip to Home
        // Updated Session Check
        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
        if (sharedPref.getBoolean("isLoggedIn", false)) {
            val storedEmail = sharedPref.getString("userEmail", "User")
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USER_EMAIL", storedEmail)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // 2. Initialize UI Components
        val emailEdit = findViewById<EditText>(R.id.editTextName)
        val passwordEdit = findViewById<EditText>(R.id.editTextEmail)
        val joinButton = findViewById<Button>(R.id.buttonJoin) // The Login Button
        val createAccountButton = findViewById<Button>(R.id.buttonCreateAccount)
        val forgotPasswordText = findViewById<TextView>(R.id.textViewForgotPassword)

        // 3. Login Logic (Retrofit Call to Python Backend)
        joinButton.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            joinButton.isEnabled = false // Prevent multiple clicks

            val loginRequest = LoginRequest(email, password)
            RetrofitClient.instance.login(loginRequest).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    joinButton.isEnabled = true
                    if (response.isSuccessful) {
                        // Success: Python sent 200 OK
                        val userName = response.body()?.user_name
                        Toast.makeText(this@MainActivity, "Welcome $userName!", Toast.LENGTH_SHORT).show()

                        sharedPref.edit().apply {
                            putBoolean("isLoggedIn", true)
                            putString("userName", userName)
                            putString("userEmail", email) // Store email for future sessions
                            apply()
                        }

                        val intent = Intent(this@MainActivity, HomeActivity::class.java)
                        intent.putExtra("USER_EMAIL", email) // This sends the email to HomeActivity
                        startActivity(intent)
                        finish()
                    } else {
                        // Error: Python sent 401 Unauthorized
                        Toast.makeText(this@MainActivity, "Invalid Credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    joinButton.isEnabled = true
                    // Troubleshooting: Check if laptop IP in RetrofitClient matches hostname -I
                    Toast.makeText(this@MainActivity, "Server Offline: Check Laptop Connection", Toast.LENGTH_LONG).show()
                }
            })
        }

        // 4. Create Account Button: FIX for the non-responsive button issue
        createAccountButton.setOnClickListener {
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
        }

        // 5. Forgot Password Logic
        forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgetActivity::class.java)
            startActivity(intent)
        }
    }
}