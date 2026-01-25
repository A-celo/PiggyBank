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
                        // 1. Guardar datos adicionales en Firestore
                        saveUserToFirestore(user.uid, email)

                        // 2. Enviar email de verificación AUTOMÁTICO
                        sendEmailVerification(user)

                    } else {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Register"
                        showErrorDialog("Registration completed but user not found")
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
        // Crear objeto con los datos del usuario para PiggyBank
        val userData = hashMapOf(
            "email" to email,
            "name" to "User",
            "createdAt" to FieldValue.serverTimestamp(),
            "userId" to userId,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "initialBalance" to 0.0,
            "currentBalance" to 0.0,
            "currency" to "USD",
            "totalSaved" to 0.0,
            "totalWithdrawn" to 0.0,
            "goalAmount" to 0.0,
            "goalName" to "My Savings Goal",
            "transactionsCount" to 0,
            "isEmailVerified" to false,
            "lastLogin" to FieldValue.serverTimestamp(),
            "accountStatus" to "active",
            "theme" to "light"
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
                // Re-habilitar botón independientemente del resultado
                val btnRegister = findViewById<Button>(R.id.btnRegister)
                btnRegister.isEnabled = true
                btnRegister.text = "Register"

                if (task.isSuccessful) {
                    // Email enviado exitosamente
                    showVerificationSuccessDialog(user.email ?: "")

                    // Registrar en Firestore que se envió el email
                    recordVerificationEmailSent(user.uid)

                } else {
                    // Error al enviar email - pero la cuenta se creó
                    showRegistrationSuccessDialog(user.email ?: "", false)
                    println("Failed to send verification email: ${task.exception?.message}")
                }
            }
    }

    private fun showVerificationSuccessDialog(email: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_email_sent, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Personalizar mensaje
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvMessage)
        tvMessage?.text = "Verification email sent to:\n$email\n\nPlease check your inbox and verify your email."

        // Botón "Open Email App"
        dialogView.findViewById<Button>(R.id.btnOpenEmail)?.setOnClickListener {
            // Intent para abrir app de email
            val emailIntent = Intent(Intent.ACTION_MAIN)
            emailIntent.addCategory(Intent.CATEGORY_APP_EMAIL)
            startActivity(emailIntent)
        }

        // Botón "Continue to Sign In"
        dialogView.findViewById<Button>(R.id.btnContinue)?.setOnClickListener {
            dialog.dismiss()
            goToSignIn(email)
        }

        dialog.show()
    }

    private fun showRegistrationSuccessDialog(email: String, verificationSent: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Personalizar mensaje
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvYourAccount)
        val message = if (verificationSent) {
            "Account created successfully!\nVerification email sent to $email"
        } else {
            "Account created!\n(Email verification not sent - you can verify later)"
        }
        tvMessage?.text = message

        // Botón Ok
        dialogView.findViewById<Button>(R.id.btnOk)?.setOnClickListener {
            dialog.dismiss()
            goToSignIn(email)
        }

        dialog.show()
    }

    private fun recordVerificationEmailSent(userId: String) {
        db.collection("users").document(userId)
            .update(
                "verificationEmailSent", true,
                "verificationEmailSentAt", FieldValue.serverTimestamp(),
                "verificationAttempts", FieldValue.increment(1)
            )
            .addOnFailureListener { e ->
                println("Could not record verification email: ${e.message}")
            }
    }

    private fun goToSignIn(email: String) {
        val intent = Intent(this, SignInActivity::class.java)
        intent.putExtra("email", email) // Pasar el email como extra
        intent.putExtra("fromRegister", true) // Indicar que viene del registro
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showErrorDialog(message: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_error, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val tvMessage = dialogView.findViewById<TextView>(R.id.tvErrorMessage)
        tvMessage?.text = message

        dialogView.findViewById<Button>(R.id.btnOkError)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onStart() {
        super.onStart()

        // Si ya hay un usuario logueado Y verificado, redirigir a Home
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        // Si está logueado pero NO verificado, quedarse aquí para que se pueda registrar otro
    }
}