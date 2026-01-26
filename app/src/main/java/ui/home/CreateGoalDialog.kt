package com.example.piggybank.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.piggybank.R
import com.example.piggybank.model.Goal
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CreateGoalDialog : DialogFragment() {

    private lateinit var etGoalName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etTargetAmount: EditText
    private lateinit var etCompletionDate: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    var onGoalSaved: (() -> Unit)? = null
    private var selectedDate: Date = Date()

    private val categories = arrayOf(
        "Food & Dining", "Transportation", "Shopping",
        "Entertainment", "Bills & Utilities",
        "Healthcare", "Education", "Personal Care", "Other"
    )

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()  // 90% ancho
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_create_goal, container, false)

        // Inicializar vistas
        etGoalName = view.findViewById(R.id.etGoalName)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        etTargetAmount = view.findViewById(R.id.etTargetAmount)
        etCompletionDate = view.findViewById(R.id.etCompletionDate)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        // Spinner
        spinnerCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        // Listeners
        etCompletionDate.setOnClickListener { showDatePicker() }
        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener { saveGoal() }

        return view
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        cal.time = selectedDate
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = cal.time
                etCompletionDate.setText(
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate)
                )
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveGoal() {
        val name = etGoalName.text.toString().trim()
        val targetText = etTargetAmount.text.toString().trim()
        val date = etCompletionDate.text.toString().trim()

        if (name.isEmpty() || targetText.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetAmount = targetText.toDoubleOrNull()
        if (targetAmount == null) {
            Toast.makeText(requireContext(), "Enter a valid number for Target Amount", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Guardar referencia al contexto ANTES de cerrar
        val appContext = requireContext().applicationContext

        // ✅ Cerrar el diálogo INMEDIATAMENTE
        dismiss()

        // ✅ DESPUÉS guardar en Firebase
        val goal = Goal(
            name = name,
            category = spinnerCategory.selectedItem.toString(),
            targetAmount = targetAmount,
            savedAmount = 0.0,
            completionDate = date
        )

        FirebaseFirestore.getInstance()
            .collection("goals")
            .add(goal)
            .addOnSuccessListener {
                Toast.makeText(appContext, "Goal saved!", Toast.LENGTH_SHORT).show()
                onGoalSaved?.invoke()
            }
            .addOnFailureListener { e ->
                Toast.makeText(appContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
