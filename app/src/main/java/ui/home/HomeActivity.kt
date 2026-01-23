package com.example.piggybank.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.*
import com.google.firebase.*
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvWelcome: TextView
    private lateinit var tvWeeklySpend: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome)
        tvWeeklySpend = findViewById(R.id.tvWeeklySpend)

        // Load user data
        loadUserData()

        // Button to AddExpense
        findViewById<Button>(R.id.btnAddExpense).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        BottomMenuHelper.setupMenu(this, "home")
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to home
        loadUserData()
    }

    private fun loadUserData() {
        val user = auth.currentUser

        if (user == null) {
            tvWelcome.text = "Hi Guest!"
            tvWeeklySpend.text = "Sign in to track your expenses"
            return
        }

        // Load user name from SharedPreferences
        val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
        val userName = prefs.getString("name", "User")
        tvWelcome.text = "Hi $userName!"

        // Load weekly expenses
        loadWeeklyExpenses(user.uid)
    }

    private fun loadWeeklyExpenses(userId: String) {
        // Calculate start of week (Monday)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfWeek = calendar.time
        val now = Date()

        // Query expenses for this week
        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", startOfWeek)
            .whereLessThanOrEqualTo("date", now)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var total = 0.0

                for (document in querySnapshot.documents) {
                    val amount = document.getDouble("amount") ?: 0.0
                    total += amount
                }

                // Update UI with real spending
                val formattedTotal = String.format(Locale.getDefault(), "%.2f", total)
                tvWeeklySpend.text = "You've spent \$$formattedTotal this week."

                // Also save to SharedPreferences for quick access
                val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
                prefs.edit().putFloat("weekly_spend", total.toFloat()).apply()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading expenses: ${exception.message}", Toast.LENGTH_SHORT).show()
                tvWeeklySpend.text = "Error loading expenses"
            }
    }
}