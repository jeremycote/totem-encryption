package com.register_renegades.totem.cache

import com.register_renegades.totem.entity.File

internal class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = TotemDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.totemDatabaseQueries

    internal fun getAllFiles(): List<File> {
        return dbQuery.selectAllFiles(::mapFileSelecting).executeAsList()
    }

    private fun mapFileSelecting(id: Long, name: String?): File {
        return File(
            id = id.toInt(),
            name = name ?: "Name not found"
        )
    }

    //TODO: Implement an insert file / delete file function.
}