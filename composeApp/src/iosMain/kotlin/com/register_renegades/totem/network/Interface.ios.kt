package com.register_renegades.totem.network

actual class InterfaceManager actual constructor(private val system: SystemInterfaceManager) {
    actual fun getInterface(): Interface {
        val ip = system.getIP()
        val mask = system.getMask()
        println("Kotlin IP: $ip, Mask: $mask")
        return Interface(ip, mask)
    }
}