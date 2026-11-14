package com.codka.app.data

import com.codka.app.parser.Currency
import com.codka.app.parser.Provider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Codka's settings, with names and types (task 14). Stored in the `settings` table. */
class Settings(private val dao: SettingsDao) {
    /**
     * The last balance an SMS showed for this account, exactly as written ("150.0049").
     * The fake-SMS check (task 13) compares new payments with it.
     */
    suspend fun lastBalance(provider: Provider, currency: Currency): String? =
        dao.get(balanceKey(provider, currency))

    suspend fun setLastBalance(provider: Provider, currency: Currency, balance: String) =
        dao.put(SettingEntity(balanceKey(provider, currency), balance))

    /** How loud Codka speaks, 0–100 % of the phone's media volume. Full by default. */
    suspend fun volume(): Int = dao.get(VOLUME)?.toIntOrNull() ?: DEFAULT_VOLUME

    suspend fun setVolume(percent: Int) =
        dao.put(SettingEntity(VOLUME, percent.coerceIn(0, 100).toString()))

    /** True once a helper has finished the setup steps (task 23). */
    suspend fun setupDone(): Boolean = dao.get(SETUP_DONE).toBoolean()

    fun watchSetupDone(): Flow<Boolean> = dao.watch(SETUP_DONE).map { it.toBoolean() }

    suspend fun setSetupDone(done: Boolean) = dao.put(SettingEntity(SETUP_DONE, done.toString()))

    private companion object {
        const val VOLUME = "volume"
        const val SETUP_DONE = "setup_done"
        const val DEFAULT_VOLUME = 100

        // Zaad and eDahab each have a dollar and a shilling account, with their own balance.
        fun balanceKey(provider: Provider, currency: Currency) =
            "balance_${provider.name}_${currency.name}"
    }
}
