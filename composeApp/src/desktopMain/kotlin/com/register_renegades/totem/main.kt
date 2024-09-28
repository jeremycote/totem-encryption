package com.register_renegades.totem

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "TotemCrypto",
    ) {
        App()
    }
}