package com.joaomartins.lilith

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val value: Double,
    val isRevenue: Boolean,
    val isPaid: Boolean = false,

    // Removidos: day, month, year
    // Adicionados: Estes 3 campos que o erro apontou como faltantes
    val dueDate: Long,        // Data completa em milissegundos
    val currentParcel: Int = 1, // Parcela atual (ex: 1)
    val totalParcels: Int = 1   // Total de parcelas (ex: 12)
)