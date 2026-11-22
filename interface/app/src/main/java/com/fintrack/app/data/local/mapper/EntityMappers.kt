package com.fintrack.app.data.local.mapper

import com.fintrack.app.data.local.entity.BudgetEntity
import com.fintrack.app.data.local.entity.CategoryEntity
import com.fintrack.app.data.local.entity.TransactionEntity
import com.fintrack.app.domain.model.Budget
import com.fintrack.app.domain.model.Category
import com.fintrack.app.domain.model.Transaction

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    description = description,
    amountMinorUnits = amountMinorUnits,
    type = type,
    categoryId = categoryId,
    date = date
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    description = description,
    amountMinorUnits = amountMinorUnits,
    type = type,
    categoryId = categoryId,
    date = date
)

fun CategoryEntity.toDomain() = Category(id = id, name = name, type = type)

fun Category.toEntity() = CategoryEntity(id = id, name = name, type = type)

fun BudgetEntity.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    month = month,
    limitMinorUnits = limitMinorUnits
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    month = month,
    limitMinorUnits = limitMinorUnits
)
