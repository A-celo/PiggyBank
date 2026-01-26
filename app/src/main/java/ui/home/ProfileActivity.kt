package com.example.piggybank.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnLogout: Button
    private lateinit var deleteAccount: TextView

    // textviews (no edittext)
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPassword: TextView
    private lateinit var tvCurrency: TextView

    // edit buttons
    private lateinit var btnEditName: FrameLayout
    private lateinit var btnEditEmail: FrameLayout
    private lateinit var btnEditPassword: FrameLayout
    private lateinit var btnEditCurrency: FrameLayout

    private val auth by lazy { FirebaseAuth.getInstance() }

    // firestore instance 
    private val db by lazy { FirebaseFirestore.getInstance() }

    // simple local storage for name/currency (to persist without backend)
    private val prefs by lazy {
        val uid = auth.currentUser?.uid ?: "guest"
        getSharedPreferences("profile_prefs_$uid", MODE_PRIVATE)
    }

    private lateinit var ivProfilePhoto: ImageView

    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                ivProfilePhoto.setImageURI(uri)
                prefs.edit().putString("profile_photo_uri", uri.toString()).apply()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // views
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

        ivProfilePhoto.setOnClickListener {
            pickPhoto.launch("image/*")
        }

        // back
        btnBack.setOnClickListener {
            finish()
        }

        // log out
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            Snackbar.make(it, "logged out", Snackbar.LENGTH_SHORT).show()

            val intent = Intent(
                this,
                com.example.piggybank.ui.auth.SignInActivity::class.java
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // delete account (real)
        deleteAccount.setOnClickListener { view ->
            confirmDeleteAccount(view)
        }

        // edit name
        btnEditName.setOnClickListener { view ->
            showTextEditDialog(
                title = "edit name",
                initialValue = tvName.text.toString(),
                inputType = InputType.TYPE_CLASS_TEXT
            ) { newValue ->
                tvName.text = newValue
                prefs.edit().putString("name", newValue).apply()
                Snackbar.make(view, "name updated", Snackbar.LENGTH_SHORT).show()
            }
        }

        // edit email (firebase)
        btnEditEmail.setOnClickListener { view ->
            val user = auth.currentUser
            if (user == null) {
                Snackbar.make(view, "no user logged in", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ask for new email + current password (re-auth)
            showTwoFieldDialog(
                title = "change email",
                firstHint = "new email",
                secondHint = "current password"
            ) { newEmail, currentPassword ->
                val currentEmail = user.email
                if (currentEmail.isNullOrBlank()) {
                    Snackbar.make(view, "current email not found", Snackbar.LENGTH_SHORT).show()
                    return@showTwoFieldDialog
                }

                val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (!reauthTask.isSuccessful) {
                        Snackbar.make(view, "wrong password", Snackbar.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    user.updateEmail(newEmail).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            tvEmail.text = newEmail
                            Snackbar.make(view, "email updated", Snackbar.LENGTH_SHORT).show()

                            // update firestore
                            db.collection("users")
                                .document(user.uid)
                                .update(
                                    mapOf(
                                        "email" to newEmail,
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                        } else {
                            Snackbar.make(
                                view,
                                "email update failed: ${updateTask.exception?.message}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // edit password (firebase)
        btnEditPassword.setOnClickListener { view ->
            val user = auth.currentUser
            if (user == null) {
                Snackbar.make(view, "no user logged in", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ask for current password + new password
            showTwoFieldDialog(
                title = "change password",
                firstHint = "current password",
                secondHint = "new password",
                firstIsPassword = true,
                secondIsPassword = true
            ) { currentPassword, newPassword ->
                if (newPassword.length < 6) {
                    Snackbar.make(view, "password must be at least 6 characters", Snackbar.LENGTH_LONG).show()
                    return@showTwoFieldDialog
                }

                val currentEmail = user.email
                if (currentEmail.isNullOrBlank()) {
                    Snackbar.make(view, "current email not found", Snackbar.LENGTH_SHORT).show()
                    return@showTwoFieldDialog
                }

                val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (!reauthTask.isSuccessful) {
                        Snackbar.make(view, "wrong current password", Snackbar.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            tvPassword.text = "••••••••"
                            Snackbar.make(view, "password updated", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Snackbar.make(
                                view,
                                "password update failed: ${updateTask.exception?.message}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // edit currency (simple picker)
        btnEditCurrency.setOnClickListener { view ->
            val options = arrayOf("USD", "EUR", "PLN", "GBP")
            AlertDialog.Builder(this)
                .setTitle("select currency")
                .setItems(options) { _, which ->
                    val selected = options[which]
                    tvCurrency.text = selected
                    prefs.edit().putString("currency", selected).apply()

                    // update firestore
                    val user = auth.currentUser
                    if (user != null) {
                        db.collection("users")
                            .document(user.uid)
                            .update(
                                mapOf(
                                    "currency" to selected,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                    }

                    Snackbar.make(view, "currency updated", Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton("cancel", null)
                .show()
        }

        // load data
        loadUserProfile()

        BottomMenuHelper.setupMenu(this, "profile")
    }

    private fun loadUserProfile() {
        val user = auth.currentUser

        // name/currency local
        tvName.text = prefs.getString("name", "Maria Wozniak")
        tvCurrency.text = prefs.getString("currency", "USD")

        // email from firebase
        tvEmail.text = user?.email ?: "no email"

        tvPassword.text = "••••••••"

        val savedUri = prefs.getString("profile_photo_uri", null)
        if (!savedUri.isNullOrBlank()) {
            ivProfilePhoto.setImageURI(Uri.parse(savedUri))
        } else {
            ivProfilePhoto.setImageResource(R.drawable.generic_ava)
        }
    }

    private fun confirmDeleteAccount(view: android.view.View) {
        val user = auth.currentUser
        if (user == null) {
            Snackbar.make(view, "no user logged in", Snackbar.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("delete account")
            .setMessage("this action cannot be undone. do you want to continue?")
            .setNegativeButton("cancel", null)
            .setPositiveButton("delete") { _, _ ->
                // to delete account, recent re-auth is required
                // ask for current password
                showTextEditDialog(
                    title = "confirm password",
                    initialValue = "",
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    hint = "current password"
                ) { currentPassword ->
                    val email = user.email
                    if (email.isNullOrBlank()) {
                        Snackbar.make(view, "current email not found", Snackbar.LENGTH_SHORT).show()
                        return@showTextEditDialog
                    }

                    val credential = EmailAuthProvider.getCredential(email, currentPassword)
                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (!reauthTask.isSuccessful) {
                            Snackbar.make(view, "wrong password", Snackbar.LENGTH_LONG).show()
                            return@addOnCompleteListener
                        }

                        user.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Snackbar.make(view, "account deleted", Snackbar.LENGTH_SHORT).show()

                                val intent = Intent(
                                    this,
                                    com.example.piggybank.ui.auth.SignInActivity::class.java
                                )
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                Snackbar.make(
                                    view,
                                    "delete failed: ${deleteTask.exception?.message}",
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
            .setNegativeButton("cancel", null)
            .setPositiveButton("save") { _, _ ->
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
            .setNegativeButton("cancel", null)
            .setPositiveButton("save") { _, _ ->
                onSave(
                    first.text.toString().trim(),
                    second.text.toString().trim()
                )
            }
            .show()
    }
}
