package com.register_renegades.totem.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class TotemSocket {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val serverSocket = aSocket(selectorManager).udp().bind(InetSocketAddress("0.0.0.0", 12345))
}