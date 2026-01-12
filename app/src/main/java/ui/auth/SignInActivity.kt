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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.example.piggybank.ui.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.*
import com.google.firebase.*

class SignInActivity : AppCompatActivity() {

    // Declarar Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Inicializar Firebase Auth
        auth = Firebase.auth

        // ---------- EditTexts ----------
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        // Quitar el texto de ejemplo para producción
        // etEmail.setText("maria@example.com")
        // etPassword.setText("password123")

        // ---------- Botón Log In ----------
        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validaciones
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mostrar progreso (puedes agregar un ProgressBar después)
            // findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
            findViewById<Button>(R.id.btnSignIn).isEnabled = false

            // Iniciar sesión con Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    // Ocultar progreso
                    // findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                    findViewById<Button>(R.id.btnSignIn).isEnabled = true

                    if (task.isSuccessful) {
                        // Login exitoso
                        val user = auth.currentUser

                        // Verificar si el email está verificado (opcional)
                        if (user?.isEmailVerified == false) {
                            // Puedes pedir verificación o permitir acceso igual
                            Toast.makeText(
                                this,
                                "Inicio de sesión exitoso. Verifica tu email para acceder a todas las funciones.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // Ir a HomeActivity
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish() // Cerrar SignInActivity para que no vuelva atrás

                    } else {
                        // Error en login
                        val errorMessage = when {
                            task.exception?.message?.contains("network") == true ->
                                "Error de conexión. Verifica tu internet."
                            task.exception?.message?.contains("invalid credential") == true ->
                                "Email o contraseña incorrectos"
                            task.exception?.message?.contains("user-not-found") == true ->
                                "Usuario no encontrado. Regístrate primero."
                            task.exception?.message?.contains("too-many-requests") == true ->
                                "Demasiados intentos. Intenta más tarde."
                            else ->
                                "Error: ${task.exception?.message ?: "Error desconocido"}"
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        // ---------- Texto "Don't have an account? Register here!" ----------
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val fullText = tvRegister.text.toString()
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
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa tu email para recuperar la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Enviar email de recuperación
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Email de recuperación enviado a $email",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Error al enviar email: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            // También puedes ir a la actividad de recuperación si la tienes
            // startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        // Verificar si ya hay un usuario autenticado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si ya está autenticado, ir directamente a Home
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}