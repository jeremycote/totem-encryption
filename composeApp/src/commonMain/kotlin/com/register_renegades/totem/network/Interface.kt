package com.register_renegades.totem.network

class Interface(val ip_address: ByteArray, val subnet_mask: ByteArray) {
    init {
        require(ip_address.size == 4 && subnet_mask.size == 4) {
            "IP address and subnet mask must be 4 bytes long"
        }
    }
}

interface SystemInterfaceManager {
    fun getIP(): String
    fun getMask(): String
}

expect class InterfaceManager {
    fun getInterface(): Interface
}