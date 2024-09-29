package com.register_renegades.totem.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*

interface NetworkEventListener {
    fun onRequestSaveFile(name: String, size: UInt): Boolean
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

fun bytesToUInt(bytes: ByteArray): UInt {
    require(bytes.size == 4) { "Byte array must have exactly 4 elements" }
    return ((bytes[0].toUInt() and 0xFFu) shl 24) or
            ((bytes[1].toUInt() and 0xFFu) shl 16) or
            ((bytes[2].toUInt() and 0xFFu) shl 8) or
            (bytes[3].toUInt() and 0xFFu)
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

                        // Must have 1 byte for type, 4 byte for size and 1+ bytes for name
                        if (packetSize < 6) { return }

                        val fileSize = bytesToUInt(bytes.sliceArray(1 until 5))
                        val fileName = String(bytes.sliceArray(5 until packetSize))

                        val shouldAccept = delegate?.onRequestSaveFile(fileName, fileSize) ?: false

                        sendChannel.writeByte(if (shouldAccept) 1 else 0)
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

    suspend fun initiateFileSave(targets: List<NetworkAddress>, name: String, contents: ByteArray) {

        val sockets = MutableList<Socket?>(targets.size) { _ -> null }

        try {
            for (i in targets.indices) {
                sockets[i] = aSocket(selectorManager).tcp().connect(targets[i].ip, targets[i].port)
                println("Connected to ${targets[i].ip}:${targets[i].port}")
            }
        } catch (e: Exception) {
            println("Failed to connect to targets")
        }

        val fileSize = contents.size

        var targetsAccepted = true
        for (i in sockets.indices) {

            if (sockets[i] == null) {
                println("Socket was null")
                targetsAccepted = false
                continue
            }

            val sendChannel = sockets[i]?.openWriteChannel(autoFlush = true)
            val readChannel = sockets[i]?.openReadChannel()

            val bytes: ByteArray = ByteArray(1 + 4 + name.length)
            bytes[0] = getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_SAVE_FILE)
            bytes[1] = (fileSize shr 24).toByte()
            bytes[2] = (fileSize shr 16).toByte()
            bytes[3] = (fileSize shr 8).toByte()
            bytes[4] = fileSize.toByte()

            for (j in name.indices) {
                bytes[j + 5] = name[j].code.toByte()
            }

            sendChannel?.writeFully(bytes)

            println("Sent bytes to ${targets[i].ip}:${targets[i].port}")

            val responseFirstByte = readChannel?.readByte()
            println("Response: $responseFirstByte")

            if (responseFirstByte?.toInt() == 0) {
                println("User denied file save request")
                targetsAccepted = false
                return
            }
        }

        // Split the file
        // Send the file
    }

    fun initiateFileLoad(name: String) {

    }
}