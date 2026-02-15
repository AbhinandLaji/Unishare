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

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val emailEdit = findViewById<EditText>(R.id.editTextCollegeEmail)
        val nameEdit = findViewById<EditText>(R.id.editTextFirstName)
        val passwordEdit = findViewById<EditText>(R.id.editTextPassword)
        val submitBtn = findViewById<Button>(R.id.buttonCreateAccountSubmit)

        submitBtn.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val name = nameEdit.text.toString().trim()
            val password = passwordEdit.text.toString()

            if (!email.endsWith("tkmce.ac.in")) {
                Toast.makeText(this, "TKMCE Email Required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitBtn.isEnabled = false
            val data = mapOf("email" to email, "name" to name, "password" to password)

            RetrofitClient.instance.register(data).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val intent = Intent(this@CreateAccountActivity, OtpVerificationActivity::class.java)
                        intent.putExtra("EXTRA_EMAIL", email)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@CreateAccountActivity, "Email blocked", Toast.LENGTH_SHORT).show()
                        submitBtn.isEnabled = true
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    submitBtn.isEnabled = true
                    Toast.makeText(this@CreateAccountActivity, "Connection Failed", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
