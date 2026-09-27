package com.fintrack.app.domain.model

import java.time.YearMonth

data class Budget(
    val id: String,
    val categoryId: String,
    val month: YearMonth,
    val limitMinorUnits: Long
)
