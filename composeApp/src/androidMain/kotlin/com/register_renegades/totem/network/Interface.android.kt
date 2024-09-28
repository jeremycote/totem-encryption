package com.register_renegades.totem.network

actual class InterfaceManager actual constructor(private val system: SystemInterfaceManager) {
    actual fun getInterface(): Interface {
        return Interface(system.getIP(), system.getMask())
    }
}