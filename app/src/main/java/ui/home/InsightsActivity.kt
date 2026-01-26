package com.example.piggybank.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R

class InsightsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)

        // Initialize back button
        btnBack = findViewById(R.id.btnBack)

        val weekly = findViewById<TextView>(R.id.tvWeekly)
        val monthly = findViewById<TextView>(R.id.tvMonthly)
        val yearly = findViewById<TextView>(R.id.tvYearly)

        fun select(selected: TextView) {
            listOf(weekly, monthly, yearly).forEach {
                it.setBackgroundResource(R.drawable.toggle_unselected)
                it.setTextColor(Color.parseColor("#9E9E9E"))
            }
            selected.setBackgroundResource(R.drawable.toggle_selected)
            selected.setTextColor(Color.WHITE)
        }

        weekly.setOnClickListener { select(weekly) }
        monthly.setOnClickListener { select(monthly) }
        yearly.setOnClickListener { select(yearly) }

        select(weekly)

        // Back button click listener - ALWAYS go to Home
        btnBack.setOnClickListener {
            navigateToHome()
        }

        BottomMenuHelper.setupMenu(this, "insights")
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // Clear the back stack so user can't go back to Insights with back button
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Finish current activity so it's removed from stack
        overridePendingTransition(0, 0)
    }

    // Optional: Override device back button to also go to Home
    override fun onBackPressed() {
        navigateToHome()
    }
}