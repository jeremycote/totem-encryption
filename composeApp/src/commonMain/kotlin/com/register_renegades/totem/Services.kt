package com.register_renegades.totem

import com.register_renegades.totem.db.Database
import com.register_renegades.totem.network.InterfaceManager

class Services private constructor() {

    var interfaceManager: InterfaceManager? = null
    var database: Database? = null

    companion object {
        val shared: Services by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Services() }

        fun initInterfaceManager(im: InterfaceManager) {
            shared.interfaceManager = im
        }

        fun initDatabase(documentsDirectory: String) {
            shared.database = Database(documentsDirectory)
        }
    }
}