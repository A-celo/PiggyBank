package com.example.piggybank.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.piggybank.R
import android.content.Intent


class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // ---------- TextViews ----------
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvWeeklySpend = findViewById<TextView>(R.id.tvWeeklySpend)
        val tvMoneyLeft = findViewById<TextView>(R.id.tvMoneyLeft)
        val tvGoal = findViewById<TextView>(R.id.tvGoal)

        // Datos simulados
        tvWelcome.text = "Hi Maria!"
        tvWeeklySpend.text = "You've spent $120 this week."
        tvMoneyLeft.text = "$80 left"
        tvGoal.text = "$100 / $500 goal"

        // ---------- Botón Add Expense ----------
        val btnAddExpense = findViewById<Button>(R.id.btnAddExpense)
        btnAddExpense.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

        // ---------- Bottom Menu  ----------
        val menuHome = findViewById<LinearLayout>(R.id.menuHome)
        val menuAdd = findViewById<LinearLayout>(R.id.menuAdd)
        val menuInsights = findViewById<LinearLayout>(R.id.menuInsights)
        val menuGoals = findViewById<LinearLayout>(R.id.menuGoals)
        val menuProfile = findViewById<LinearLayout>(R.id.menuProfile)

        // Listener para que el menú "Add" abra AddExpenseActivity
        menuAdd.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }
    }


}








