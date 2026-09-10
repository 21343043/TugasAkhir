package com.fadhil.financereceipt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fadhil.financereceipt.data.local.entity.FinancialPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialPlanDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(plan: FinancialPlanEntity): Long

    @Update
    suspend fun update(plan: FinancialPlanEntity): Int

    @Query("DELETE FROM financial_plans WHERE plan_id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM financial_plans WHERE plan_id = :id LIMIT 1")
    suspend fun getById(id: Long): FinancialPlanEntity?

    @Query(
        """
        SELECT * FROM financial_plans
        WHERE plan_year = :year AND plan_month = :month
        ORDER BY plan_id DESC
        """
    )
    fun observeByMonth(year: Int, month: Int): Flow<List<FinancialPlanEntity>>

    @Query(
        """
        SELECT * FROM financial_plans
        WHERE category_id = :categoryId AND plan_year = :year AND plan_month = :month
        LIMIT 1
        """
    )
    suspend fun getByCategoryAndMonth(
        categoryId: Long,
        year: Int,
        month: Int
    ): FinancialPlanEntity?
}
