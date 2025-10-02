package com.ezio.unishare

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Make activity full-screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // --- Session Check using SharedPreferences ---
            val sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE)
            val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
            val userEmail = sharedPref.getString("userEmail", null)

            if (isLoggedIn && userEmail != null) {
                // User already logged in → Go to HomeActivity
                val intent = Intent(this@SplashActivity, HomeActivity::class.java).apply {
                    putExtra("USER_EMAIL", userEmail)
                }
                startActivity(intent)
            } else {
                // User not logged in → Go to Login
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
            }

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish() // remove splash from back stack
        }, splashTimeOut)
    }
}
