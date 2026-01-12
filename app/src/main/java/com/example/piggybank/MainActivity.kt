package com.example.piggybank

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.*
import com.google.firebase.initialize

class MainActivity : AppCompatActivity() {

    // Declarar Firebase Auth
    private lateinit var auth: FirebaseAuth

    // Declarar views (los agregaremos después en el layout)
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar Firebase
        Firebase.initialize(this)
        auth = Firebase.auth

        setContentView(R.layout.activity_main)

        // Inicializar views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogin = findViewById(R.id.btnLogin)

        // Configurar listeners de botones
        btnRegister.setOnClickListener {
            registerUser()
        }

        btnLogin.setOnClickListener {
            loginUser()
        }

        // Verificar si ya hay usuario logueado
        checkCurrentUser()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validaciones básicas
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // Registrar usuario en Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso
                    val user = auth.currentUser
                    Toast.makeText(
                        this,
                        "¡Registro exitoso! Bienvenido a PiggyBank ${user?.email}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Aquí podrías navegar a otra actividad después del registro
                    // startActivity(Intent(this, HomeActivity::class.java))
                    // finish()

                } else {
                    // Error en registro
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(
                        this,
                        "Error en registro: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validaciones básicas
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // Iniciar sesión en Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login exitoso
                    val user = auth.currentUser
                    Toast.makeText(
                        this,
                        "¡Bienvenido de vuelta ${user?.email}!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Aquí podrías navegar a otra actividad después del login
                    // startActivity(Intent(this, DashboardActivity::class.java))
                    // finish()

                } else {
                    // Error en login
                    val errorMessage = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(
                        this,
                        "Error en login: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuario ya está logueado, mostrar mensaje
            Toast.makeText(
                this,
                "Ya estás conectado como: ${currentUser.email}",
                Toast.LENGTH_LONG
            ).show()

            // Podrías redirigir automáticamente a la pantalla principal
            // startActivity(Intent(this, HomeActivity::class.java))
            // finish()
        } else {
            Toast.makeText(
                this,
                "Bienvenido a PiggyBank. Por favor regístrate o inicia sesión",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Método para cerrar sesión (puedes agregar un botón después)
    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        // Limpiar campos
        etEmail.text.clear()
        etPassword.text.clear()
    }
}