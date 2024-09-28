package com.register_renegades.totem.com.register_renegades.totem.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

interface DatabaseDriverFactory {
    interface DatabaseDriverFactory {
        fun createDriver(): SqlDriver
    }
}