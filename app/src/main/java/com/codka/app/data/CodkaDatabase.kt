package com.codka.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Codka's database on the phone (task 14). Nothing in it ever leaves the phone. */
@Database(entities = [PaymentEntity::class, SettingEntity::class], version = 1, exportSchema = true)
abstract class CodkaDatabase : RoomDatabase() {
    abstract fun payments(): PaymentDao

    abstract fun settings(): SettingsDao

    companion object {
        private const val FILE = "codka.db"

        @Volatile private var instance: CodkaDatabase? = null

        fun get(context: Context): CodkaDatabase = instance ?: synchronized(this) {
            instance ?: Room
                .databaseBuilder(context.applicationContext, CodkaDatabase::class.java, FILE)
                .build()
                .also { instance = it }
        }
    }
}
