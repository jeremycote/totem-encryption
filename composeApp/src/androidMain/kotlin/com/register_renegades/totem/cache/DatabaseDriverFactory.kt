package com.register_renegades.totem.cache

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.register_renegades.totem.cache.TotemDatabase

interface DatabaseDriverFactory {
    class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
        fun createDriver(): SqlDriver {
            return AndroidSqliteDriver(TotemDatabase.Schema, context, "launch.db")
        }
    }
}