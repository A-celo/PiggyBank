package com.example.piggybank.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.example.piggybank.ui.home.HomeActivity

class SignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // ---------- EditTexts ----------
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        // Solo front: texto de ejemplo
        etEmail.setText("maria@example.com")
        etPassword.setText("password123")

        // ---------- Botón Log In ----------
        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        // ---------- Texto "Don't have an account? Register here!" ----------
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val fullText = tvRegister.text.toString()  // <-- usamos el texto que ya existe
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf("Register here!")
        val end = start + "Register here!".length

        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#C7B8EA")),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvRegister.text = spannable

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // ---------- "I forgot my password" ----------
        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}


