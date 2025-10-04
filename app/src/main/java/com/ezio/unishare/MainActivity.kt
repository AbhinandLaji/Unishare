package com.ezio.unishare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.Animation // Keep this if shakeAnimation is used, or AnimationUtils for others
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView // Import TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.ezio.unishare.HomeActivity // Import HomeActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views from the layout
        val nameEditText = findViewById<EditText>(R.id.editTextName) // This is for College Mail ID
        val emailEditText = findViewById<EditText>(R.id.editTextEmail) // This is for Password

        // Find TextInputLayouts
        val nameTextInputLayout = findViewById<TextInputLayout>(R.id.textInputLayoutName)
        val emailTextInputLayout = findViewById<TextInputLayout>(R.id.textInputLayoutEmail)

        val joinButton = findViewById<Button>(R.id.buttonJoin)
        val createAccountButton = findViewById<Button>(R.id.buttonCreateAccount)
        val forgotPasswordTextView = findViewById<TextView>(R.id.textViewForgotPassword) // Find the TextView

        // Load animations
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake_anim)

        // Helper function to add TextWatcher for clearing error
        fun addTextWatcherToClearError(editText: EditText, layout: TextInputLayout) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.isNotEmpty() == true && layout.error != null) {
                        layout.error = null // Clear error from TextInputLayout
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        addTextWatcherToClearError(nameEditText, nameTextInputLayout)
        addTextWatcherToClearError(emailEditText, emailTextInputLayout)

        // Set a click listener for the JOIN button
        joinButton.setOnClickListener {
            // Load a new animation instance for this specific click
            val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale_anim)
            it.startAnimation(scaleAnimation)

            val collegeMail = nameEditText.text.toString().trim()
            val password = emailEditText.text.toString().trim()

            var isValid = true

            if (collegeMail.isBlank()) {
                nameTextInputLayout.error = "College mail ID is required"
                nameTextInputLayout.startAnimation(shakeAnimation)
                isValid = false
            } else {
                nameTextInputLayout.error = null
            }

            if (password.isBlank()) {
                emailTextInputLayout.error = "Password is required"
                emailTextInputLayout.startAnimation(shakeAnimation)
                isValid = false
            } else {
                emailTextInputLayout.error = null
            }

            if (isValid) {
                // For now, we'll navigate directly.
                // In a real app, you'd verify credentials before navigating.
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

                // Your navigation code integrated here:
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                // If you want to prevent the user from going back to MainActivity after login,
                // you can add finish() here:
                // finish()
            }
        }

        // Set a click listener for the CREATE ACCOUNT button
        createAccountButton.setOnClickListener {
            val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale_anim)
            it.startAnimation(scaleAnimation)
            val intent = Intent(this, CreateAccountActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) // Your existing animation
        }

        // Set a click listener for the FORGOT PASSWORD TextView
        forgotPasswordTextView.setOnClickListener {
            // Optional: Add a small animation for visual feedback if desired
            // val clickFeedbackAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale_anim) // or any other subtle anim
            // it.startAnimation(clickFeedbackAnimation)

            val intent = Intent(this, ForgetActivity::class.java)
            startActivity(intent)
            // Optional: Add a transition animation for opening ForgetActivity
            // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // Example: fade
            // or your custom slide animations if you have them for this transition
            // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}
