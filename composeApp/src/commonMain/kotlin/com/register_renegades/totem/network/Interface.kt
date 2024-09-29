package com.register_renegades.totem.network

class Interface(val ipAddress: String, val subnetMask: String) {
    fun getBroadcastAddress(): String {
        val ipParts = ipAddress.split(".").map { it.toUInt().toUByte() }
        val maskParts = subnetMask.split(".").map { it.toUInt().toUByte() }

        val broadcastAddress = ipParts.zip(maskParts) { ipPart, maskPart ->
            ipPart or (maskPart.inv())
        }

        return broadcastAddress.joinToString(".")
    }
}

interface SystemInterfaceManager {
    fun getIP(): String
    fun getMask(): String
}

class InterfaceManager(private val system: SystemInterfaceManager) {
    fun getInterface(): Interface {
        return Interface(system.getIP(), system.getMask())
    }
}