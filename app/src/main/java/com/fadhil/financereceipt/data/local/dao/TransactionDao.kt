package com.fadhil.financereceipt.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Query
import com.fadhil.financereceipt.data.local.entity.TransactionEntity
import com.fadhil.financereceipt.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE transaction_id = :id")
    suspend fun deleteById(id: Long): Int

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
    @Query(
        """
        SELECT t.*, c.category_name AS categoryName, c.emoji AS categoryEmoji
        FROM transactions AS t
        INNER JOIN categories AS c ON c.category_id = t.category_id
        ORDER BY t.transaction_date DESC, t.transaction_id DESC
        """
    )
    fun observeWithCategory(): Flow<List<TransactionWithCategory>>
}