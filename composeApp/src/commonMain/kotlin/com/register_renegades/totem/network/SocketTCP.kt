package com.register_renegades.totem.network

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
                bytes[0] = firstByte

                when (firstByte) {
                    getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_SAVE_FILE) -> {
                        println("Received save file request!")
                        val packetSize = receiveChannel.readAvailable(bytes, 1, bytes.size - 1) + 1

                        // Must have 1 byte for type, 4 byte for size, 4 bytes for shards, 1 byte for user id, and 1+ bytes for name
                        if (packetSize < 12) { return }

                        val fileSize = bytesToInt(bytes.sliceArray(1 until 5))
                        val numShards = bytesToInt(bytes.sliceArray(5 until 9))
                        val numUsers = bytesToInt(bytes.sliceArray(9 until 10))
                        val userId = bytesToInt(bytes.sliceArray(10 until 11))
                        val fileName = String(bytes.sliceArray(11 until packetSize))

                        val shouldAccept = delegate?.onRequestSaveFile(fileName, fileSize) ?: false

                        sendChannel.writeByte(if (shouldAccept) 1 else 0)

                        val fileBytes = ByteArray(fileSize / numShards)
                        val numBytesReceived = receiveChannel.readAvailable(fileBytes, 0, fileBytes.size)

                        sendChannel.writeByte(if (numBytesReceived == fileSize / numShards) 1 else 0)
                    }

                    getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_LOAD_FILE) -> {
                        println("Received load file request!")
                        val packetSize = receiveChannel.readAvailable(bytes, 1, bytes.size - 1) + 1

                        // Must have 1 byte for type, 1+ for name
                        if (packetSize < 2) { return }

                        val fileName = String(bytes.sliceArray(1 until packetSize))
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

        val readChannels = MutableList<ByteReadChannel>(targets.size) { i -> sockets[i]!!.openReadChannel() }
        val writeChannels = MutableList<ByteWriteChannel>(targets.size) { i -> sockets[i]!!.openWriteChannel(autoFlush = true) }

        val fileSize = contents.size
        val numUsers = targets.size + 1
        val numShards = if (contents.size % shardSize == 0) (contents.size / shardSize) else (contents.size / shardSize) + 1

        var targetsAccepted = true
        for (i in sockets.indices) {

            if (sockets[i] == null) {
                println("Socket was null")
                targetsAccepted = false
                continue
            }

            val bytes: ByteArray = ByteArray(1 + 4 + 4 + 1 + 1 + name.length)
            bytes[0] = getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_SAVE_FILE)
            bytes[1] = (fileSize shr 24).toByte()
            bytes[2] = (fileSize shr 16).toByte()
            bytes[3] = (fileSize shr 8).toByte()
            bytes[4] = fileSize.toByte()

            bytes[5] = (numShards shr 24).toByte()
            bytes[6] = (numShards shr 16).toByte()
            bytes[7] = (numShards shr 8).toByte()
            bytes[8] = numShards.toByte()

            // Num users
            bytes[9] = numUsers.toByte()

            // user ID (For order)
            bytes[10] = (i + 1).toByte()

            for (j in name.indices) {
                bytes[j + 11] = name[j].code.toByte()
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

        var sendSuccessful = true
        if (targetsAccepted) {
            // Split the file
            for (i in sockets.indices) {
                // Split file into chunks
                val userId = i + 1
                var chunkIndex = 0
                while (chunkIndex * shardSize < fileSize) {
                    if (chunkIndex % numUsers == userId) {
                        writeChannels[i].writeFully(contents, chunkIndex * shardSize, if ((chunkIndex * (shardSize+1)) < fileSize) shardSize else fileSize - (chunkIndex * shardSize))
                    }
                    chunkIndex++
                }

                val received = readChannels[i].readByte()

                if (received.toInt() == 0) {
                    println("Send not acknowledged")
                    sendSuccessful = false
                    continue
                }
            }
        }

        for (socket in sockets) {
            socket?.close()
        }

        return targetsAccepted and sendSuccessful
    }

    fun initiateFileLoad(name: String) {

    }
}