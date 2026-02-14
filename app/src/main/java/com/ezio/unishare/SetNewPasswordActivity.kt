package com.ezio.unishare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout

class SetNewPasswordActivity : AppCompatActivity() {

    private lateinit var textInputLayoutNewPassword: TextInputLayout
    private lateinit var textInputLayoutConfirmPassword: TextInputLayout
    private lateinit var editTextNewPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonSetNewPassword: Button
    private lateinit var textViewPasswordCriteriaErrors: TextView

    private val passwordMinLength = 8
    private val hasUpperCasePattern = ".*[A-Z].*".toRegex()
    private val hasLowerCasePattern = ".*[a-z].*".toRegex()
    private val hasDigitPattern = ".*\\d.*".toRegex()
    private val hasSpecialCharPattern = ".*[!@#\$%^&*()_+\\-=\\[\\]{};':\\\"\\\\|,.<>/?].*".toRegex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_new_password)

        textInputLayoutNewPassword = findViewById(R.id.textInputLayoutNewPassword)
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPassword)
        editTextNewPassword = findViewById(R.id.editTextNewPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonSetNewPassword = findViewById(R.id.buttonSetNewPassword)
        textViewPasswordCriteriaErrors = findViewById(R.id.textViewPasswordCriteriaErrors)

        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake_anim)

        // Get verified email from ForgetActivity
        val userEmail = intent.getStringExtra("EXTRA_EMAIL")

        buttonSetNewPassword.setOnClickListener {
            val newPassword = editTextNewPassword.text.toString()
            val confirmPassword = editTextConfirmPassword.text.toString()

            var isValid = true
            val errors = mutableListOf<String>()

            if (newPassword.length < passwordMinLength) errors.add("At least 8 characters")
            if (!newPassword.contains(hasUpperCasePattern)) errors.add("At least 1 uppercase letter")
            if (!newPassword.contains(hasLowerCasePattern)) errors.add("At least 1 lowercase letter")
            if (!newPassword.contains(hasDigitPattern)) errors.add("At least 1 number")
            if (!newPassword.contains(hasSpecialCharPattern)) errors.add("At least 1 special character")

            if (errors.isNotEmpty()) {
                textViewPasswordCriteriaErrors.text = errors.joinToString("\n")
                textViewPasswordCriteriaErrors.visibility = TextView.VISIBLE
                textInputLayoutNewPassword.startAnimation(shakeAnimation)
                isValid = false
            } else {
                textViewPasswordCriteriaErrors.text = ""
                textViewPasswordCriteriaErrors.visibility = TextView.GONE
            }

            if (newPassword != confirmPassword) {
                textInputLayoutConfirmPassword.error = "Passwords do not match"
                textInputLayoutConfirmPassword.startAnimation(shakeAnimation)
                isValid = false
            } else {
                textInputLayoutConfirmPassword.error = null
            }

            if (isValid) {
                if (userEmail.isNullOrBlank()) {
                    Toast.makeText(this, "Error: Could not identify user. Please start over.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                it.isEnabled = false

                // Logic for Realtime Database update has been removed.
                // In the future, this is where you will send a POST request to your SQL API.
                Log.d("PasswordReset", "Local validation successful for $userEmail")

                Toast.makeText(this, "Local validation successful! Redirecting to login...", Toast.LENGTH_LONG).show()

                // Navigate back to login screen
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}