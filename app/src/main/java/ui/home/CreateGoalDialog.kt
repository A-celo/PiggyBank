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
    private lateinit var etSavedAmount: EditText
    private lateinit var etCompletionDate: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var savedAmountLayout: View  // Para ocultar/mostrar el saved amount

    private val categories = arrayOf(
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

    private var selectedDate: Date = Date()

    var onGoalSaved: (() -> Unit)? = null
    var goalToEdit: Goal? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_create_goal, container, false)

        etGoalName = view.findViewById(R.id.etGoalName)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        etTargetAmount = view.findViewById(R.id.etTargetAmount)
        etSavedAmount = view.findViewById(R.id.etSavedAmount)
        etCompletionDate = view.findViewById(R.id.etCompletionDate)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        // Asume que tienes un layout contenedor para el saved amount
        // Si no lo tienes, usa etSavedAmount directamente
        savedAmountLayout = etSavedAmount // o usa etSavedAmount

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Configurar según el modo (crear o editar)
        goalToEdit?.let { goal ->
            // MODO EDICIÓN - Mostrar saved amount
            savedAmountLayout.visibility = View.VISIBLE
            etGoalName.setText(goal.name)
            etTargetAmount.setText(goal.targetAmount.toString())
            etSavedAmount.setText(goal.savedAmount.toString())
            etCompletionDate.setText(goal.completionDate)

            val categoryPosition = categories.indexOf(goal.category)
            if (categoryPosition >= 0) {
                spinnerCategory.setSelection(categoryPosition)
            }

            btnSave.text = "Update"
        } ?: run {
            // MODO CREACIÓN - Ocultar saved amount
            savedAmountLayout.visibility = View.GONE
        }

        etCompletionDate.setOnClickListener { showDatePicker() }

        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener { saveGoalImmediateDismiss() }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val params: WindowManager.LayoutParams = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, day)
                selectedDate = newCalendar.time
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etCompletionDate.setText(sdf.format(selectedDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveGoalImmediateDismiss() {
        val name = etGoalName.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val target = etTargetAmount.text.toString().trim()
        val date = etCompletionDate.text.toString().trim()

        if (name.isBlank() || target.isBlank() || date.isBlank()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetDouble = try {
            target.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Enter a valid number for Target Amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Guardar referencia al contexto ANTES de cerrar el diálogo
        val appContext = requireContext().applicationContext

        // Cerrar el diálogo de inmediato
        dismiss()

        if (goalToEdit != null) {
            // MODO EDICIÓN - Validar y usar saved amount
            val saved = etSavedAmount.text.toString().trim()

            if (saved.isBlank()) {
                Toast.makeText(appContext, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return
            }

            val savedDouble = try {
                saved.toDouble()
            } catch (e: NumberFormatException) {
                Toast.makeText(appContext, "Enter a valid number for Saved Amount", Toast.LENGTH_SHORT).show()
                return
            }

            val updatedGoal = mapOf(
                "name" to name,
                "category" to category,
                "targetAmount" to targetDouble,
                "savedAmount" to savedDouble,
                "completionDate" to date
            )

            FirebaseFirestore.getInstance().collection("goals")
                .document(goalToEdit!!.id ?: "")
                .update(updatedGoal)
                .addOnSuccessListener {
                    Toast.makeText(appContext, "Goal updated successfully!", Toast.LENGTH_SHORT).show()
                    onGoalSaved?.invoke()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(appContext, "Error updating goal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // MODO CREACIÓN - No usar saved amount, usar 0.0 por defecto
            val goal = Goal(
                name = name,
                category = category,
                targetAmount = targetDouble,
                savedAmount = 0.0,  // Valor por defecto
                completionDate = date
            )

            FirebaseFirestore.getInstance().collection("goals")
                .add(goal)
                .addOnSuccessListener { docRef ->
                    goal.id = docRef.id
                    Toast.makeText(appContext, "Goal saved successfully!", Toast.LENGTH_SHORT).show()
                    onGoalSaved?.invoke()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(appContext, "Error saving goal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}