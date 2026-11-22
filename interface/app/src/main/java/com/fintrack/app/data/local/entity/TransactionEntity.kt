package com.fintrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fintrack.app.domain.model.TransactionType
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val description: String,
    // Money is stored as an integer count of minor units, never as Float/Double.
    val amountMinorUnits: Long,
    val type: TransactionType,
    val categoryId: String?,
    val date: LocalDate
)
