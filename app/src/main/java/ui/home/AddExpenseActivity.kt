package com.example.piggybank.ui.home

import android.app.DatePickerDialog
import android.content.Context
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

// ==================== CURRENCY HELPER ====================
object CurrencyHelper {

    // Currency symbols map
    private val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "PLN" to "zł",
        "GBP" to "£"
    )

    // Get user's selected currency from SharedPreferences
    fun getUserCurrency(context: Context): String {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currency = prefs.getString("currency", "USD") ?: "USD"

        // Debug logging
        android.util.Log.d("CurrencyHelper", "Retrieved currency from prefs: $currency")
        android.util.Log.d("CurrencyHelper", "All prefs: ${prefs.all}")

        return currency
    }

    // Get currency symbol for a currency code
    fun getCurrencySymbol(currencyCode: String): String {
        val symbol = currencySymbols[currencyCode] ?: "$"
        android.util.Log.d("CurrencyHelper", "Symbol for $currencyCode: $symbol")
        return symbol
    }

    // Format amount with user's currency
    fun formatAmount(context: Context, amount: Double): String {
        val currency = getUserCurrency(context)
        val symbol = getCurrencySymbol(currency)

        // Use appropriate decimal formatting based on currency
        val decimalFormat = when (currency) {
            "EUR", "USD", "GBP" -> "%.2f"
            "PLN" -> "%.2f"
            else -> "%.2f"
        }

        val formattedAmount = String.format(Locale.getDefault(), decimalFormat, amount)
        val result = "$symbol$formattedAmount"

        android.util.Log.d("CurrencyHelper", "Formatted $amount as: $result (currency: $currency)")
        return result
    }
}

// ==================== CLASE MODELO (dentro del mismo archivo) ====================
data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: Date = Date(),
    val userId: String = "",
    val paymentMethod: String = "",
    val notes: String = "",
    val currency: String = "USD"
)

// ==================== CLASE ADAPTER (dentro del mismo archivo) ====================
class ExpenseAdapter(
    private val expenses: List<Expense>,
    private val onItemClick: (Expense) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvExpenseDescription: TextView = itemView.findViewById(R.id.tvExpenseDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(expense: Expense) {
            // Format amount with user's currency
            val formattedAmount = CurrencyHelper.formatAmount(context, expense.amount)
            val categoryLower = expense.category.lowercase()

            // Use localized string
            tvExpenseDescription.text = context.getString(
                R.string.expense_description_format,
                formattedAmount,
                categoryLower
            )

            // Format date as dd/MM/yyyy
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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

        // Inicializar vistas
        rvExpenses = findViewById(R.id.rvExpenses)
        btnAddExpenseOverlay = findViewById(R.id.btnAddExpense)

        // Buscar o crear tvTotalExpenses
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        if (tvTotalExpenses == null) {
            tvTotalExpenses = TextView(this)
        }

        // Configurar RecyclerView
        expensesList = mutableListOf()
        expenseAdapter = ExpenseAdapter(expensesList, { expense ->
            showExpenseOptions(expense)
        }, this)

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
                    tvAmount.text = CurrencyHelper.formatAmount(this@AddExpenseActivity, amount)
                } else {
                    tvAmount.text = CurrencyHelper.formatAmount(this@AddExpenseActivity, 0.0)
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

        // Get user's selected currency
        val userCurrency = CurrencyHelper.getUserCurrency(this)

        // Crear objeto Expense con paymentMethod y currency
        val expense = hashMapOf(
            "userId" to user.uid,
            "amount" to amount,
            "description" to note,
            "category" to category,
            "paymentMethod" to paymentMethod,
            "date" to selectedDate,
            "createdAt" to FieldValue.serverTimestamp(),
            "currency" to userCurrency,
            "notes" to note
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
            runOnUiThread {
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expense_confirmation, null)

                val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
                val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvSubtitle)
                val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

                tvTitle?.text = "Your expense was added successfully!"
                tvSubtitle?.text = "Keep saving! You almost met your goal"

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
                    Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddExpenseActivity", "Error in showSuccessDialog: ${e.message}")
            Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showErrorDialog(message: String? = null) {
        try {
            runOnUiThread {
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_expense_error, null)

                val tvTitle = dialogView.findViewById<TextView>(R.id.there_was_a)
                val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvErrorMessage)
                val btnOk = dialogView.findViewById<Button>(R.id.btnOkError)

                tvTitle?.text = "There was a mistake!"
                tvSubtitle?.text = message ?: "Try to add your expense again to keep saving!"

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
                    android.util.Log.e("AddExpenseActivity", "Error showing error dialog: ${e.message}")
                    Toast.makeText(this, message ?: "An error occurred", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddExpenseActivity", "Error in showErrorDialog: ${e.message}")
            Toast.makeText(this, message ?: "An error occurred", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUserBalance(userId: String, amount: Double) {
        db.collection("users").document(userId)
            .update("currentBalance", FieldValue.increment(-amount))
            .addOnFailureListener { e ->
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
                    android.util.Log.e("AddExpenseActivity", "Error loading expenses: ${error.message}")
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
                        userId = document.getString("userId") ?: "",
                        paymentMethod = document.getString("paymentMethod") ?: "",
                        notes = document.getString("notes") ?: document.getString("description") ?: "",
                        currency = document.getString("currency") ?: "USD"
                    )
                    expensesList.add(expense)
                    total += expense.amount
                }

                expenseAdapter.notifyDataSetChanged()

                // Format total with user's currency
                val formattedTotal = CurrencyHelper.formatAmount(this, total)
                tvTotalExpenses.text = "Total: $formattedTotal"
            }
    }

    private fun showExpenseOptions(expense: Expense) {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.dialog_expense_details,
            null
        )

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Views
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val tvNotes = dialogView.findViewById<TextView>(R.id.tvNotes)
        val tvPayment = dialogView.findViewById<TextView>(R.id.tvPayment)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        // Format date for display
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(expense.date)

        // Format amount for display WITH USER'S CURRENCY
        val formattedAmount = CurrencyHelper.formatAmount(this, expense.amount)

        // Populate data
        tvDescription?.text = "You spent $formattedAmount on ${expense.category} on $formattedDate"
        tvNotes?.text = expense.notes.ifEmpty { expense.description.ifEmpty { "—" } }
        tvPayment?.text = expense.paymentMethod.ifEmpty { "Not specified" }

        // Actions
        btnOk?.setOnClickListener {
            dialog.dismiss()
        }

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete?.setOnClickListener {
            dialog.dismiss()
            deleteExpense(expense)
        }

        dialog.show()
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