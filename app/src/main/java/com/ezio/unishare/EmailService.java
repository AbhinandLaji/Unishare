package com.ezio.unishare;

import android.util.Log;

public class EmailService {
    // This is now a "Dummy" function because your Python
    // backend handles the actual mailing now.
    public String sendOtp(String email) {
        Log.d("EmailService", "Python backend is now responsible for sending OTP to: " + email);
        // We return a placeholder because the Activity expects a String,
        // but the Python backend will generate the real one.
        return "BACKEND_HANDLED";
    }
}