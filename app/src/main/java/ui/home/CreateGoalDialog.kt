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

    // Lambda opcional para avisar a la Activity que se guardó un goal
    var onGoalSaved: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_create_goal, container, false)

        etGoalName = view.findViewById(R.id.etGoalName)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        etTargetAmount = view.findViewById(R.id.etTargetAmount)
        etCompletionDate = view.findViewById(R.id.etCompletionDate)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        etCompletionDate.setOnClickListener { showDatePicker() }

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener { dismiss() }
        view.findViewById<Button>(R.id.btnSave).setOnClickListener { saveGoal() }

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

    private fun saveGoal() {
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

        val goal = Goal(
            name = name,
            category = category,
            targetAmount = targetDouble,
            completionDate = date
        )

        val db = FirebaseFirestore.getInstance()
        db.collection("goals")
            .add(goal)
            .addOnSuccessListener { docRef ->
                // Guardar el ID generado
                goal.id = docRef.id
                Toast.makeText(requireContext(), "Goal saved successfully!", Toast.LENGTH_SHORT).show()
                onGoalSaved?.invoke() // Avisar a la Activity
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error saving goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}