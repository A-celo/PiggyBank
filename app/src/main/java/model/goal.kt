package com.example.piggybank.model

data class Goal(
    var id: String? = null,          // ID generado por Firestore
    var name: String = "",
    var category: String = "",
    var targetAmount: Double = 0.0,
    var savedAmount: Double = 0.0,  // cuánto ha guardado el usuario
    var completionDate: String = ""
)

