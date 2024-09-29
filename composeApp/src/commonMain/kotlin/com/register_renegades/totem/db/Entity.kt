package com.register_renegades.totem.db

class File(val id: Int, val name: String, val content: ByteArray, val shardIndex: Int, val userIps: List<String>)

// by 1 offset to deal with 1 based indexing for db

fun convertIPToInt(ip: String): Int {
    val octets = ip.split(".").map { it.toInt() }
    return ((octets[0] shl 24) or (octets[1] shl 16) or (octets[2] shl 8) or octets[3]) + 1
}

fun convertIntToIP(ip: Int): String {
    val i = ip - 1
    return "${(i shr 24) and 0xFF}.${(i shr 16) and 0xFF}.${(i shr 8) and 0xFF}.${i and 0xFF}"
}