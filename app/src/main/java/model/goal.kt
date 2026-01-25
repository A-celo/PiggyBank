package com.example.piggybank.model

data class Goal(
    var name: String = "",
    var category: String = "",
    var targetAmount: Double = 0.0,
    var completionDate: String = "",
    var savedAmount: Double = 0.0,
    var id: String? = null
)
