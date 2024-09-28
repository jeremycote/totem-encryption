package com.register_renegades.totem

import com.register_renegades.totem.network.InterfaceManager
import com.register_renegades.totem.network.SystemInterfaceManager
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Initialize Koin dependency injection
 * This function is used!
 */
fun initDependencies(systemInterfaceManager: SystemInterfaceManager) {
    println("Initializing Coin!")
    println("Kotlin IP: ${systemInterfaceManager.getIP()}, Mask: ${systemInterfaceManager.getMask()}")
    Services.initShared(InterfaceManager(systemInterfaceManager))
}