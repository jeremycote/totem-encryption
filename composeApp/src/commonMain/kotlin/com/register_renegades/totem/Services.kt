package com.register_renegades.totem

import com.register_renegades.totem.network.InterfaceManager

class Services private constructor() {

    var interfaceManager: InterfaceManager? = null

    companion object {
        val shared: Services by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { Services() }
        fun initShared(im: InterfaceManager) { shared.interfaceManager = im }
    }
}