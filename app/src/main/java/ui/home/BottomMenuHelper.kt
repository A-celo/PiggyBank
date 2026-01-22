package com.example.piggybank.ui.home

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.piggybank.R

object BottomMenuHelper {

    fun setupMenu(activity: Activity, active: String) {
        val menuHome = activity.findViewById<LinearLayout>(R.id.menuHome)
        val menuAdd = activity.findViewById<LinearLayout>(R.id.menuAdd)
        val menuInsights = activity.findViewById<LinearLayout>(R.id.menuInsights)
        val menuGoals = activity.findViewById<LinearLayout>(R.id.menuGoals)
        val menuProfile = activity.findViewById<LinearLayout>(R.id.menuProfile)

        // Colores
        val activeColor = activity.getColor(R.color.activeMenuColor)
        val inactiveColor = activity.getColor(R.color.inactiveMenuColor)

        // Función para resetear todos los items
        fun resetAll() {
            listOf(
                R.id.iconHome to R.id.textHome,
                R.id.iconAdd to R.id.textAdd,
                R.id.iconInsights to R.id.textInsights,
                R.id.iconGoals to R.id.textGoals,
                R.id.iconProfile to R.id.textProfile
            ).forEach { (iconId, textId) ->
                activity.findViewById<ImageView>(iconId)?.setColorFilter(inactiveColor)
                activity.findViewById<TextView>(textId)?.setTextColor(inactiveColor)
            }
        }

        // Reset todos
        resetAll()

        // Activar el item actual
        when (active) {
            "home" -> {
                activity.findViewById<ImageView>(R.id.iconHome)?.setColorFilter(activeColor)
                activity.findViewById<TextView>(R.id.textHome)?.setTextColor(activeColor)
            }
            "add" -> {
                activity.findViewById<ImageView>(R.id.iconAdd)?.setColorFilter(activeColor)
                activity.findViewById<TextView>(R.id.textAdd)?.setTextColor(activeColor)
            }
            "insights" -> {
                activity.findViewById<ImageView>(R.id.iconInsights)?.setColorFilter(activeColor)
                activity.findViewById<TextView>(R.id.textInsights)?.setTextColor(activeColor)
            }
            "goals" -> {
                activity.findViewById<ImageView>(R.id.iconGoals)?.setColorFilter(activeColor)
                activity.findViewById<TextView>(R.id.textGoals)?.setTextColor(activeColor)
            }
            "profile" -> {
                activity.findViewById<ImageView>(R.id.iconProfile)?.setColorFilter(activeColor)
                activity.findViewById<TextView>(R.id.textProfile)?.setTextColor(activeColor)
            }
        }

        // Listeners para navegación
        menuHome?.setOnClickListener {
            if (activity !is HomeActivity) {
                val intent = Intent(activity, HomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        menuAdd?.setOnClickListener {
            if (activity !is AddExpenseActivity) {
                val intent = Intent(activity, AddExpenseActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        menuInsights?.setOnClickListener {
            if (activity !is InsightsActivity) {
                val intent = Intent(activity, InsightsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        menuGoals?.setOnClickListener {
            if (activity !is GoalsActivity) {
                val intent = Intent(activity, GoalsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        menuProfile?.setOnClickListener {
            if (activity !is ProfileActivity) {
                val intent = Intent(activity, ProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
            }
        }
    }
}
