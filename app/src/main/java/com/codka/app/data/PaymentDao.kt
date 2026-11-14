package com.codka.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    /** Saves the payment and returns its id, or -1 if the same SMS was already saved. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: PaymentEntity): Long

    /** The newest payment, or null if there are none yet. Updates by itself when one is added. */
    @Query("SELECT * FROM payments ORDER BY received_at DESC, id DESC LIMIT 1")
    fun last(): Flow<PaymentEntity?>

    /** Payments received from [from] (included) to [until] (not included), newest first. */
    @Query(
        "SELECT * FROM payments WHERE received_at >= :from AND received_at < :until " +
            "ORDER BY received_at DESC, id DESC"
    )
    fun between(from: Long, until: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun byId(id: Long): PaymentEntity?
}
