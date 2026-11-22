package com.fintrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fintrack.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    // REPLACE gives insert-or-update semantics for a budget with the same id.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Query("SELECT * FROM budgets ORDER BY month ASC, rowid ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY month ASC, rowid ASC")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun count(): Int
}
