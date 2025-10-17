package com.ezio.unishare

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Check if user already logged in
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            Log.d("Login", "User already logged in: ${currentUser.email}")
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

        // --- Animations ---
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake_anim)
        val buttonScaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale_anim)

        // --- Helper: clear error when typing ---
        fun addTextWatcherToClearError(editText: EditText, layout: TextInputLayout) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!s.isNullOrEmpty() && layout.error != null) {
                        layout.error = null
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        addTextWatcherToClearError(collegeMailEditText, collegeMailLayout)
        addTextWatcherToClearError(passwordEditText, passwordLayout)

        // --- LOGIN BUTTON ---
        // Create a dedicated animation instance for the login button
        val loginButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale_anim)

        // Set up the listener to run the login logic AFTER the animation finishes
        loginButtonAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                // Animation started
            }

            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Animation finished, now run the login logic
                val email = collegeMailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                var isValid = true

                // --- Input Validation ---
                when {
                    email.isBlank() -> {
                        collegeMailLayout.error = "Email is required"
                        collegeMailLayout.startAnimation(shakeAnimation)
                        isValid = false
                    }
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        collegeMailLayout.error = "Enter a valid email address"
                        collegeMailLayout.startAnimation(shakeAnimation)
                        isValid = false
                    }
                    else -> collegeMailLayout.error = null
                }

                if (password.isBlank()) {
                    passwordLayout.error = "Password is required"
                    passwordLayout.startAnimation(shakeAnimation)
                    isValid = false
                } else {
                    passwordLayout.error = null
                }

                // If validation fails, re-enable the button and stop.
                if (!isValid) {
                    joinButton.isEnabled = true
                    return
                }

                // --- Sign in with Firebase Authentication (Validation passed) ---
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@MainActivity) { task ->
                        // Always re-enable the button once Firebase returns a result
                        joinButton.isEnabled = true

                        if (task.isSuccessful) {
                            // ✅ Login success
                            val user = firebaseAuth.currentUser
                            Log.d("Login", "signInWithEmail:success - ${user?.email}")
                            Toast.makeText(this@MainActivity, "Login successful ✅", Toast.LENGTH_SHORT).show()

                            // Save session locally
                            getSharedPreferences("UserSession", MODE_PRIVATE).edit().apply {
                                putBoolean("isLoggedIn", true)
                                putString("userEmail", email)
                                apply()
                            }

                            // Navigate to HomeActivity
                            startActivity(Intent(this@MainActivity, HomeActivity::class.java).apply {
                                putExtra("USER_EMAIL", email)
                            })
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            finish()

                        } else {
                            // ❌ Login failed
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
                                else -> "Login failed. Please try again later."
                            }

                            Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
            }
        })

        joinButton.setOnClickListener { buttonView ->
            // Disable button immediately to prevent double-clicks
            buttonView.isEnabled = false
            // Start the animation. The listener will handle the rest.
            buttonView.startAnimation(loginButtonAnimation)
        }

        // --- CREATE ACCOUNT BUTTON ---
        createAccountButton.setOnClickListener {
            it.startAnimation(buttonScaleAnimation)
            startActivity(Intent(this, CreateAccountActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // --- FORGOT PASSWORD ---
        forgotPasswordTextView.setOnClickListener {
            startActivity(Intent(this, ForgetActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}
