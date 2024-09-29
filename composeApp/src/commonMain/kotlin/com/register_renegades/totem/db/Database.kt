package com.register_renegades.totem.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import androidx.sqlite.use

class Database(private val documentsDirectory: String) {

    private fun openDatabase(): SQLiteConnection {
        val databaseFileName = "totem.db"
        val databaseFilePath = "$documentsDirectory/$databaseFileName"

        return BundledSQLiteDriver().open(databaseFilePath, SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE)
    }

    init {
        println("Initializing database...")

        try {
            val database = openDatabase()

//            database.execSQL("DROP TABLE FileUsers;")
//            database.execSQL("DROP TABLE File;")
//            database.execSQL("DROP TABLE Users;")

            database.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS User (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT
                    );
                    """)

            database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS File (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT,
                        fragment_data BLOB,
                        fragment_id INTEGER
                    );
                    """)

            database.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS FileUsers (
                        user_id INTEGER,
                        file_id INTEGER,
                        user_ip TEXT,
                        FOREIGN KEY (user_id) REFERENCES User(id),
                        FOREIGN KEY (file_id) REFERENCES File(id),
                        PRIMARY KEY (file_id, user_id)
                    );
                    """)



            database.close()
        } catch (E: Exception) {
            println("Database Error: ${E.message}")
        }
    }

    fun getAllFiles(): List<File> {
        val files = mutableListOf<File>()

        try {
            val database = openDatabase()

            database.prepare("SELECT * FROM File").use { statement ->
                while (statement.step()) {
                    val name = statement.getText(1)
                    val content = statement.getBlob(2)
                    val shardIndex = statement.getInt(3)

                    files.add(File(name, content,shardIndex))
                }
            }
        } catch (E: Exception) {
            println("Database Error: ${E.message}")
        }

        return files
    }

    // Method to insert a file with its content
    fun insertFileWithContent(fileName: String, content: ByteArray, shardIndex: Int) {
        var numTries = 0
        while (numTries < 5) {
            try {
                val database = openDatabase()

                // Insert the file name first to get its ID
                database.prepare("""
                INSERT INTO File (name, fragment_data, fragment_id) VALUES (?, ?, ?)
            """.trimIndent()).use { statement ->
                    statement.bindText(1, fileName)
                    statement.bindBlob(2, content)
                    statement.bindInt(3, shardIndex)
                    statement.step()
                }
//
//            val fileId = database.prepare("SELECT last_insert_rowid()").use { statement ->
//                statement.step()
//                statement.getLong(0)
//            }
//
//            // Insert the content as a fragment
//            database.prepare("""
//                INSERT INTO Fragments (file_id, fragment_data, fragment_id) VALUES (?, ?, ?)
//            """.trimIndent()).use { statement ->
//                statement.bindLong(1, fileId)
//                statement.bindBlob(2, content)
//                statement.bindLong(3, shardIndex.toLong())
//                statement.step()
//            }

                database.close()
                return
            } catch (E: Exception) {
                println("Database Error: ${E.message}")
            }
        }
    }

    // Accessor method to retrieve a file by ID
    fun getFileByName(name: String): File? {
        try {
            val database = openDatabase()

            // Retrieve the file name and content
            database.prepare("""
                SELECT f.name, fr.fragment_data, fr.fragment_id
                FROM File f 
                WHERE f.name = ?
            """.trimIndent()).use { statement ->
                statement.bindText(1, name)
                if (statement.step()) {
                    val name = statement.getText(0)
                    val content = statement.getBlob(1)
                    val fragmentId = statement.getInt(2)

                    return File(name, content, fragmentId)
                }
            }
            database.close()
        } catch (E: Exception) {
            println("Database Error: ${E.message}")
        }
        return null
    }
}