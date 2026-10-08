package com.fintrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fintrack.app.data.local.dao.BudgetDao
import com.fintrack.app.data.local.dao.CategoryDao
import com.fintrack.app.data.local.dao.TransactionDao
import com.fintrack.app.data.local.entity.BudgetEntity
import com.fintrack.app.data.local.entity.CategoryEntity
import com.fintrack.app.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FinTrackDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        private const val DATABASE_NAME = "fintrack.db"

        // No destructive migration: a schema change must ship with an explicit Migration
        // so users' financial data is never silently wiped.
        fun create(context: Context): FinTrackDatabase =
            Room.databaseBuilder(context.applicationContext, FinTrackDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
