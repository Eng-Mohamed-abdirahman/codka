package com.codka.app.data

import com.codka.app.parser.ParsedPayment
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow

/** Saving and reading payments (task 14). The SMS receiver (task 19) and the screens use this. */
class PaymentStore(private val dao: PaymentDao) {
    sealed interface SaveResult {
        data class Saved(val id: Long) : SaveResult

        /** This exact SMS was already saved: don't announce it again. */
        data object Duplicate : SaveResult

        /** The amount can't be stored exactly (for example 3 decimals): nothing was saved. */
        data object BadAmount : SaveResult
    }

    suspend fun save(
        payment: ParsedPayment,
        smsBody: String,
        receivedAt: Long,
        isSuspicious: Boolean = false,
        reason: String? = null
    ): SaveResult {
        val minor = Money.toMinor(payment.amount) ?: return SaveResult.BadAmount
        val id =
            dao.insert(
                PaymentEntity(
                    provider = payment.provider,
                    currency = payment.currency,
                    amountMinor = minor,
                    senderName = payment.senderName,
                    senderPhone = payment.senderPhone,
                    receivedAt = receivedAt,
                    isSuspicious = isSuspicious,
                    reason = reason,
                    smsHash = SmsFingerprint.of(smsBody)
                )
            )
        return if (id == -1L) SaveResult.Duplicate else SaveResult.Saved(id)
    }

    fun last(): Flow<PaymentEntity?> = dao.last()

    /** Today's payments (from midnight on the phone's clock), newest first. */
    fun today(
        now: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): Flow<List<PaymentEntity>> {
        val (from, until) = dayOf(now, zone)
        return dao.between(from, until)
    }

    companion object {
        /** The start (midnight) and end (next midnight) of the day [now] falls in. */
        fun dayOf(now: Long, zone: TimeZone): Pair<Long, Long> {
            val start =
                Calendar.getInstance(zone).apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            return start.timeInMillis to end.timeInMillis
        }
    }
}
