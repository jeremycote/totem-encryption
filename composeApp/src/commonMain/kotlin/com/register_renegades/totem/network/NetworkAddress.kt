package com.register_renegades.totem.network

class NetworkAddress (
    val ip: String,
    val port: Int
) {
    fun getRawIp(): ByteArray {
        val ipParts = ip.split(".").map { it.toInt() }

        if (ipParts.size != 4) {
            println("Invalid IP address")
            return byteArrayOf(0, 0, 0, 0)
        }

        return byteArrayOf(ipParts[0].toByte(), ipParts[1].toByte(), ipParts[2].toByte(), ipParts[3].toByte())
    }
}