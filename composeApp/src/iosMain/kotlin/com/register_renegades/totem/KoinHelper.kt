package com.register_renegades.totem

import com.register_renegades.totem.network.Interface
import com.register_renegades.totem.network.InterfaceManager
import com.register_renegades.totem.network.SystemInterfaceManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(systemInterfaceManager: SystemInterfaceManager) {
    startKoin {
        module {
            single<InterfaceManager> { InterfaceManager(systemInterfaceManager) }
        }
    }
}