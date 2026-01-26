package com.example.piggybank.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piggybank.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.example.piggybank.model.Goal

class GoalsActivity : AppCompatActivity() {

    private lateinit var rvGoals: RecyclerView
    private lateinit var btnCreateGoal: MaterialButton
    private lateinit var btnBack: ImageView // Add this line
    private val goalsList = mutableListOf<Goal>()
    private lateinit var goalAdapter: GoalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_goals)

        // PRIMERO: Inicializar TODAS las vistas
        rvGoals = findViewById(R.id.rvGoals)
        btnCreateGoal = findViewById(R.id.btnCreateGoal)
        btnBack = findViewById(R.id.btnBack) // Initialize back button

        // SEGUNDO: Configurar el adapter
        goalAdapter = GoalAdapter(
            goalsList,
            onGoalUpdated = { listenGoalsFromFirebase() },
            onGoalDeleted = { listenGoalsFromFirebase() }
        )
        rvGoals.adapter = goalAdapter
        rvGoals.layoutManager = LinearLayoutManager(this)


        // TERCERO: Configurar listeners
        btnCreateGoal.setOnClickListener {
            val dialog = CreateGoalDialog() // <-- Siempre vacío
            dialog.onGoalSaved = { listenGoalsFromFirebase() }
            dialog.show(supportFragmentManager, "CreateGoalDialog")
        }

        // Add back button listener
        btnBack.setOnClickListener {
            navigateToHome()
        }

        // CUARTO: Configurar window insets
        val content = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.contentContainer)
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // QUINTO: Configurar el menú (DESPUÉS de inicializar las vistas)
        BottomMenuHelper.setupMenu(this, "goals")

        // SEXTO: Cargar datos
        listenGoalsFromFirebase()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // Clear the back stack so user can't go back to Goals with back button
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Finish current activity so it's removed from stack
        overridePendingTransition(0, 0)
    }

    private fun listenGoalsFromFirebase() {
        val db = FirebaseFirestore.getInstance()

        db.collection("goals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading goals", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    goalsList.clear()
                    for (doc in snapshot.documents) {
                        val goal = doc.toObject(Goal::class.java)
                        goal?.id = doc.id
                        if (goal != null) goalsList.add(goal)
                    }
                    goalAdapter.notifyDataSetChanged()
                }
            }
    }

    // Optional: Override device back button to also go to Home
    override fun onBackPressed() {
        navigateToHome()
    }
}