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

class EditGoalDialog(
    private val goal: Goal,
    private val onGoalUpdated: () -> Unit
) : DialogFragment() {

    private lateinit var etGoalName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etTargetAmount: EditText
    private lateinit var etSavedAmount: EditText
    private lateinit var etCompletionDate: EditText

    private val categories = arrayOf(
        "Food & Dining", "Transportation", "Shopping",
        "Entertainment", "Bills & Utilities",
        "Healthcare", "Education", "Personal Care", "Other"
    )

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_edit_goal, container, false)

        etGoalName = view.findViewById(R.id.etGoalName)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        etTargetAmount = view.findViewById(R.id.etTargetAmount)
        etSavedAmount = view.findViewById(R.id.etSavedAmount)
        etCompletionDate = view.findViewById(R.id.etCompletionDate)

        spinnerCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        // Pre-fill
        etGoalName.setText(goal.name)
        etTargetAmount.setText(goal.targetAmount.toString())
        etSavedAmount.setText(goal.savedAmount.toString())
        etCompletionDate.setText(goal.completionDate)

        val categoryPosition = categories.indexOf(goal.category)
        if (categoryPosition >= 0) {
            spinnerCategory.setSelection(categoryPosition)
        }

        view.findViewById<Button>(R.id.btnSave).setOnClickListener { updateGoal() }
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener { dismiss() }

        return view
    }

    private fun updateGoal() {
        val name = etGoalName.text.toString().trim()
        val targetText = etTargetAmount.text.toString().trim()
        val savedText = etSavedAmount.text.toString().trim()
        val date = etCompletionDate.text.toString().trim()

        // Validaciones
        if (name.isEmpty() || targetText.isEmpty() || savedText.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetAmount = targetText.toDoubleOrNull()
        val savedAmount = savedText.toDoubleOrNull()

        if (targetAmount == null || savedAmount == null) {
            Toast.makeText(requireContext(), "Enter valid numbers", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Guardar referencia al contexto ANTES de cerrar
        val appContext = requireContext().applicationContext

        // ✅ Cerrar el diálogo INMEDIATAMENTE
        dismiss()

        // ✅ DESPUÉS actualizar en Firebase
        FirebaseFirestore.getInstance()
            .collection("goals")
            .document(goal.id!!)
            .update(
                mapOf(
                    "name" to name,
                    "category" to spinnerCategory.selectedItem.toString(),
                    "targetAmount" to targetAmount,
                    "savedAmount" to savedAmount,
                    "completionDate" to date
                )
            )
            .addOnSuccessListener {
                Toast.makeText(appContext, "Goal updated!", Toast.LENGTH_SHORT).show()
                onGoalUpdated()  // Notifica a la activity
            }
            .addOnFailureListener { e ->
                Toast.makeText(appContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}