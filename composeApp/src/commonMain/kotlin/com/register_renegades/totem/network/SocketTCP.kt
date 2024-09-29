package com.register_renegades.totem.network

import com.register_renegades.totem.Services
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*

val shardSize = 512

interface NetworkEventListener {
    fun onRequestSaveFile(name: String, size: Int): Boolean
    fun onRequestLoadFile(name: String): Boolean
}

enum class NetworkRequestType {
    REQUEST_SAVE_FILE,
    REQUEST_LOAD_FILE
}

fun getNetworkRequestTypeAsByte(requestType: NetworkRequestType): Byte {
    return when (requestType) {
        NetworkRequestType.REQUEST_SAVE_FILE -> 0x01
        NetworkRequestType.REQUEST_LOAD_FILE -> 0x02
    }
}

fun getNetworkRequestTypeFromByte(byte: Byte): NetworkRequestType {
    return when (byte) {
        getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_SAVE_FILE) -> NetworkRequestType.REQUEST_SAVE_FILE
        getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_LOAD_FILE) -> NetworkRequestType.REQUEST_LOAD_FILE
        else -> throw IllegalArgumentException("Invalid byte value: $byte")
    }
}

fun bytesToInt(bytes: ByteArray): Int {
    var result = 0

    for (i in bytes.indices) {
        result = result shl 8
        result = result or (bytes[i].toInt() and 0xFF)
    }

    return result
}

class SocketTCP() {
    private var delegate: NetworkEventListener? = null

    private val selectorManager = SelectorManager(Dispatchers.IO)

    suspend fun startListening(port: Int) {
        val serverSocket = try { aSocket(selectorManager).tcp().bind("127.0.0.1", port) } catch (e: Exception) { throw IllegalStateException("Failed to bind") }

        while (true) {
            val socket = serverSocket.accept()

            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            try {
                val bytes = ByteArray(1024)

                val firstByte = receiveChannel.readByte()
                var cursor = 0
                bytes[cursor++] = firstByte

                when (firstByte) {
                    getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_SAVE_FILE) -> {
                        println("Received save file request!")
                        val packetSize = receiveChannel.readAvailable(bytes, 1, bytes.size - 1) + 1

                        // Must have 1 byte for type, 4 byte for size, 4 bytes for shards, 1 byte for user id, 8+ bytes for user ips, and 1+ bytes for name
                        if (packetSize < 20) { return }

                        val fileSize = bytesToInt(bytes.sliceArray(cursor until cursor + 4))
                        cursor += 4

                        val numShards = bytesToInt(bytes.sliceArray(cursor until cursor + 4))
                        cursor += 4

                        val numUsers = bytesToInt(bytes.sliceArray(cursor until cursor + 1))
                        cursor++

                        val userId = bytesToInt(bytes.sliceArray(cursor until cursor + 1))
                        cursor++

                        if (packetSize < 12 + numUsers * 4) { return }

                        val userIps = mutableListOf<String>()
                        for (i in 0..<numUsers) {
                            val ipBytes = bytes.sliceArray(cursor until cursor + 4)
                            cursor += 4
                            val ip = "${ipBytes[0]}.${ipBytes[1]}.${ipBytes[2]}.${ipBytes[3]}"
                            userIps.add(ip)
                        }

                        val fileName = String(bytes.sliceArray(cursor until packetSize))

                        val shouldAccept = delegate?.onRequestSaveFile(fileName, fileSize) ?: false

                        println("Should accept: $shouldAccept")
                        sendChannel.writeByte(if (shouldAccept) 1 else 0)

//                        val fileBytes = ByteArray(fileSize / numUsers)
                        println("Receiving file bytes!")
                        val packet = receiveChannel.readRemaining((fileSize / numUsers).toLong())
                        val fileBytes = packet.readBytes()

                        println("Received file bytes!")
                        sendChannel.writeByte(1)
                        sendChannel.close()

                        println("File received! $fileName. Received num bytes: ${fileBytes.size}")
                        Services.shared.database?.insertFileWithContent(fileName, fileBytes, userId, userIps)
                    }

                    getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_LOAD_FILE) -> {
                        println("Received load file request!")
                        val packetSize = receiveChannel.readAvailable(bytes, 1, bytes.size - 1) + 1

                        // Must have 1 byte for type, 1+ for name
                        if (packetSize < 2) { return }

                        val fileName = String(bytes.sliceArray(1 until packetSize))

                        val localFile = Services.shared.database?.getFileByName(fileName)

                        if (localFile == null) {
                            return
                        }

                        // Prompt user
                        if (!(delegate?.onRequestLoadFile(fileName) ?: false)) { return }

                        sendChannel.writeFully(localFile.content)
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            } finally {
                socket.close()
            }
        }
    }

