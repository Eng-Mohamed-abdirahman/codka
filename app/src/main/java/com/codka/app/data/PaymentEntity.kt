package com.codka.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.codka.app.parser.Currency
import com.codka.app.parser.Provider

/**
 * One payment Codka received (task 14). Money is stored in minor units (cents), so
 * $45.25 is 4525 and SLSH 2,000 is 200000: whole numbers, never rounded.
 */
@Entity(
    tableName = "payments",
    indices = [
        // The same SMS is never saved twice (see [SmsFingerprint]).
        Index(value = ["sms_hash"], unique = true),
        Index(value = ["received_at"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val provider: Provider,
    val currency: Currency,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "sender_name") val senderName: String,
    /** The full number from the SMS (Codka reads it out digit by digit). */
    @ColumnInfo(name = "sender_phone") val senderPhone: String,
    /**
     * The balance the SMS says the seller has after this payment, exactly as written
     * ("150.0049": Zaad dollars have 4 decimals, so cents would round it). Null if not shown.
     */
    @ColumnInfo(name = "balance_after") val balanceAfter: String? = null,
    /** When the phone received the SMS, in milliseconds since 1970. */
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    /** True if the fake-SMS check (task 13) doubts this payment; [reason] says why. */
    @ColumnInfo(name = "is_suspicious") val isSuspicious: Boolean = false,
    val reason: String? = null,
    /** A fingerprint of the SMS text, never the text itself. */
    @ColumnInfo(name = "sms_hash") val smsHash: String
) {
    /** The amount as Codka speaks it ("45.25", "2000"). */
    val amount: String get() = Money.fromMinor(amountMinor)
}
