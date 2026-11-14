package com.codka.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One setting as a key and a text value. [Settings] gives them names and types. */
@Entity(tableName = "settings")
data class SettingEntity(@PrimaryKey val key: String, val value: String)
