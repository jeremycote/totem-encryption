package com.register_renegades.totem.network

class AndroidSystemInterfaceManager: SystemInterfaceManager {
    override fun getIP(): String {
        return "0.0.0.0"
    }

    override fun getMask(): String {
        return "255.255.255.0"
    }
}