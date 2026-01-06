package com.example.piggybank.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.example.piggybank.ui.home.HomeActivity
import android.widget.Button
import android.widget.TextView
import android.widget.EditText

class SignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // ---------- EditTexts ----------
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        etEmail.setText("maria@example.com")
        etPassword.setText("password123")

        // ---------- Botón Log In ----------
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        btnSignIn.setOnClickListener {
            // Solo front: abre HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // ---------- "Register here!" ----------
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.setOnClickListener {
            // Solo front: ir a RegisterActivity simulada
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // ---------- "I forgot my password" ----------
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            // Solo front: ir a ForgotPasswordActivity simulada
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}

