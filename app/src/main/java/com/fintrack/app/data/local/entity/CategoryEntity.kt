package com.fintrack.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fintrack.app.domain.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: CategoryType
)
