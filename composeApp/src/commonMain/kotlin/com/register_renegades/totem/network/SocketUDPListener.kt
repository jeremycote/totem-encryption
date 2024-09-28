package com.register_renegades.totem.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.String
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class SocketUDPListener(private val port: Int) {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private lateinit var socket: BoundDatagramSocket

    suspend fun startListening() {
        socket = aSocket(selectorManager).udp().bind(InetSocketAddress("::", port));
        println("Listening for UDP packets on port $port")

        while (true) {
            val datagram = socket.receive()
            val data = datagram.packet.readBytes()
            val sender = datagram.address

            println("Received message from $sender: ${String(data)}")
        }
    }

    fun stopListening() {
        socket.close()
        selectorManager.close()
    }
}