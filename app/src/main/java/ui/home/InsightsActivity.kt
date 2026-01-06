package com.example.piggybank.ui.home

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R

class InsightsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)

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

        BottomMenuHelper.setupMenu(this, "insights")
    }
}


