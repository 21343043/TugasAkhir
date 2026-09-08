package com.fadhil.financereceipt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query(
        """
        SELECT * FROM transactions
        ORDER BY transaction_date DESC, transaction_id DESC
        """
    )
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE transaction_id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: Long): TransactionEntity?
}