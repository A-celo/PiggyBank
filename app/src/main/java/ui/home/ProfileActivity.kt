package com.example.piggybank.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.*
import com.google.firebase.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    private lateinit var btnBack: ImageView
    private lateinit var btnLogout: Button
    private lateinit var deleteAccount: TextView

    // TextViews (NO EditText)
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPassword: TextView
    private lateinit var tvCurrency: TextView

    // Edit buttons
    private lateinit var btnEditName: FrameLayout
    private lateinit var btnEditEmail: FrameLayout
    private lateinit var btnEditPassword: FrameLayout
    private lateinit var btnEditCurrency: FrameLayout

    private val auth by lazy { Firebase.auth }

    // Simple local storage for Name/Currency (para que se vea guardado sin backend)
    private val prefs by lazy { getSharedPreferences("profile_prefs", MODE_PRIVATE) }

    private lateinit var ivProfilePhoto: ImageView

    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // En algunos dispositivos no hace falta / no lo permite
                }

                ivProfilePhoto.setImageURI(uri)
                prefs.edit().putString("profile_photo_uri", uri.toString()).apply()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        db = Firebase.firestore

        // Views
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)
        deleteAccount = findViewById(R.id.btnDeleteAccount)

        tvName = findViewById(R.id.tvNameValue)
        tvEmail = findViewById(R.id.tvEmailValue)
        tvPassword = findViewById(R.id.tvPasswordValue)
        tvCurrency = findViewById(R.id.tvCurrencyValue)

        btnEditName = findViewById(R.id.btnEditName)
        btnEditEmail = findViewById(R.id.btnEditEmail)
        btnEditPassword = findViewById(R.id.btnEditPassword)
        btnEditCurrency = findViewById(R.id.btnEditCurrency)

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto)

        // Load data
        loadUserProfile()

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Profile photo
        ivProfilePhoto.setOnClickListener {
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
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

        // Delete account (real)
        deleteAccount.setOnClickListener { view ->
            confirmDeleteAccount(view)
        }

        // Edit Name
        btnEditName.setOnClickListener { view ->
            showTextEditDialog(
                title = "Edit name",
                initialValue = tvName.text.toString(),
                inputType = InputType.TYPE_CLASS_TEXT
            ) { newValue ->
                updateUserNameInFirestore(newValue, view)
            }
        }

        // Edit Email (Firebase)
        btnEditEmail.setOnClickListener { view ->
            val user = auth.currentUser
            if (user == null) {
                Snackbar.make(view, "No user logged in", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pedimos el email nuevo + password actual (re-auth)
            showTwoFieldDialog(
                title = "Change email",
                firstHint = "New email",
                secondHint = "Current password"
            ) { newEmail, currentPassword ->
                val currentEmail = user.email
                if (currentEmail.isNullOrBlank()) {
                    Snackbar.make(view, "Current email not found", Snackbar.LENGTH_SHORT).show()
                    return@showTwoFieldDialog
                }

                val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (!reauthTask.isSuccessful) {
                        Snackbar.make(view, "Wrong password", Snackbar.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    user.updateEmail(newEmail).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            tvEmail.text = newEmail
                            Snackbar.make(view, "Email updated", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Snackbar.make(
                                view,
                                "Email update failed: ${updateTask.exception?.message}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // Edit Password (Firebase)
        btnEditPassword.setOnClickListener { view ->
            val user = auth.currentUser
            if (user == null) {
                Snackbar.make(view, "No user logged in", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pedimos el password actual + password nueva
            showTwoFieldDialog(
                title = "Change password",
                firstHint = "Current password",
                secondHint = "New password",
                firstIsPassword = true,
                secondIsPassword = true
            ) { currentPassword, newPassword ->
                if (newPassword.length < 6) {
                    Snackbar.make(
                        view,
                        "Password must be at least 6 characters",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@showTwoFieldDialog
                }

                val currentEmail = user.email
                if (currentEmail.isNullOrBlank()) {
                    Snackbar.make(view, "Current email not found", Snackbar.LENGTH_SHORT).show()
                    return@showTwoFieldDialog
                }

                val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (!reauthTask.isSuccessful) {
                        Snackbar.make(view, "Wrong current password", Snackbar.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            tvPassword.text = "••••••••"
                            Snackbar.make(view, "Password updated", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Snackbar.make(
                                view,
                                "Password update failed: ${updateTask.exception?.message}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // Edit Currency (simple picker)
        btnEditCurrency.setOnClickListener { view ->
            val options = arrayOf("USD", "EUR", "PLN", "GBP")
            AlertDialog.Builder(this)
                .setTitle("Select currency")
                .setItems(options) { _, which ->
                    val selected = options[which]
                    tvCurrency.text = selected
                    prefs.edit().putString("currency", selected).apply()
                    Snackbar.make(view, "Currency updated", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        BottomMenuHelper.setupMenu(this, "profile")
    }

    private fun updateUserNameInFirestore(newName: String, view: View) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Update UI immediately
        tvName.text = newName

        // Save to SharedPreferences for offline use
        prefs.edit().putString("name", newName).apply()

        // Save to Firestore
        db.collection("users").document(user.uid)
            .update("name", newName, "updatedAt", FieldValue.serverTimestamp())
            .addOnSuccessListener {
                Snackbar.make(view, "Name updated successfully!", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(view, "Error updating name: ${e.message}", Snackbar.LENGTH_LONG)
                    .show()
            }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser

        // Name/Currency local
        tvName.text = prefs.getString("name", "Maria Wozniak")
        tvCurrency.text = prefs.getString("currency", "USD")

        // Email from Firebase if exists
        tvEmail.text = user?.email ?: "No email"

        tvPassword.text = "••••••••"

        val savedUri = prefs.getString("profile_photo_uri", null)
        if (savedUri != null) {
            ivProfilePhoto.setImageURI(Uri.parse(savedUri))
        }
    }

    private fun confirmDeleteAccount(view: View) {
        val user = auth.currentUser
        if (user == null) {
            Snackbar.make(view, "No user logged in", Snackbar.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete account")
            .setMessage("This action cannot be undone. Do you want to continue?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                // Para borrar cuenta, re-auth reciente.
                // Pedimos password actual.
                showTextEditDialog(
                    title = "Confirm password",
                    initialValue = "",
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    hint = "Current password"
                ) { currentPassword ->
                    val email = user.email
                    if (email.isNullOrBlank()) {
                        Snackbar.make(view, "Current email not found", Snackbar.LENGTH_SHORT).show()
                        return@showTextEditDialog
                    }

                    val credential = EmailAuthProvider.getCredential(email, currentPassword)
                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (!reauthTask.isSuccessful) {
                            Snackbar.make(view, "Wrong password", Snackbar.LENGTH_LONG).show()
                            return@addOnCompleteListener
                        }

                        user.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Snackbar.make(view, "Account deleted", Snackbar.LENGTH_SHORT).show()

                                val intent = Intent(
                                    this,
                                    com.example.piggybank.ui.auth.SignInActivity::class.java
                                )
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                Snackbar.make(
                                    view,
                                    "Delete failed: ${deleteTask.exception?.message}",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
            .show()
    }

    private fun showTextEditDialog(
        title: String,
        initialValue: String,
        inputType: Int,
        hint: String = "",
        onSave: (String) -> Unit
    ) {
        val input = android.widget.EditText(this)
        input.setText(initialValue)
        input.inputType = inputType
        if (hint.isNotBlank()) input.hint = hint

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                onSave(input.text.toString().trim())
            }
            .show()
    }

    private fun showTwoFieldDialog(
        title: String,
        firstHint: String,
        secondHint: String,
        firstIsPassword: Boolean = false,
        secondIsPassword: Boolean = true,
        onSave: (String, String) -> Unit
    ) {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val first = android.widget.EditText(this).apply {
            hint = firstHint
            inputType = if (firstIsPassword)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val second = android.widget.EditText(this).apply {
            hint = secondHint
            inputType = if (secondIsPassword)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT
        }

        layout.addView(first)
        layout.addView(second)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                onSave(first.text.toString().trim(), second.text.toString().trim())
            }
            .show()
    }
}