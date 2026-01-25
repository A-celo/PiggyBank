package com.example.piggybank.ui.home

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.piggybank.R
import com.example.piggybank.model.Goal
import com.google.firebase.firestore.FirebaseFirestore

class GoalAdapter(
    private val goalsList: MutableList<Goal>,
    private val onGoalDeleted: () -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalName: TextView = itemView.findViewById(R.id.tvGoalName)
        val tvGoalProgress: TextView = itemView.findViewById(R.id.tvGoalProgress)
        val progressBarGoal: ProgressBar = itemView.findViewById(R.id.progressBarGoal)
        val btnDeleteGoal: ImageView = itemView.findViewById(R.id.btnDeleteGoal)
        val btnEditGoal: ImageView = itemView.findViewById(R.id.btnEditGoal)
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
            if(goal.targetAmount != 0.0) ((goal.savedAmount / goal.targetAmount) * 100).toInt()
            else 0

        // DELETE
        holder.btnDeleteGoal.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Delete Goal")
                .setMessage("Are you sure you want to delete this goal?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteGoal(goal, position, holder.itemView)
                }
                .setNegativeButton("No", null)
                .show()
        }

        // EDIT
        holder.btnEditGoal.setOnClickListener {
            val dialog = CreateGoalDialog()
            dialog.goalToEdit = goal
            dialog.onGoalSaved = { notifyItemChanged(position) }
            dialog.show((holder.itemView.context as AppCompatActivity).supportFragmentManager, "EditGoalDialog")
        }
    }

    private fun deleteGoal(goal: Goal, position: Int, view: View) {
        val db = FirebaseFirestore.getInstance()
        db.collection("goals").document(goal.id ?: "").delete()
            .addOnSuccessListener {
                goalsList.removeAt(position)
                notifyItemRemoved(position)
                Toast.makeText(view.context,"Goal deleted!",Toast.LENGTH_SHORT).show()
                onGoalDeleted()
            }
            .addOnFailureListener { e ->
                Toast.makeText(view.context,"Error: ${e.message}",Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int = goalsList.size
}
