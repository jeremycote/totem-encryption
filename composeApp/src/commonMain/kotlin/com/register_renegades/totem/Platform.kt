package com.register_renegades.totem

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform