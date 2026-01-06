package com.example.piggybank.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.example.piggybank.ui.home.HomeActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ---------- Botón de volver atrás ----------
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // ---------- Botón Register ----------
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // ---------- Texto "Already have an account? Sign In" ----------
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        val fullText = "Already have an account? Sign In"
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Sign In")
        val end = start + "Sign In".length
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#C7B8EA")),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        tvSignIn.text = spannable

        // Click para ir a SignInActivity
        tvSignIn.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