    fun setDelegate(delegate: NetworkEventListener?) { this.delegate = delegate }

    suspend fun initiateFileSave(targets: List<NetworkAddress>, name: String, contents: ByteArray): Boolean {

        val sockets = MutableList<Socket?>(targets.size) { _ -> null }

        try {
            for (i in targets.indices) {
                sockets[i] = aSocket(selectorManager).tcp().connect(targets[i].ip, targets[i].port)
                println("Connected to ${targets[i].ip}:${targets[i].port}")
            }
        } catch (e: Exception) {
            println("Failed to connect to targets")
        }

        for (socket in sockets) {
            if (socket == null) {
                println("Socket was null")
                for (socket in sockets) {
                    socket?.close()
                }
                return false
            }
        }

        val readChannels = MutableList<ByteReadChannel>(targets.size) { i -> sockets[i]!!.openReadChannel() }
        val writeChannels = MutableList<ByteWriteChannel>(targets.size) { i -> sockets[i]!!.openWriteChannel(autoFlush = true) }

        val fileSize = contents.size
        val numUsers = targets.size + 1
        val numShards = if (contents.size % shardSize == 0) (contents.size / shardSize) else (contents.size / shardSize) + 1

        val allUsers = targets + NetworkAddress(Services.shared.interfaceManager!!.getInterface().ipAddress, 0)

        var targetsAccepted = true
        for (i in sockets.indices) {

            if (sockets[i] == null) {
                println("Socket was null")
                targetsAccepted = false
                continue
            }

            val bytes: ByteArray = ByteArray(1 + 4 + 4 + 1 + 1 + 4*numUsers + name.length)
            var cursor: Int = 0

            bytes[cursor++] = getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_SAVE_FILE)
            bytes[cursor++] = (fileSize shr 24).toByte()
            bytes[cursor++] = (fileSize shr 16).toByte()
            bytes[cursor++] = (fileSize shr 8).toByte()
            bytes[cursor++] = fileSize.toByte()

            bytes[cursor++] = (numShards shr 24).toByte()
            bytes[cursor++] = (numShards shr 16).toByte()
            bytes[cursor++] = (numShards shr 8).toByte()
            bytes[cursor++] = numShards.toByte()

            // Num users
            bytes[cursor++] = numUsers.toByte()

            // user ID (For order)
            bytes[cursor++] = (i + 1).toByte()

            if (Services.shared.interfaceManager == null) {
                println("Failed to get interface manager")

                for (socket in sockets) {
                    socket?.close()
                }

                return false
            }

            for (j in 0..<numUsers) {
                val targetIp = allUsers[j].getRawIp()
                for (k in 0..<4) {
                    bytes[cursor++] = targetIp[k]
                }
            }

            for (j in name.indices) {
                bytes[cursor++] = name[j].code.toByte()
            }

            writeChannels[i].writeFully(bytes)

            println("Sent bytes to ${targets[i].ip}:${targets[i].port}")

            val responseFirstByte = readChannels[i].readByte()
            println("Response: $responseFirstByte")

            if (responseFirstByte.toInt() == 0) {
                println("User denied file save request")
                targetsAccepted = false
                continue
            }
        }

        println("Targets accepted: $targetsAccepted")

