package com.register_renegades.totem.disk

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGalleryManager(onResult: (SharedImage?) -> Unit): GalleryManager {
    TODO("Not yet implemented")
}

actual class GalleryManager actual constructor(onLaunch: () -> Unit) {
    actual fun launch() {
    }
}