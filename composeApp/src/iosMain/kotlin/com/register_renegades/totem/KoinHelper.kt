package com.register_renegades.totem

import com.register_renegades.totem.network.InterfaceManager
import com.register_renegades.totem.network.SystemInterfaceManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains

/**
 * Initialize Koin dependency injection
 * This function is used!
 */
fun initDependencies(systemInterfaceManager: SystemInterfaceManager) {
    println("Initializing Coin!")
    println("Kotlin IP: ${systemInterfaceManager.getIP()}, Mask: ${systemInterfaceManager.getMask()}")
    Services.initInterfaceManager(InterfaceManager(systemInterfaceManager))

    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, 1u, true)
    Services.initDatabase(paths.first() as String)
}