        var sendSuccessful = true
        if (targetsAccepted) {
            // Split the file
            for (i in sockets.indices) {
                // Split file into chunks
                val userId = i + 1
                var chunkIndex = 0
                while (chunkIndex * shardSize < fileSize) {
                    if (chunkIndex % numUsers == userId) {
                        try {
                            writeChannels[i].writeAvailable(contents, chunkIndex * shardSize, if (((chunkIndex+1) * shardSize) < fileSize) shardSize else fileSize - (chunkIndex * shardSize))
                        } catch (E: Exception) {
                            println("Error: ${E.message}")
                        }
                    }
                    chunkIndex++
                }

                // Close to signal end
                writeChannels[i].close()

                // TODO: This hangs if initial file is smaller then n people * shardSize
                println("Waiting for reception confirmation")
                val received = readChannels[i].readByte()

                if (received.toInt() == 0) {
                    println("Send not acknowledged")
                    sendSuccessful = false
                    continue
                }
                println("Received acknowledged")
            }
        }

        if (targetsAccepted and sendSuccessful) {
            println("Preparing sender to local database")
            val localFile = ByteArray(fileSize)
            var chunkIndex = 0
            var localIndex = 0
            while (chunkIndex * shardSize < fileSize) {
                // Local is user 0
                if (chunkIndex % numUsers == 0) {
                    try {
                        contents.copyInto(
                            localFile,
                            localIndex * shardSize,
                            chunkIndex * shardSize,
                            if (((chunkIndex + 1) * shardSize) < fileSize) ((chunkIndex + 1) * shardSize) else fileSize
                        )
                    } catch (E: Exception) {
                        println("Error: ${E.message}")
                    }
                        localIndex++
                }
                chunkIndex++
            }

            // Insert file into local database
            println("Saving sender to local database")
            Services.shared.database?.insertFileWithContent(name, localFile, 0, allUsers.map { address -> address.ip })
        }

        for (socket in sockets) {
            socket?.close()
        }

        println("File sent!")

        return targetsAccepted and sendSuccessful
    }

    suspend fun initiateFileLoad(targets: List<NetworkAddress>, name: String): ByteArray? {
        val sockets = MutableList<Socket?>(targets.size) { _ -> null }

        try {
            for (i in targets.indices) {
                sockets[i] = aSocket(selectorManager).tcp().connect(targets[i].ip, targets[i].port)
                println("Connected to ${targets[i].ip}:${targets[i].port}")
            }
        } catch (e: Exception) {
            println("Failed to connect to targets")
        }

        val readChannels = MutableList<ByteReadChannel>(targets.size) { i -> sockets[i]!!.openReadChannel() }
        val writeChannels = MutableList<ByteWriteChannel>(targets.size) { i -> sockets[i]!!.openWriteChannel(autoFlush = true) }

        val localFragment = Services.shared.database?.getFileByName(name)

        if (localFragment == null) {
            println("File not found in local database")
            for (socket in sockets) {
                socket?.close()
            }
            return null
        }

        val fileSize = localFragment.content.size * localFragment.userIps.size
        val fileBuffer = ByteArray(fileSize)

        for (i in targets.indices) {
            writeChannels[i].writeByte(getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_LOAD_FILE))
            writeChannels[i].writeStringUtf8(name)
        }

        // TODO: What if local fragment is not same size as everyone else?
        for (i in targets.indices) {
            val chunk = ByteArray(localFragment.content.size)

            if (readChannels[i].readAvailable(chunk) != chunk.size) {
                for (socket in sockets) {
                    socket?.close()
                }
                return null
            }

            var chunkIndex = 0
            while (chunkIndex * shardSize < fileSize) {
                // Local is user 0, so start at 1
                if (chunkIndex % localFragment.userIps.size == i + 1) {
                    chunk.copyInto(fileBuffer, chunkIndex * shardSize, if ((chunkIndex * (shardSize+1)) < fileSize) shardSize else fileSize - (chunkIndex * shardSize))
                }
                chunkIndex++
            }
        }

        return fileBuffer
    }
}