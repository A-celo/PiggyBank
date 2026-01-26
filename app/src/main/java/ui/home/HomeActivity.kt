package com.example.piggybank.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.example.piggybank.model.Goal
import com.example.piggybank.ui.home.AddExpenseActivity
import com.example.piggybank.utils.CurrencyHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvWelcome: TextView
    private lateinit var tvWeeklySpend: TextView
    private lateinit var tvTopCategory: TextView
    private lateinit var tvGoal: TextView
    private lateinit var progressGoal: ProgressBar



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvWelcome = findViewById(R.id.tvWelcome)
        tvWeeklySpend = findViewById(R.id.tvWeeklySpend)
        tvTopCategory = findViewById(R.id.tvTopCategoryHome)
        tvGoal = findViewById(R.id.tvGoal)
        progressGoal = findViewById(R.id.progressGoal)

        findViewById<Button>(R.id.btnAddExpense).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        BottomMenuHelper.setupMenu(this, "home")
        loadUserData()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val user = auth.currentUser

        if (user == null) {
            tvWelcome.text = "Hi Guest!"
            tvWeeklySpend.text = "Sign in to track your expenses"
            tvTopCategory.text = "No data yet"
            tvGoal.text = "No goals yet"
            progressGoal.progress = 0
            return
        }

        val prefs = getSharedPreferences("profile_prefs", MODE_PRIVATE)
        val userName = prefs.getString("name", "User")
        tvWelcome.text = "Hi $userName!"

        loadWeeklyExpenses(user.uid)
        loadClosestGoal(user.uid)
    }

    private fun loadWeeklyExpenses(userId: String) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = calendar.time
        val now = Date()

        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", sevenDaysAgo)
            .whereLessThanOrEqualTo("date", now)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    tvWeeklySpend.text = "No expenses in the last 7 days"
                    tvTopCategory.text = "No category data yet"
                    return@addOnSuccessListener
                }

                var total = 0.0
                val categoryTotals = mutableMapOf<String, Double>()

                for (doc in snapshot.documents) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val category = doc.getString("category") ?: "Other"

                    total += amount
                    categoryTotals[category] =
                        categoryTotals.getOrDefault(category, 0.0) + amount
                }

                val formattedAmount = CurrencyHelper.formatAmount(this, total)
                tvWeeklySpend.text =
                    "You've spent $formattedAmount in the last 7 days"

                val topCategory =
                    categoryTotals.maxByOrNull { it.value }?.key ?: "Other"

                tvTopCategory.text =
                    "Top category this week: $topCategory"
            }
    }

    private fun loadClosestGoal(userId: String) {
        db.collection("goals")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    tvGoal.text = "No goals yet"
                    progressGoal.progress = 0
                    return@addOnSuccessListener
                }

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val today = Date()

                val goals = snapshot.documents.mapNotNull { doc ->
                    val goal = doc.toObject(Goal::class.java)
                    goal?.id = doc.id
                    goal
                }

                val closestGoal = goals.minByOrNull { goal ->
                    val goalDate = try {
                        sdf.parse(goal.completionDate)
                    } catch (e: Exception) {
                        null
                    }
                    val diff = goalDate?.time?.minus(today.time) ?: Long.MAX_VALUE
                    if (diff < 0) Long.MAX_VALUE else diff
                }

                if (closestGoal != null) {
                    val saved = CurrencyHelper.formatAmount(this, closestGoal.savedAmount)
                    val target = CurrencyHelper.formatAmount(this, closestGoal.targetAmount)

                    tvGoal.text = "$saved / $target"

                    progressGoal.progress =
                        if (closestGoal.targetAmount > 0)
                            ((closestGoal.savedAmount / closestGoal.targetAmount) * 100).toInt()
                        else 0
                }
            }
    }


}
