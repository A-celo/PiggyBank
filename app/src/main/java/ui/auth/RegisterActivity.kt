package com.example.piggybank.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.EditText
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
            //setContentView(R.layout.activity_sign_in)
        }

        //back button when registering - botón de regresar cuando te registras
        /*findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            setContentView(R.layout.activity_sign_in)
        }*/

        // ---------- Botón Register ----------
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {

            // logica de validacion
            val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword).text.toString().trim()
            val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword).text.toString().trim()

            if (email.isEmpty()) {
                showErrorDialog("Email cannot be empty")
            } else if (password.length < 6) {
                showErrorDialog("Password must be at least 6 characters")
            } else if (password != confirmPassword) {
                showErrorDialog("Passwords do not match")
            } else {
                showConfirmationDialog()
            }
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
            //setContentView(R.layout.activity_sign_in)
        }

        //btnRegister.setOnClickListener {
        //    showConfirmationDialog()
        //}
    }

    private fun showConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Botón Ok
        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    // mostramos mensajes de error más especificos
    private fun showErrorDialog(message: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_error, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val tvMessage = dialogView.findViewById<TextView>(R.id.tvErrorMessage)
        tvMessage.text = message

        dialogView.findViewById<Button>(R.id.btnOkError).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
