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

    suspend fun initiateFileSave(target: String, port: Int, name: String, size: UInt) {
        println("Initiating file save to $target:$port")

        val socket = try { aSocket(selectorManager).tcp().connect(target, port) } catch (e: Exception) {
            println("Failed to connect to $target:$port")
            return
        }
        val sendChannel = socket.openWriteChannel(autoFlush = true)
        val readChannel = socket.openReadChannel()

        val bytes: ByteArray = ByteArray(1 + 4 + name.length)
        bytes[0] = getNetworkRequestTypeAsByte(NetworkRequestType.REQUEST_SAVE_FILE)
        bytes[1] = (size shr 24).toByte()
        bytes[2] = (size shr 16).toByte()
        bytes[3] = (size shr 8).toByte()
        bytes[4] = size.toByte()

        for (i in name.indices) {
            bytes[i + 5] = name[i].code.toByte()
        }

        sendChannel.writeFully(bytes)

        println("Sent bytes to $target:$port")

        val responseFirstByte = readChannel.readByte()
        println("Response: $responseFirstByte")

        // Load the file
        // Split the file
        // Send the file

        socket.close()
    }

    fun initiateFileLoad(name: String) {

    }
}