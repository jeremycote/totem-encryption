package com.register_renegades.totem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.register_renegades.totem.disk.GalleryManager
import com.register_renegades.totem.entity.File
import com.register_renegades.totem.network.SocketUDPListener
import com.register_renegades.totem.network.UDPPacketSender
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.register_renegades.totem.disk.rememberGalleryManager
import kotlinx.coroutines.withContext

fun sendPacket() {
    CoroutineScope(Dispatchers.IO).launch {
        val packetSender = UDPPacketSender()
        val data = "Hello, UDP!".toByteArray()

//        packetSender.sendPacket(data, targetAddress = "255.255.255.255", targetPort = 5000)
        packetSender.broadcastPacket(data, targetPort = 5000)
    }
}

@Composable
@Preview
fun OtherApp() {
    MaterialTheme {
        val listener = SocketUDPListener(port = 5000)
        val dummyList: List<File> = listOf(File(1, "Dank meme"), File(1,"Homework"))
        val coroutineScope = rememberCoroutineScope()
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var imageBytes:ByteArray
        val galleryManager = rememberGalleryManager {
            coroutineScope.launch {
                val bitmap = withContext(Dispatchers.Default) {
                    it?.toImageBitmap()
                }
                imageBitmap = bitmap
                imageBytes = copyPixelsToBuffer()
            }
        }
        Column {
            Text(text="Totem Crypto")//, textAlign = Center)
            PlusButton(galleryManager)
            Text(text = "Encrypted Files")
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp)
            ) {
                items(dummyList) { file ->
                    FileItem(file)
                }
            }
//        LazyColumn{
//            items(dummyList){file ->
//
//            }
//        }
        }
    }

}
@Composable
fun FileItem(file: File){
    Button(onClick = {}){
        Text(file.name)
    }
}
@Composable
fun PlusButton(galleryManager:GalleryManager){
    Button(onClick = {galleryManager.launch()},
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Blue)){
        Text(text="Encrypt file")
    }
}
//@Composable
//fun()
//override fun onPermissionStatus(
//    permissionType: PermissionType,
//    status: PermissionStatus
//) {
//    when (status) {
//        PermissionStatus.GRANTED -> {
//            when (permissionType) {
//                PermissionType.CAMERA -> launchCamera = true
//                PermissionType.GALLERY -> launchGallery = true
//            }
//        }
//
//        else -> {
//            permissionRationalDialog = true
//        }
//    }
//}