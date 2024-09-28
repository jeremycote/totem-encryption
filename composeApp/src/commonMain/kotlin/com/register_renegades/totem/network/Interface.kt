package com.register_renegades.totem.network

class Interface(val ipAddress: String, val subnetMask: String)

interface SystemInterfaceManager {
    fun getIP(): String
    fun getMask(): String
}

expect class InterfaceManager(system: SystemInterfaceManager) {
    fun getInterface(): Interface
}