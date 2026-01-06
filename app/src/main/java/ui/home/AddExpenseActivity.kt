package com.example.piggybank.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import android.widget.ImageView

class AddExpenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        // Botón atrás
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Aquí podrías inicializar el RecyclerView y el micrófono
    }
}
