package com.ezio.unishare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ForgetActivity : AppCompatActivity() {

    // --- UI Components ---
    private lateinit var emailEditText: EditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var sendOtpButton: Button
    private lateinit var otpEditText: EditText
    private lateinit var otpInputLayout: TextInputLayout
    private lateinit var verifyOtpButton: Button
    private lateinit var resendOtpTextView: TextView
    private lateinit var otpTimerTextView: TextView
    private lateinit var instructionTextView: TextView
    private lateinit var titleTextView: TextView

    // --- Logic Variables ---
    private var countDownTimer: CountDownTimer? = null
    private val otpTimerDuration = 5 * 60 * 1000L // 5 minutes
    private var isTimerRunning = false
    private var correctOtp: String? = null
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget)

        firebaseAuth = FirebaseAuth.getInstance()

        // --- Find all views ---
        emailEditText = findViewById(R.id.editTextEmail)
        emailInputLayout = findViewById(R.id.textInputLayoutEmail)
        sendOtpButton = findViewById(R.id.buttonSendOtp)
        otpEditText = findViewById(R.id.editTextOtp)
        otpInputLayout = findViewById(R.id.textInputLayoutOtp)
        verifyOtpButton = findViewById(R.id.buttonVerifyOtp)
        resendOtpTextView = findViewById(R.id.textViewResendOtp)
        otpTimerTextView = findViewById(R.id.textViewOtpTimer)
        instructionTextView = findViewById(R.id.textViewInstruction)
        titleTextView = findViewById(R.id.textViewTitle)

        // --- Initial UI State ---
        otpInputLayout.visibility = View.GONE
        verifyOtpButton.visibility = View.GONE
        resendOtpTextView.visibility = View.GONE
        otpTimerTextView.visibility = View.GONE

        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake_anim)

        // --- "Send OTP" Button Logic ---
        sendOtpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isBlank() || !email.endsWith("@tkmce.ac.in")) {
                emailInputLayout.error = "Please enter your registered TKMCE email"
                emailInputLayout.startAnimation(shakeAnimation)
                return@setOnClickListener
            }
            emailInputLayout.error = null
            it.isEnabled = false
            Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show()
            sendOtpInBackground(email, it as Button)
        }

        // --- "Verify OTP" Button Logic ---
        verifyOtpButton.setOnClickListener {
            val enteredOtp = otpEditText.text.toString().trim()
            val userEmail = emailEditText.text.toString().trim()

            if (enteredOtp.length != 6) {
                otpInputLayout.error = "OTP must be 6 digits"
                otpInputLayout.startAnimation(shakeAnimation)
                return@setOnClickListener
            }

            if (enteredOtp == correctOtp) {
                Toast.makeText(this, "OTP Verified! Please wait...", Toast.LENGTH_LONG).show()
                countDownTimer?.cancel()
                it.isEnabled = false


// --- NEW LOGIC: Directly go to SetNewPasswordActivity with email ---
                val intent = Intent(this, SetNewPasswordActivity::class.java)
                intent.putExtra("EXTRA_EMAIL", userEmail) // ✅ Pass email to next activity
                startActivity(intent)
                finish()

            } else {
                otpInputLayout.error = "Invalid OTP"
                otpInputLayout.startAnimation(shakeAnimation)
                Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_LONG).show()
            }
        }

        // (Resend OTP logic can remain as you had it)
    }

    // --- Send OTP in background ---
    private fun sendOtpInBackground(email: String, button: Button) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val emailService = EmailService()
                val otp = emailService.sendOtp(email)

                withContext(Dispatchers.Main) {
                    if (otp != null) {
                        correctOtp = otp
                        Log.d("ForgetActivity", "OTP sent successfully: $correctOtp")
                        Toast.makeText(applicationContext, "OTP Sent! Check your email.", Toast.LENGTH_LONG).show()

                        titleTextView.text = "Verify Account"
                        emailInputLayout.visibility = View.GONE
                        button.visibility = View.GONE
                        instructionTextView.text = "Enter the OTP sent to $email"

                        otpInputLayout.visibility = View.VISIBLE
                        verifyOtpButton.visibility = View.VISIBLE
                        resendOtpTextView.visibility = View.VISIBLE
                        otpTimerTextView.visibility = View.VISIBLE
                        startTimer()
                    } else {
                        button.isEnabled = true
                        Toast.makeText(applicationContext, "Failed to send OTP. Check logs.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    button.isEnabled = true
                    Toast.makeText(applicationContext, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Start OTP Timer ---
    private fun startTimer() {
        countDownTimer?.cancel()
        resendOtpTextView.isEnabled = false
        resendOtpTextView.alpha = 0.5f
        isTimerRunning = true
        countDownTimer = object : CountDownTimer(otpTimerDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                otpTimerTextView.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                isTimerRunning = false
                otpTimerTextView.text = "00:00"
                resendOtpTextView.isEnabled = true
                resendOtpTextView.alpha = 1.0f
                Toast.makeText(this@ForgetActivity, "OTP expired.", Toast.LENGTH_SHORT).show()
            }
        }.start()
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