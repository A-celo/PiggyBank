package com.example.piggybank.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.piggybank.R
import com.example.piggybank.model.Expense
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class InsightsActivity : AppCompatActivity() {

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Toggles
    private lateinit var tvWeekly: TextView
    private lateinit var tvMonthly: TextView
    private lateinit var tvYearly: TextView

    // UI
    private lateinit var chart: LineChart
    private lateinit var tvTopCategory: TextView
    private lateinit var tvSmartTip: TextView
    private lateinit var btnBack: ImageView

    // Navigation (Ahora están en el XML principal)
    private lateinit var menuHome: LinearLayout
    private lateinit var menuAdd: LinearLayout
    private lateinit var menuInsights: LinearLayout
    private lateinit var menuGoals: LinearLayout
    private lateinit var menuProfile: LinearLayout

    // Data
    private var allExpenses = listOf<Expense>()
    private var currentPeriod = "weekly"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insights)

        // ===== Bind views =====
        initViews()

        // ===== Setup listeners =====
        setupToggleListeners()
        setupNavigationListeners()

        // Default state
        selectToggle(tvWeekly, "weekly")

        // Load expenses from Firebase
        loadExpensesFromFirebase()
    }

    private fun initViews() {
        // Toggles
        tvWeekly = findViewById(R.id.tvWeekly)
        tvMonthly = findViewById(R.id.tvMonthly)
        tvYearly = findViewById(R.id.tvYearly)

        // Chart and info
        chart = findViewById(R.id.lineChart)
        tvTopCategory = findViewById(R.id.tvTopCategoryValue)
        tvSmartTip = findViewById(R.id.tvSmartTipMessage)
        btnBack = findViewById(R.id.btnBack)

        // Bottom navigation
        menuHome = findViewById(R.id.menuHome)
        menuAdd = findViewById(R.id.menuAdd)
        menuInsights = findViewById(R.id.menuInsights)
        menuGoals = findViewById(R.id.menuGoals)
        menuProfile = findViewById(R.id.menuProfile)
    }

    private fun setupToggleListeners() {
        tvWeekly.setOnClickListener {
            selectToggle(tvWeekly, "weekly")
            loadData()
        }

        tvMonthly.setOnClickListener {
            selectToggle(tvMonthly, "monthly")
            loadData()
        }

        tvYearly.setOnClickListener {
            selectToggle(tvYearly, "yearly")
            loadData()
        }
    }

    private fun setupNavigationListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        menuHome.setOnClickListener {
            Log.d("InsightsActivity", "Home clicked")
            finish() // Volver a Home
        }

        menuAdd.setOnClickListener {
            Log.d("InsightsActivity", "Add clicked")
            // Descomenta cuando tengas AddExpenseActivity
            // startActivity(Intent(this, AddExpenseActivity::class.java))
            Toast.makeText(this, "Add expense - Coming soon", Toast.LENGTH_SHORT).show()
        }

        menuInsights.setOnClickListener {
            Log.d("InsightsActivity", "Insights clicked (already here)")
            Toast.makeText(this, "You're already in Insights", Toast.LENGTH_SHORT).show()
        }

        menuGoals.setOnClickListener {
            Log.d("InsightsActivity", "Goals clicked")
            // Descomenta cuando tengas GoalsActivity
            // startActivity(Intent(this, GoalsActivity::class.java))
            Toast.makeText(this, "Goals - Coming soon", Toast.LENGTH_SHORT).show()
        }

        menuProfile.setOnClickListener {
            Log.d("InsightsActivity", "Profile clicked")
            // Descomenta cuando tengas ProfileActivity
            // startActivity(Intent(this, ProfileActivity::class.java))
            Toast.makeText(this, "Profile - Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectToggle(selected: TextView, period: String) {
        listOf(tvWeekly, tvMonthly, tvYearly).forEach {
            it.setBackgroundResource(R.drawable.toggle_unselected)
            it.setTextColor(Color.parseColor("#6E6E6E"))
        }
        selected.setBackgroundResource(R.drawable.toggle_selected)
        selected.setTextColor(Color.WHITE)
        currentPeriod = period
    }

    private fun loadExpensesFromFirebase() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("InsightsActivity", "User not logged in")
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            // Mostrar datos vacíos
            loadData()
            return
        }

        Log.d("InsightsActivity", "Loading expenses for user: $userId")

        db.collection("expenses")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("InsightsActivity", "Found ${documents.size()} expenses")

                allExpenses = documents.mapNotNull { doc ->
                    try {
                        Expense(
                            id = doc.id,
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = doc.getString("category") ?: "",
                            createdAt = doc.getTimestamp("createdAt"),
                            currency = doc.getString("currency") ?: "EUR",
                            date = doc.getTimestamp("date"),
                            description = doc.getString("description") ?: "",
                            notes = doc.getString("notes") ?: "",
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            userId = doc.getString("userId") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("InsightsActivity", "Error parsing expense: ${e.message}")
                        null
                    }
                }

                Log.d("InsightsActivity", "Parsed ${allExpenses.size} expenses successfully")
                loadData()
            }
            .addOnFailureListener { e ->
                Log.e("InsightsActivity", "Error loading expenses: ${e.message}")
                Toast.makeText(this, "Error loading expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                // Mostrar datos vacíos
                loadData()
            }
    }

    private fun loadData() {
        when (currentPeriod) {
            "weekly" -> loadWeekly()
            "monthly" -> loadMonthly()
            "yearly" -> loadYearly()
        }
    }

    private fun loadWeekly() {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Ajustar para que lunes sea el primer día
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - 2
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val weekStart = calendar.timeInMillis
        val weekExpenses = allExpenses.filter { it.getDateInMillis() >= weekStart }

        Log.d("InsightsActivity", "Weekly expenses: ${weekExpenses.size}")

        // Agrupar por día de la semana
        val dailyTotals = mutableMapOf<Int, Double>()
        weekExpenses.forEach { expense ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = expense.getDateInMillis()
            val dayIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            dailyTotals[dayIndex] = (dailyTotals[dayIndex] ?: 0.0) + expense.amount
        }

        val entries = (0..6).map { day ->
            Entry(day.toFloat(), dailyTotals[day]?.toFloat() ?: 0f)
        }

        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        setupChart(entries, labels)

        updateTopCategory(weekExpenses)
        updateSmartTip(weekExpenses)
    }

    private fun loadMonthly() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val monthStart = calendar.timeInMillis
        val monthExpenses = allExpenses.filter { it.getDateInMillis() >= monthStart }

        Log.d("InsightsActivity", "Monthly expenses: ${monthExpenses.size}")

        // Agrupar por semana
        val weeklyTotals = mutableMapOf<Int, Double>()
        monthExpenses.forEach { expense ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = expense.getDateInMillis()
            val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH) - 1
            weeklyTotals[weekOfMonth] = (weeklyTotals[weekOfMonth] ?: 0.0) + expense.amount
        }

        val maxWeeks = 5
        val entries = (0 until maxWeeks).map { week ->
            Entry(week.toFloat(), weeklyTotals[week]?.toFloat() ?: 0f)
        }

        val labels = listOf("W1", "W2", "W3", "W4", "W5")
        setupChart(entries, labels)

        updateTopCategory(monthExpenses)
        updateSmartTip(monthExpenses)
    }

    private fun loadYearly() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val yearStart = calendar.timeInMillis
        val yearExpenses = allExpenses.filter { it.getDateInMillis() >= yearStart }

        Log.d("InsightsActivity", "Yearly expenses: ${yearExpenses.size}")

        // Agrupar por mes
        val monthlyTotals = mutableMapOf<Int, Double>()
        yearExpenses.forEach { expense ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = expense.getDateInMillis()
            val month = cal.get(Calendar.MONTH)
            monthlyTotals[month] = (monthlyTotals[month] ?: 0.0) + expense.amount
        }

        val entries = (0..11).map { month ->
            Entry(month.toFloat(), monthlyTotals[month]?.toFloat() ?: 0f)
        }

        val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        setupChart(entries, labels)

        updateTopCategory(yearExpenses)
        updateSmartTip(yearExpenses)
    }

    private fun updateTopCategory(expenses: List<Expense>) {
        if (expenses.isEmpty()) {
            tvTopCategory.text = "No data"
            Log.d("InsightsActivity", "No expenses to show top category")
            return
        }

        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val topCategory = categoryTotals.maxByOrNull { it.value }?.key ?: "No data"
        tvTopCategory.text = topCategory

        Log.d("InsightsActivity", "Top category: $topCategory")
    }

    private fun updateSmartTip(expenses: List<Expense>) {
        if (expenses.isEmpty()) {
            tvSmartTip.text = "Start tracking expenses to get smart tips!"
            return
        }

        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val topCategory = categoryTotals.maxByOrNull { it.value }?.key ?: ""
        val totalSpent = expenses.sumOf { it.amount }
        val topCategoryAmount = categoryTotals[topCategory] ?: 0.0
        val percentage = ((topCategoryAmount / totalSpent) * 100).toInt()

        val tip = when (topCategory.lowercase()) {
            "transport", "transportation" -> "You'll save $percentage% if you take public transport"
            "food", "food & dining", "dining" -> "You can save more by cooking at home"
            "shopping" -> "Try waiting 24 hours before making purchases"
            "entertainment" -> "Look for free entertainment options in your area"
            "education" -> "Plan education expenses ahead"
            else -> "Your top spending is on $topCategory ($percentage% of total)"
        }

        tvSmartTip.text = tip
        Log.d("InsightsActivity", "Smart tip: $tip")
    }

    private fun setupChart(entries: List<Entry>, labels: List<String>) {
        val dataSet = LineDataSet(entries, "").apply {
            color = Color.parseColor("#8B7CF6")
            setCircleColor(Color.parseColor("#8B7CF6"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(false)
        }

        chart.data = LineData(dataSet)

        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(false)

        chart.axisLeft.apply {
            axisMinimum = 0f
            setDrawGridLines(true)
            gridColor = Color.parseColor("#E0E0E0")
            textColor = Color.parseColor("#666666")
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            granularity = 1f
            textColor = Color.parseColor("#666666")
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in labels.indices) {
                        labels[index]
                    } else {
                        ""
                    }
                }
            }
        }

        chart.invalidate()
        Log.d("InsightsActivity", "Chart updated with ${entries.size} data points")
    }
}