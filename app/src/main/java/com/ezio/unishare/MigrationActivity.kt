package com.ezio.unishare

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * MigrationActivity
 * -----------------
 * This activity migrates users from Realtime Database to Firebase Authentication.
 * Shows a simple progress bar while migration happens.
 * After migration completes, automatically opens MainActivity.
 */
class MigrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---------------------- Simple ProgressBar layout ----------------------
        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true

        val layout = FrameLayout(this)
        layout.addView(progressBar)
        val params = progressBar.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.CENTER
        progressBar.layoutParams = params

        setContentView(layout) // Shows the progress bar immediately

        // ---------------------- Start migration ----------------------
        migrateUsers()
    }

    private fun migrateUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = FirebaseDatabase.getInstance()
                val usersRef = database.getReference("users")

                // Read all users from Realtime Database
                val snapshot = usersRef.get().addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        for (child in dataSnapshot.children) {
                            val email = child.child("collegeMail").getValue(String::class.java) ?: continue
                            val password = child.child("password").getValue(String::class.java) ?: continue

                            // ----------------- Create Firebase Authentication account -----------------
                            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    Log.d("Migration", "User $email migrated successfully ✅")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Migration", "Failed to migrate $email: ${e.message}", e)
                                }
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e("Migration", "Failed to read users: ${e.message}", e)
                }

                // Wait briefly to ensure Firebase operations are done (for demo only)
                kotlinx.coroutines.delay(3000)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MigrationActivity,
                        "Migration completed! Opening MainActivity...",
                        Toast.LENGTH_LONG
                    ).show()

                    // ----------------- Open MainActivity -----------------
                    startActivity(Intent(this@MigrationActivity, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MigrationActivity, "Migration error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }
}
