package com.register_renegades.totem.network

import com.register_renegades.totem.Services
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class UDPPacketSender {
    private val interfaceManager = Services.shared.interfaceManager!!
    private val selectorManager = SelectorManager(Dispatchers.IO)

    suspend fun sendPacket(data: ByteArray, targetAddress: String, targetPort: Int) {
        val socket = aSocket(selectorManager).udp().bind()

        val packet = ByteReadPacket(data)
        val datagram = Datagram(packet, InetSocketAddress(targetAddress, targetPort))

        try {
            socket.send(datagram)
            println("Packet sent to $targetAddress:$targetPort")
        } catch (e: Exception) {
            println("Error sending packet: ${e.message}")
        } finally {
            socket.close()
        }

    }

    suspend fun broadcastPacket(data: ByteArray, targetPort: Int) {

        println("Broadcasting packet!")
        println(interfaceManager.getInterface().ipAddress)
        println(interfaceManager.getInterface().subnetMask)

        val socket = aSocket(selectorManager).udp().bind()
        println(socket.localAddress)
        socket.close()
    }
}