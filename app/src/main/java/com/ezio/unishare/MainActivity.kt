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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Check if already logged in (Firebase Auth session exists)
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            Log.d("Login", "User already logged in: ${currentUser.email}")
            // User is already signed in, go to Home
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("USER_EMAIL", currentUser.email ?: "")
            }
            startActivity(intent)
            finish()
            return
        }

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
        val buttonScaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale_anim)

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
            it.startAnimation(buttonScaleAnimation)
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

            // Disable button during login process
            joinButton.isEnabled = false

            // --- Sign in with Firebase Authentication ---
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    joinButton.isEnabled = true

                    if (task.isSuccessful) {
                        // Sign in success
                        val user = firebaseAuth.currentUser
                        Log.d("Login", "signInWithEmail:success - ${user?.email}")
                        Toast.makeText(this, "Login successful ✅", Toast.LENGTH_SHORT).show()

                        // Save session in SharedPreferences (optional, for backup)
                        val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putBoolean("isLoggedIn", true)
                            putString("userEmail", email)
                            apply()
                        }

                        // Navigate to Home
                        val intent = Intent(this, HomeActivity::class.java).apply {
                            putExtra("USER_EMAIL", email)
                        }
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    } else {
                        // Sign in failed
                        Log.e("Login", "signInWithEmail:failure", task.exception)

                        val errorMessage = when {
                            task.exception?.message?.contains("no user record", ignoreCase = true) == true -> {
                                collegeMailLayout.error = "Account not found"
                                collegeMailLayout.startAnimation(shakeAnimation)
                                "No account found with this email"
                            }
                            task.exception?.message?.contains("password is invalid", ignoreCase = true) == true -> {
                                passwordLayout.error = "Incorrect password"
                                passwordLayout.startAnimation(shakeAnimation)
                                "Incorrect password"
                            }
                            else -> {
                                "Login failed: ${task.exception?.message}"
                            }
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        // --- Navigation to other activities ---
        createAccountButton.setOnClickListener {
            it.startAnimation(buttonScaleAnimation)
            startActivity(Intent(this, CreateAccountActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        forgotPasswordTextView.setOnClickListener {
            it.startAnimation(buttonScaleAnimation)
            startActivity(Intent(this, ForgetActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}
