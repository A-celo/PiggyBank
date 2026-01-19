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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.example.piggybank.ui.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.*
import com.google.firebase.*

class RegisterActivity : AppCompatActivity() {

    // Declarar Firebase Auth y Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar Firebase Auth y Firestore
        auth = Firebase.auth
        db = Firebase.firestore

        // ---------- Botón de volver atrás ----------
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // ---------- Botón Register ----------
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener {
            registerUser()
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

    private fun registerUser() {
        // Obtener valores de los campos
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validaciones básicas
        if (email.isEmpty()) {
            showErrorDialog("Email cannot be empty")
            etEmail.requestFocus()
            return
        }

        if (!isValidEmail(email)) {
            showErrorDialog("Please enter a valid email address")
            etEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            showErrorDialog("Password must be at least 6 characters")
            etPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            showErrorDialog("Passwords do not match")
            etConfirmPassword.requestFocus()
            return
        }

        // Deshabilitar botón para evitar múltiples clics
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.isEnabled = false
        btnRegister.text = "Creating Account..."

        // Registrar usuario en Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro en Auth exitoso
                    val user = auth.currentUser

                    if (user != null) {
                        // Guardar datos adicionales en Firestore
                        saveUserToFirestore(user.uid, email)

                        // Enviar email de verificación (opcional)
                        sendEmailVerification(user)

                        // Mostrar diálogo de confirmación
                        showSuccessDialog(email)
                    }

                } else {
                    // Error en registro
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register"

                    val errorMessage = when {
                        task.exception?.message?.contains("already in use", ignoreCase = true) == true ->
                            "This email is already registered"
                        task.exception?.message?.contains("invalid email", ignoreCase = true) == true ->
                            "Invalid email format"
                        task.exception?.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Check your internet connection"
                        task.exception?.message?.contains("too many", ignoreCase = true) == true ->
                            "Too many attempts. Please try again later"
                        else ->
                            "Registration failed: ${task.exception?.message ?: "Unknown error"}"
                    }

                    showErrorDialog(errorMessage)
                }
            }
    }

    private fun saveUserToFirestore(userId: String, email: String) {
        // Crear objeto con los datos del usuario
        val userData = hashMapOf(
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "balance" to 0.0,
            "currency" to "USD",
            "isEmailVerified" to false,
            "totalSavings" to 0.0,
            "totalExpenses" to 0.0,
            "accountType" to "personal",
            "hasProfilePicture" to false
        )

        // Guardar en la colección "users" con el userId como ID del documento
        db.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                // Éxito al guardar en Firestore
                println("User data saved to Firestore: $userId")
            }
            .addOnFailureListener { e ->
                // Error al guardar en Firestore
                println("Error saving user data: ${e.message}")
                Toast.makeText(
                    this,
                    "Account created but additional data couldn't be saved",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun sendEmailVerification(user: com.google.firebase.auth.FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Email enviado exitosamente
                    println("Verification email sent to ${user.email}")
                } else {
                    // Error al enviar email
                    println("Failed to send verification email: ${task.exception?.message}")
                }
            }
    }

    private fun showSuccessDialog(email: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Personalizar mensaje si quieres
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvYourAccount)
        if (tvMessage != null) {
            tvMessage.text = "Account created successfully!\nCheck your email to verify your account."
        }

        // Botón Ok
        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()

            // Ir a SignInActivity
            val intent = Intent(this, SignInActivity::class.java)
            intent.putExtra("email", email) // Pasar el email como extra
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

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

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onStart() {
        super.onStart()

        // Si ya hay un usuario logueado, redirigir a Home
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}