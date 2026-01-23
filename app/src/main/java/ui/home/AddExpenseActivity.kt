package com.example.piggybank.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piggybank.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.*
import com.google.firebase.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ==================== CLASE MODELO (dentro del mismo archivo) ====================
data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: Date = Date(),
    val userId: String = ""
)

// ==================== CLASE ADAPTER (dentro del mismo archivo) ====================
class ExpenseAdapter(
    private val expenses: List<Expense>,
    private val onItemClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(expense: Expense) {
            tvAmount.text = String.format(Locale.getDefault(), "$%.2f", expense.amount)
            tvDescription.text = expense.description
            tvCategory.text = expense.category

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvDate.text = dateFormat.format(expense.date)

            itemView.setOnClickListener {
                onItemClick(expense)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size
}

// ==================== ACTIVIDAD PRINCIPAL ====================
class AddExpenseActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var expensesList: MutableList<Expense>

    private lateinit var rvExpenses: RecyclerView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var btnAddExpenseOverlay: Button

    private var selectedDate: Date = Calendar.getInstance().time

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        // Inicializar Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Inicializar vistas - AÑADIR EL TOTAL EXPENSES SI NO EXISTE
        rvExpenses = findViewById(R.id.rvExpenses)
        btnAddExpenseOverlay = findViewById(R.id.btnAddExpense)

        // Buscar o crear tvTotalExpenses
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        if (tvTotalExpenses == null) {
            // Si no existe en el layout, lo creamos programáticamente
            tvTotalExpenses = TextView(this)
        }

        // Configurar RecyclerView
        expensesList = mutableListOf()
        expenseAdapter = ExpenseAdapter(expensesList) { expense ->
            showExpenseOptions(expense)
        }

        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = expenseAdapter

        // Cargar gastos del usuario
        loadUserExpenses()

        // Configurar botón para abrir overlay
        btnAddExpenseOverlay.setOnClickListener {
            showAddExpenseOverlay()
        }
    }

    private fun showAddExpenseOverlay() {
        // Crear BottomSheetDialog
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null)

        // Inicializar elementos del diálogo
        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val etNote = view.findViewById<EditText>(R.id.etNote)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerPayment = view.findViewById<Spinner>(R.id.spinnerPayment)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvAmount = view.findViewById<TextView>(R.id.tvAmountPreview)
        val btnDatePicker = view.findViewById<ImageView>(R.id.btnDatePicker)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)

        // Configurar spinner de categorías
        val categories = arrayOf(
            "Food & Dining",
            "Transportation",
            "Shopping",
            "Entertainment",
            "Bills & Utilities",
            "Healthcare",
            "Education",
            "Personal Care",
            "Other"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Configurar spinner de métodos de pago
        val paymentMethods = arrayOf(
            "Cash",
            "Credit Card",
            "Debit Card",
            "Bank Transfer",
            "Digital Wallet",
            "PayPal",
            "Other"
        )
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentMethods)
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPayment.adapter = paymentAdapter

        // Configurar fecha inicial
        updateDateTextView(tvDate)

        // Configurar DatePicker
        btnDatePicker.setOnClickListener {
            showDatePickerDialog(tvDate)
        }

        // Actualizar preview del monto en tiempo real
        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val amountText = s.toString().trim()
                if (amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    tvAmount.text = String.format(Locale.getDefault(), "$%.2f", amount)
                } else {
                    tvAmount.text = "$0.00"
                }
            }
        })

        // Botón Guardar
        btnSave.setOnClickListener {
            val amountText = etAmount.text.toString().trim()
            val note = etNote.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()
            val paymentMethod = spinnerPayment.selectedItem.toString()

            if (validateExpenseInput(amountText, note)) {
                saveExpenseToFirebase(amountText, note, category, paymentMethod)
                dialog.dismiss()
            }
        }

        // Botón Cancelar
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Mostrar diálogo
        dialog.setContentView(view)
        dialog.show()
    }

    private fun updateDateTextView(tvDate: TextView) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        tvDate.text = dateFormat.format(selectedDate)
    }

    private fun showDatePickerDialog(tvDate: TextView) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, day)
                selectedDate = newCalendar.time
                updateDateTextView(tvDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateExpenseInput(amountText: String, note: String): Boolean {
        if (amountText.isEmpty()) {
            showErrorDialog("Please enter an amount")
            return false
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            showErrorDialog("Please enter a valid amount")
            return false
        }

        if (note.isEmpty()) {
            showErrorDialog("Please add a note")
            return false
        }

        return true
    }

    private fun saveExpenseToFirebase(amountText: String, note: String, category: String, paymentMethod: String) {
        val user = auth.currentUser
        if (user == null) {
            showErrorDialog("Please sign in first")
            return
        }

        val amount = amountText.toDouble()

        // Crear objeto Expense con paymentMethod
        val expense = hashMapOf(
            "userId" to user.uid,
            "amount" to amount,
            "description" to note,
            "category" to category,
            "paymentMethod" to paymentMethod,
            "date" to selectedDate,
            "createdAt" to FieldValue.serverTimestamp(),
            "currency" to "USD"
        )

        // Guardar en Firestore
        db.collection("expenses")
            .add(expense)
            .addOnSuccessListener { documentReference ->
                android.util.Log.d("AddExpenseActivity", "Expense added successfully: ${documentReference.id}")

                // Actualizar el balance del usuario
                updateUserBalance(user.uid, amount)

                // Actualizar estadísticas del usuario
                updateUserStats(user.uid, amount)

                // Mostrar mensaje de éxito
                showSuccessDialog()

                // Recargar la lista de gastos
                loadUserExpenses()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AddExpenseActivity", "Error adding expense: ${e.message}")
                showErrorDialog("Error adding expense: ${e.message}")
            }
    }

    private fun showSuccessDialog() {
        try {
            // Asegurar que estamos en el hilo principal
            runOnUiThread {
                // Crear un diálogo personalizado
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expense_confirmation, null)

                // Configurar el texto
                val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
                val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvSubtitle)
                val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

                tvTitle?.text = "Your expense was added successfully!"
                tvSubtitle?.text = "Keep saving! You almost met your goal"

                // Crear el diálogo
                val dialog = MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()

                // Configurar el botón OK
                btnOk?.setOnClickListener {
                    dialog.dismiss()
                }

                // Mostrar el diálogo
                try {
                    dialog.show()
                } catch (e: Exception) {
                    //android.util.Log.e("AddExpenseActivity", "Error showing success dialog: ${e.message}")
                    Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            //android.util.Log.e("AddExpenseActivity", "Error in showSuccessDialog: ${e.message}")
            Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showErrorDialog(message: String? = null) {
        try {
            // Asegurar que estamos en el hilo principal
            runOnUiThread {
                // Crear un diálogo personalizado
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expense_error, null)

                // Configurar el texto
                val tvTitle = dialogView.findViewById<TextView>(R.id.there_was_a)
                val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvErrorMessage)
                val btnOk = dialogView.findViewById<Button>(R.id.btnOkError)

                tvTitle?.text = "There was a mistake!"
                tvSubtitle?.text = message ?: "Try to add your expense again to keep saving!"

                // Crear el diálogo
                val dialog = MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()

                // Configurar el botón OK
                btnOk?.setOnClickListener {
                    dialog.dismiss()
                }

                // Mostrar el diálogo
                try {
                    dialog.show()
                } catch (e: Exception) {
                    //android.util.Log.e("AddExpenseActivity", "Error showing error dialog: ${e.message}")
                    Toast.makeText(this, message ?: "An error occurred", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            //android.util.Log.e("AddExpenseActivity", "Error in showErrorDialog: ${e.message}")
            Toast.makeText(this, message ?: "An error occurred", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUserBalance(userId: String, amount: Double) {
        db.collection("users").document(userId)
            .update("currentBalance", FieldValue.increment(-amount))
            .addOnFailureListener { e ->
                // Log error pero continuar
                println("Error updating balance: ${e.message}")
            }
    }

    private fun updateUserStats(userId: String, amount: Double) {
        val updates = hashMapOf<String, Any>(
            "totalWithdrawn" to FieldValue.increment(amount),
            "transactionsCount" to FieldValue.increment(1),
            "lastTransaction" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId)
            .update(updates)
            .addOnFailureListener { e ->
                println("Error updating stats: ${e.message}")
            }
    }

    private fun loadUserExpenses() {
        val user = auth.currentUser ?: return

        db.collection("expenses")
            .whereEqualTo("userId", user.uid)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Solo mostrar error si es crítico y no es un problema de permisos inicial
                    android.util.Log.e("AddExpenseActivity", "Error loading expenses: ${error.message}")
                    // No mostrar diálogo, solo loguear el error
                    return@addSnapshotListener
                }

                expensesList.clear()
                var total = 0.0

                snapshot?.documents?.forEach { document ->
                    val expense = Expense(
                        id = document.id,
                        amount = document.getDouble("amount") ?: 0.0,
                        description = document.getString("description") ?: "",
                        category = document.getString("category") ?: "Other",
                        date = (document.getDate("date") ?: Date()),
                        userId = document.getString("userId") ?: ""
                    )
                    expensesList.add(expense)
                    total += expense.amount
                }

                expenseAdapter.notifyDataSetChanged()
                tvTotalExpenses.text = String.format(Locale.getDefault(), "Total: $%.2f", total)
            }
    }

    private fun showExpenseOptions(expense: Expense) {
        // Diálogo simple para eliminar
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Delete '${expense.description}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteExpense(expense)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense(expense: Expense) {
        db.collection("expenses").document(expense.id)
            .delete()
            .addOnSuccessListener {
                android.util.Log.d("AddExpenseActivity", "Expense deleted successfully")

                // Actualizar balance del usuario (sumar el monto de vuelta)
                updateUserBalance(expense.userId, -expense.amount)

                // Mostrar mensaje de éxito
                showSuccessDialog("Expense deleted successfully!")

                // Recargar gastos
                loadUserExpenses()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AddExpenseActivity", "Error deleting expense: ${e.message}")
                showErrorDialog("Error deleting expense: ${e.message}")
            }
    }

    // Sobrecarga para mostrar diferentes mensajes de éxito
    private fun showSuccessDialog(message: String) {
        try {
            runOnUiThread {
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expense_confirmation, null)

                val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
                val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvSubtitle)
                val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

                tvTitle?.text = "Success!"
                tvSubtitle?.text = message

                val dialog = MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()

                btnOk?.setOnClickListener {
                    dialog.dismiss()
                }

                try {
                    dialog.show()
                } catch (e: Exception) {
                    android.util.Log.e("AddExpenseActivity", "Error showing success dialog: ${e.message}")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddExpenseActivity", "Error in showSuccessDialog(String): ${e.message}")
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}