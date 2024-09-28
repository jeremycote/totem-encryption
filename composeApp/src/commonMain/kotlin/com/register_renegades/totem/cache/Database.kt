package com.register_renegades.totem.cache

internal class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = AppDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.appDatabaseQueries

    internal fun getAllFiles(){
        return dbQuery.selectAllFiles(::mapFileSelecting).executeAsLlist()
    }

    private fun mapFileSelecting(id: Int, name: String): File{
        return File(
            id = id.toInt(),
            name = name
        )
    }

    //TODO: Implement an insert file / delete file function.
}