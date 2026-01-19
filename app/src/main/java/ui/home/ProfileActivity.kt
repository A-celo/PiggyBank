package com.example.piggybank.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnLogout: Button
    private lateinit var deleteAccount: TextView

    // TextViews (NO EditText)
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPassword: TextView
    private lateinit var tvCurrency: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Views
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)
        deleteAccount = findViewById(R.id.btnDeleteAccount)

        tvName = findViewById(R.id.tvNameValue)
        tvEmail = findViewById(R.id.tvEmailValue)
        tvPassword = findViewById(R.id.tvPasswordValue)
        tvCurrency = findViewById(R.id.tvCurrencyValue)

        // Back
        btnBack.setOnClickListener {
            finish()
        }

        // Log out
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            Snackbar.make(it, "Logged out", Snackbar.LENGTH_SHORT).show()

            val intent = Intent(
                this,
                com.example.piggybank.ui.auth.SignInActivity::class.java
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Delete account (placeholder)
        deleteAccount.setOnClickListener {
            Snackbar.make(it, "Account deleted", Snackbar.LENGTH_SHORT).show()
        }

        // Load data
        loadUserProfile()

        BottomMenuHelper.setupMenu(this, "profile")
    }

    private fun loadUserProfile() {
        tvName.text = "Maria Wozniak"
        tvEmail.text = "maria@email.com"
        tvPassword.text = "••••••••"
        tvCurrency.text = "USD"
    }
}

