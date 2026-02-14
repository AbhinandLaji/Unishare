package com.ezio.unishare

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Find Views ---
        val collegeMailEditText = findViewById<EditText>(R.id.editTextName)
        val collegeMailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutName)
        val passwordEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)
        val joinButton = findViewById<Button>(R.id.buttonJoin)
        val createAccountButton = findViewById<Button>(R.id.buttonCreateAccount)
        val forgotPasswordTextView = findViewById<TextView>(R.id.textViewForgotPassword)
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake_anim)

        // --- Helper to clear errors ---
        fun addTextWatcherToClearError(editText: EditText, layout: TextInputLayout) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.isNotEmpty() == true && layout.error != null) {
                        layout.error = null
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        addTextWatcherToClearError(collegeMailEditText, collegeMailLayout)
        addTextWatcherToClearError(passwordEditText, passwordLayout)

        // --- LOGIN BUTTON ---
        joinButton.setOnClickListener {
            val email = collegeMailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            var isValid = true

            // --- Input Validation ---
            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                collegeMailLayout.error = "Enter a valid email address"
                collegeMailLayout.startAnimation(shakeAnimation)
                isValid = false
            } else {
                collegeMailLayout.error = null
            }

            if (password.isBlank()) {
                passwordLayout.error = "Password is required"
                passwordLayout.startAnimation(shakeAnimation)
                isValid = false
            } else {
                passwordLayout.error = null
            }

            if (!isValid) return@setOnClickListener

            // --- Database Logic Removed ---
            // Local validation complete. You can now bridge this to your SQL API.
            Log.d("Login", "Local validation successful for $email")

            // For now, we simulate a successful login to keep the app flow working
            val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
            sharedPref.edit().apply {
                putBoolean("isLoggedIn", true)
                putString("userEmail", email)
                apply()
            }

            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("USER_EMAIL", email)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }

        // --- Navigation ---
        createAccountButton.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        forgotPasswordTextView.setOnClickListener {
            startActivity(Intent(this, ForgetActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}