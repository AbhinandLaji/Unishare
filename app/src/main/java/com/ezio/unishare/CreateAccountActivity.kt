package com.ezio.unishare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake_anim)

        // --- Clear errors on typing ---
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

        // --- Button click ---
        createAccountButton.setOnClickListener {
            val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale_anim)
            it.startAnimation(scaleAnimation)

            // --- Read all fields as non-null Strings ---
            val firstName: String = firstNameEditText.text.toString().trim()
            val lastName: String = lastNameEditText.text.toString().trim()
            val email: String = collegeEmailEditText.text.toString().trim()
            val phone: String = phoneEditText.text.toString().trim()
            val password: String = passwordEditText.text.toString()
            val confirmPassword: String = confirmPasswordEditText.text.toString()

            var isValid = true

            // --- Validation ---
            if (firstName.isEmpty()) {
                firstNameLayout.error = "Enter first name"
                isValid = false
            }
            if (lastName.isEmpty()) {
                lastNameLayout.error = "Enter last name"
                isValid = false
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Enter valid email"
                isValid = false
            }
            if (phone.length != 10) {
                phoneLayout.error = "Enter valid phone number"
                isValid = false
            }
            if (password != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"
                isValid = false
            }
            if (!isPasswordStrong(password)) {
                isValid = false
            }

            if (!isValid) {
                Toast.makeText(this, "Please correct the errors.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- Everything valid, send OTP ---
            it.isEnabled = false
            Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show()

            CoroutineScope(Dispatchers.IO).launch {
                val emailService = EmailService()
                val otp: String? = emailService.sendOtp(email)
                withContext(Dispatchers.Main) {
                    if (otp != null) {
                        Toast.makeText(this@CreateAccountActivity, "OTP sent to $email", Toast.LENGTH_SHORT).show()

                        // Launch OTP verification activity safely
                        val intent = Intent(this@CreateAccountActivity, OtpVerificationActivity::class.java)
                        intent.putExtra("EXTRA_FIRST_NAME", firstName)
                        intent.putExtra("EXTRA_LAST_NAME", lastName)
                        intent.putExtra("EXTRA_EMAIL", email)
                        intent.putExtra("EXTRA_PHONE", phone)
                        intent.putExtra("EXTRA_PASSWORD", password)
                        intent.putExtra("EXTRA_OTP", otp)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@CreateAccountActivity, "Failed to send OTP. Try again.", Toast.LENGTH_LONG).show()
                        it.isEnabled = true
                    }
                }
            }
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val errors = mutableListOf<String>()

        if (password.length < passwordMinLength) errors.add("Min $passwordMinLength characters")
        if (!password.matches(hasUpperCasePattern)) errors.add("At least 1 uppercase letter")
        if (!password.matches(hasLowerCasePattern)) errors.add("At least 1 lowercase letter")
        if (!password.matches(hasDigitPattern)) errors.add("At least 1 digit")
        if (!password.matches(hasSpecialCharPattern)) errors.add("At least 1 special character")

        return if (errors.isEmpty()) {
            true
        } else {
            textViewPasswordCriteriaErrorsCreate.text = errors.joinToString("\n")
            textViewPasswordCriteriaErrorsCreate.visibility = View.VISIBLE
            false
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
