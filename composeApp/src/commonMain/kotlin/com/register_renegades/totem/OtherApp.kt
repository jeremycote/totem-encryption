package com.register_renegades.totem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import kotlin.reflect.typeOf

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
        var imageSelected = false;
        val listener = SocketUDPListener(port = 5000)
        val dummyList: List<File> = listOf(File(1, "Dank meme"), File(1,"Homework"))
        val coroutineScope = rememberCoroutineScope()
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        val galleryManager = rememberGalleryManager {
            coroutineScope.launch {
                var bytes: ByteArray?
                val bitmap = withContext(Dispatchers.Default) {
                    bytes = it?.toByteArray()
                    it?.toImageBitmap()
                }
                imageBitmap = bitmap
                if(imageBitmap != null){
                    imageSelected = true
                }
            }
        }
        //val filenameManager{}'
        if(imageSelected){
            DialogWithImage(onDismissRequest = {}, { sendImage() }, BitmapPainter(imageBitmap!!),"")
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
fun sendImage():Unit{

}
@Composable
fun DialogWithImage(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    painter: Painter,
    imageDescription: String,
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painter,
                    contentDescription = imageDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(160.dp)
                )
                Text(
                    text = "This is a dialog with buttons and an image.",
                    modifier = Modifier.padding(16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = { onConfirmation() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
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