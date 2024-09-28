package com.register_renegades.totem.network

import io.ktor.utils.io.core.toByteArray

actual class InterfaceManager(private val system: SystemInterfaceManager) {
    actual fun getInterface(): Interface {
        val ip = system.getIP()
        val mask = system.getMask()
        println("IP: $ip, Mask: $mask")
        return Interface(ip.toByteArray(), mask.toByteArray())
    }
}