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
import java.text.SimpleDateFormat
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

    // Navigation
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

        initViews()
        setupToggleListeners()
        setupNavigationListeners()

        selectToggle(tvWeekly, "weekly")
        loadExpensesFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        setupNavigationListeners()
    }

    private fun initViews() {
        tvWeekly = findViewById(R.id.tvWeekly)
        tvMonthly = findViewById(R.id.tvMonthly)
        tvYearly = findViewById(R.id.tvYearly)

        chart = findViewById(R.id.lineChart)
        tvTopCategory = findViewById(R.id.tvTopCategoryValue)
        tvSmartTip = findViewById(R.id.tvSmartTipMessage)
        btnBack = findViewById(R.id.btnBack)

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
            finish()
        }

        menuAdd.setOnClickListener {
            Log.d("InsightsActivity", "Add clicked")
            Toast.makeText(this, "Add clicked", Toast.LENGTH_SHORT).show()
            //if trouble comment this line
            startActivity(Intent(this, AddExpenseActivity::class.java))


        }

        menuInsights.setOnClickListener {
            Log.d("InsightsActivity", "Insights clicked (already here)")
            Toast.makeText(this, "Already in Insights", Toast.LENGTH_SHORT).show()
        }

        menuGoals.setOnClickListener {
            Log.d("InsightsActivity", "Goals clicked")
            Toast.makeText(this, "Goals - Coming soon", Toast.LENGTH_SHORT).show()
        }

        menuProfile.setOnClickListener {
            Log.d("InsightsActivity", "Profile clicked")
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
                        val expense = Expense(
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

                        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(expense.getDateInMillis()))
                        Log.d("InsightsActivity", "Expense: ${expense.amount} EUR - ${expense.category} - Date: $dateStr")

                        expense
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
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val weekStart = calendar.timeInMillis


        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val weekEnd = calendar.timeInMillis

        val weekExpenses = allExpenses.filter { expense ->
            val expenseDate = expense.getDateInMillis()
            expenseDate >= weekStart && expenseDate < weekEnd
        }

        Log.d("InsightsActivity", "Week range: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(weekStart))} to ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(weekEnd))}")
        Log.d("InsightsActivity", "Weekly expenses: ${weekExpenses.size}")

        calendar.add(Calendar.DAY_OF_YEAR, -7)

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
            Log.d("InsightsActivity", "Day $dayIndex: ${expense.amount}")
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
        calendar.set(Calendar.MILLISECOND, 0)

        val monthStart = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis

        val monthExpenses = allExpenses.filter { expense ->
            val expenseDate = expense.getDateInMillis()
            expenseDate >= monthStart && expenseDate < monthEnd
        }

        Log.d("InsightsActivity", "Month range: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(monthStart))} to ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(monthEnd))}")
        Log.d("InsightsActivity", "Monthly expenses: ${monthExpenses.size}")

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
        calendar.set(Calendar.MILLISECOND, 0)

        val yearStart = calendar.timeInMillis

        calendar.add(Calendar.YEAR, 1)
        val yearEnd = calendar.timeInMillis

        val yearExpenses = allExpenses.filter { expense ->
            val expenseDate = expense.getDateInMillis()
            expenseDate >= yearStart && expenseDate < yearEnd
        }

        Log.d("InsightsActivity", "Year range: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(yearStart))} to ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(yearEnd))}")
        Log.d("InsightsActivity", "Yearly expenses: ${yearExpenses.size}")

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
            "entertainment" -> "Look for free entertainment options"
            "education" -> "Plan education expenses ahead"
            else -> "Top spending: $topCategory ($percentage% of total)"
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
                    return if (index in labels.indices) labels[index] else ""
                }
            }
        }

        chart.invalidate()
        Log.d("InsightsActivity", "Chart updated with ${entries.size} data points")
    }
}