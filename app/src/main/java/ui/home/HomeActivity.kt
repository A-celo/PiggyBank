package com.example.piggybank.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.example.piggybank.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.*
import com.google.firebase.*
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.piggybank.utils.CurrencyHelper

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
        // Calculate 7 days ago
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = calendar.time
        val now = Date()

        // Query expenses for last 7 days
        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", sevenDaysAgo)
            .whereLessThanOrEqualTo("date", now)
            .get()
            .addOnSuccessListener { querySnapshot ->
                var total = 0.0

                for (document in querySnapshot.documents) {
                    val amount = document.getDouble("amount") ?: 0.0
                    total += amount
                }

                val formattedAmount = CurrencyHelper.formatAmount(this, total)

                // Show different message based on count
                val count = querySnapshot.documents.size
                if (count == 0) {
                    tvWeeklySpend.text = "No expenses in the last 7 days"
                } else {
                    tvWeeklySpend.text = "You've spent $formattedAmount in the last 7 days"
                }
            }
    }
}