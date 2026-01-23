package com.example.piggybank.ui.home

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.piggybank.R
import com.example.piggybank.model.Goal
import com.google.firebase.firestore.FirebaseFirestore

class GoalAdapter(
    private val goalsList: MutableList<Goal>,
    private val onGoalDeleted: () -> Unit  // <-- AGREGAR ESTE PARÁMETRO
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalName: TextView = itemView.findViewById(R.id.tvGoalName)
        val tvGoalProgress: TextView = itemView.findViewById(R.id.tvGoalProgress)
        val progressBarGoal: ProgressBar = itemView.findViewById(R.id.progressBarGoal)
        val btnDeleteGoal: ImageView = itemView.findViewById(R.id.btnDeleteGoal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goalsList[position]

        holder.tvGoalName.text = goal.name
        holder.tvGoalProgress.text = "$${goal.savedAmount} / $${goal.targetAmount}"
        holder.progressBarGoal.progress =
            if (goal.targetAmount != 0.0) ((goal.savedAmount / goal.targetAmount) * 100).toInt()
            else 0

        holder.btnDeleteGoal.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete this goal?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteGoalFromFirebase(goal, position, context)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun deleteGoalFromFirebase(goal: Goal, position: Int, context: android.content.Context) {
        val db = FirebaseFirestore.getInstance()
        db.collection("goals")
            .document(goal.id ?: "")
            .delete()
            .addOnSuccessListener {
                goalsList.removeAt(position)
                notifyItemRemoved(position)
                Toast.makeText(context, "Goal deleted successfully", Toast.LENGTH_SHORT).show()
                onGoalDeleted()  // <-- LLAMAR AL CALLBACK PARA RECARGAR
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int = goalsList.size
}


