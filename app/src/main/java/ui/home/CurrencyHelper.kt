package com.example.piggybank.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

object CurrencyHelper {

    // Currency symbols map
    private val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "PLN" to "zł",
        "GBP" to "£"
    )

    // Get user's selected currency from SharedPreferences
    fun getUserCurrency(context: Context): String {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currency = prefs.getString("currency", "USD") ?: "USD"

        android.util.Log.d("CurrencyHelper", "Retrieved currency from prefs: $currency")

        return currency
    }

    // Get currency symbol for a currency code
    fun getCurrencySymbol(currencyCode: String): String {
        return currencySymbols[currencyCode] ?: "$"
    }

    // Format amount with user's currency
    fun formatAmount(context: Context, amount: Double): String {
        val currency = getUserCurrency(context)
        val symbol = getCurrencySymbol(currency)

        val formattedAmount = String.format(Locale.getDefault(), "%.2f", amount)
        return "$symbol$formattedAmount"
    }
}