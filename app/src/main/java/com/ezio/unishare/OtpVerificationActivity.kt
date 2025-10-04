package com.ezio.unishare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var editTextOtp: EditText
    private lateinit var buttonVerifyOtp: Button
    private lateinit var textViewResendOtp: TextView
    private lateinit var textViewOtpTimer: TextView
    private lateinit var textViewInstruction: TextView

    private var countDownTimer: CountDownTimer? = null
    private val otpTimerDuration = 5 * 60 * 1000L // 5 minutes
    private var isTimerRunning = false
    private var correctOtp: String? = null

    // User details passed from CreateAccountActivity
    private var firstName: String = ""
    private var lastName: String = ""
    private var email: String = ""
    private var phone: String = ""
    private var password: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        // --- Find views ---
        editTextOtp = findViewById(R.id.editTextOtp)
        buttonVerifyOtp = findViewById(R.id.buttonVerifyOtp)
        textViewResendOtp = findViewById(R.id.textViewResendOtp)
        textViewOtpTimer = findViewById(R.id.textViewOtpTimer)
        textViewInstruction = findViewById(R.id.textViewForgetInstruction)

        // --- Get data from intent ---
        firstName = intent.getStringExtra("EXTRA_FIRST_NAME") ?: ""
        lastName = intent.getStringExtra("EXTRA_LAST_NAME") ?: ""
        email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        phone = intent.getStringExtra("EXTRA_PHONE") ?: ""
        password = intent.getStringExtra("EXTRA_PASSWORD") ?: ""
        correctOtp = intent.getStringExtra("EXTRA_OTP") ?: ""

        Log.d("OtpVerification", "Correct OTP received: $correctOtp")

        if (email.isNotEmpty()) {
            textViewInstruction.text = "Enter the OTP sent to $email to verify your account."
        }

        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake_anim)
        startOtpTimer(otpTimerDuration)

        // --- Verify OTP ---
        buttonVerifyOtp.setOnClickListener {
            val enteredOtp = editTextOtp.text.toString().trim()

            if (enteredOtp.length != 6) {
                editTextOtp.error = "OTP must be 6 digits"
                editTextOtp.startAnimation(shakeAnimation)
                return@setOnClickListener
            }

            if (enteredOtp == correctOtp) {
                countDownTimer?.cancel()
                saveUserToDatabase(firstName, lastName, email, phone, password)
            } else {
                editTextOtp.error = "Invalid OTP"
                editTextOtp.startAnimation(shakeAnimation)
                Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Resend OTP ---
        textViewResendOtp.setOnClickListener {
            if (!isTimerRunning && email.isNotEmpty()) {
                Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show()
                resendOtpInBackground(email)
            } else {
                Toast.makeText(this, "Please wait for the timer to finish.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resendOtpInBackground(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val emailService = EmailService()
            val newOtp = emailService.sendOtp(email)
            withContext(Dispatchers.Main) {
                if (newOtp != null) {
                    correctOtp = newOtp
                    Log.d("OtpVerification", "New OTP sent: $correctOtp")
                    Toast.makeText(applicationContext, "A new OTP has been sent.", Toast.LENGTH_SHORT).show()
                    editTextOtp.text.clear()
                    editTextOtp.error = null
                    startOtpTimer(otpTimerDuration)
                } else {
                    Toast.makeText(applicationContext, "Failed to resend OTP.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startOtpTimer(duration: Long) {
        countDownTimer?.cancel()
        textViewResendOtp.isEnabled = false
        textViewResendOtp.alpha = 0.5f

        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                isTimerRunning = true
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                textViewOtpTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
            override fun onFinish() {
                isTimerRunning = false
                textViewOtpTimer.text = "00:00"
                textViewResendOtp.isEnabled = true
                textViewResendOtp.alpha = 1.0f
                Toast.makeText(this@OtpVerificationActivity, "OTP expired.", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun saveUserToDatabase(firstName: String, lastName: String, email: String, phone: String, password: String) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        val key = email.replace(".", "_")

        val userData = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "collegeMail" to email,
            "phone" to phone,
            "password" to password // ⚠️ plain password for learning only
        )

        usersRef.child(key).setValue(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_LONG).show()
                Log.d("DB_SAVE", "User saved to Firebase")

                // After saving, navigate to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save account: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("DB_SAVE", "Error saving user", e)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}


