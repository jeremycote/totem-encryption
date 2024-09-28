package com.register_renegades.totem.com.register_renegades.totem.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

interface DatabaseDriverFactory {
    class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
        override fun createDriver(): SqlDriver {
            return AndroidSqliteDriver(AppDatabase.Schema, context, "launch.db")
        }
    }
}