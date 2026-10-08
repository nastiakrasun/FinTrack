package com.fintrack.app.data.local

import androidx.room.TypeConverter
import com.fintrack.app.domain.model.CategoryType
import com.fintrack.app.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth

// Stable text representations: ISO-8601 dates sort chronologically, enums are stored by name.
class Converters {
    @TypeConverter
    fun localDateToString(value: LocalDate): String = value.toString()

    @TypeConverter
    fun stringToLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun yearMonthToString(value: YearMonth): String = value.toString()

    @TypeConverter
    fun stringToYearMonth(value: String): YearMonth = YearMonth.parse(value)

    @TypeConverter
    fun transactionTypeToString(value: TransactionType): String = value.name

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun categoryTypeToString(value: CategoryType): String = value.name

    @TypeConverter
    fun stringToCategoryType(value: String): CategoryType = CategoryType.valueOf(value)
}
