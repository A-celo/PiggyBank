package com.example.piggybank.model

import com.google.firebase.Timestamp

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val createdAt: Timestamp? = null,
    val currency: String = "EUR",
    val date: Timestamp? = null,
    val description: String = "",
    val notes: String = "",
    val paymentMethod: String = "",
    val userId: String = ""
) {
    fun getDateInMillis(): Long {
        return date?.toDate()?.time ?: 0L
    }
}