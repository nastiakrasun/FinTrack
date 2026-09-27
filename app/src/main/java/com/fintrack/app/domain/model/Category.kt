package com.fintrack.app.domain.model

enum class CategoryType {
    INCOME,
    EXPENSE
}

data class Category(
    val id: String,
    val name: String,
    val type: CategoryType
)
