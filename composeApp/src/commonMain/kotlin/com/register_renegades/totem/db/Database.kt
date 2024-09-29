package com.register_renegades.totem.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_CREATE
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import androidx.sqlite.use
import kotlinx.coroutines.delay

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
//            database.execSQL("DROP TABLE User;")

            database.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS User (
                        ip INTEGER PRIMARY KEY
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

    fun getAllFileNames(): List<String> {
        val files = mutableListOf<String>()

        try {
            val database = openDatabase()

            database.prepare("SELECT File.name FROM File").use { statement ->
                while (statement.step()) {
                    val name = statement.getText(0)
                    files.add(name)
                }
            }
        } catch (E: Exception) {
            println("Database Error: ${E.message}")
        }

        return files
    }

    // Method to insert a file with its content
    suspend fun insertFileWithContent(fileName: String, content: ByteArray, shardIndex: Int, userIps: List<String>) {
        var numTries = 0
        while (numTries < 5) {
            try {
                val database = openDatabase()

                // Insert the file name first to get its ID
                database.prepare("""
                INSERT INTO File (name, fragment_data, fragment_id) VALUES (?, ?, ?);
            """.trimIndent()).use { statement ->
                    statement.bindText(1, fileName)
                    statement.bindBlob(2, content)
                    statement.bindInt(3, shardIndex)
                    statement.step()
                }

                val fileId = database.prepare("SELECT last_insert_rowid();").use { statement ->
                    statement.step()
                    statement.getLong(0)
                }

                for (ip in userIps) {
                    // Insert user ips into the db
                    database.prepare("""
                        INSERT OR IGNORE INTO User (ip) VALUES (?);
                    """.trimIndent()).use { statement ->
                        statement.bindInt(1, convertIPToInt(ip))
                        statement.step()
                    }

                    database.prepare("""
                        INSERT INTO FileUsers (user_id, file_id) VALUES (?, ?);
                    """.trimIndent()).use { statement ->
                            statement.bindInt(1, convertIPToInt(ip))
                            statement.bindLong(2, fileId)
                            statement.step()
                        }
                }

                database.close()
                return
            } catch (E: Exception) {
                println("Database Error: ${E.message}")
                numTries++
                delay(10)
            }
        }
    }

    // Accessor method to retrieve a file by ID
    fun getFileByName(name: String): File? {
        try {
            val database = openDatabase()

            var file = File(0, "", ByteArray(0), 0, listOf())
            val users = MutableList<String>(4) { _ -> ""}

            // Retrieve the file name and content
            database.prepare("""
                SELECT 
                    f.id AS file_id,
                    f.name AS file_name,
                    f.fragment_data AS fragment_data,
                    f.fragment_id AS fragment_id,
                    u.ip AS user_ip
                FROM 
                    File f
                JOIN 
                    FileUsers fu ON f.id = fu.file_id
                JOIN 
                    User u ON fu.user_id = u.ip
                WHERE 
                    f.name = ?;
            """.trimIndent()).use { statement ->
                statement.bindText(1, name)
                while (statement.step()) {
                    val id = statement.getInt(0)
                    val name = statement.getText(1)
                    val content = statement.getBlob(2)
                    val fragmentId = statement.getInt(3)
                    val ip = statement.getInt(4)

                    users.add(convertIntToIP(ip))
                    file = File(id, name, content, fragmentId, users)
                }
            }
            database.close()
            return file
        } catch (E: Exception) {
            println("Database Error: ${E.message}")
        }
        return null
    }
}