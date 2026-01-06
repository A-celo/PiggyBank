package com.example.piggybank.ui.home

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R

class AddExpenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        BottomMenuHelper.setupMenu(this, "add")
    }
}



