package com.ezio.unishare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import android.util.Log

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var textViewPasswordCriteriaErrorsCreate: TextView

    // Password strength criteria
    private val passwordMinLength = 8
    private val hasUpperCasePattern = ".*[A-Z].*".toRegex()
    private val hasLowerCasePattern = ".*[a-z].*".toRegex()
    private val hasDigitPattern = ".*\\d.*".toRegex()
    private val hasSpecialCharPattern = ".*[!@#\$%^&*()_+\\-=\\[\\]{};':\\\"\\\\|,.<>/?].*".toRegex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        // --- Find Views ---
        val firstNameLayout = findViewById<TextInputLayout>(R.id.textInputLayoutFirstName)
        val lastNameLayout = findViewById<TextInputLayout>(R.id.textInputLayoutLastName)
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayoutCollegeEmail)
        val phoneLayout = findViewById<TextInputLayout>(R.id.textInputLayoutPhone)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutPassword)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.textInputLayoutConfirmPassword)

        val firstNameEditText = findViewById<EditText>(R.id.editTextFirstName)
        val lastNameEditText = findViewById<EditText>(R.id.editTextLastName)
        val collegeEmailEditText = findViewById<EditText>(R.id.editTextCollegeEmail)
        val phoneEditText = findViewById<EditText>(R.id.editTextPhone)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.editTextConfirmPassword)
        val createAccountButton = findViewById<Button>(R.id.buttonCreateAccountSubmit)

        textViewPasswordCriteriaErrorsCreate = findViewById(R.id.textViewPasswordCriteriaErrorsCreate)

        // --- Text Watchers to clear errors ---
        fun addTextWatcherToClearError(editText: EditText, layout: TextInputLayout) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.isNotEmpty() == true && layout.error != null) {
                        layout.error = null
                        if (editText == passwordEditText) {
                            textViewPasswordCriteriaErrorsCreate.visibility = View.GONE
                        }
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        addTextWatcherToClearError(firstNameEditText, firstNameLayout)
        addTextWatcherToClearError(lastNameEditText, lastNameLayout)
        addTextWatcherToClearError(collegeEmailEditText, emailLayout)
        addTextWatcherToClearError(phoneEditText, phoneLayout)
        addTextWatcherToClearError(passwordEditText, passwordLayout)
        addTextWatcherToClearError(confirmPasswordEditText, confirmPasswordLayout)


        createAccountButton.setOnClickListener {
            val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale_anim)
            it.startAnimation(scaleAnimation)

            val firstName = firstNameEditText.text.toString().trim()
            val lastName = lastNameEditText.text.toString().trim()
            val email = collegeEmailEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            var isValid = true

            // Basic validation check
            if (password != confirmPassword) {
                isValid = false
                confirmPasswordLayout.error = "Passwords do not match"
            }

            if (isValid) {
                // Button is disabled to prevent multiple clicks
                it.isEnabled = false

                // Logic for account creation is removed.
                // Redirecting to Login Screen as a placeholder.
                Toast.makeText(this, "Account data validated locally.", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please correct the errors.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}