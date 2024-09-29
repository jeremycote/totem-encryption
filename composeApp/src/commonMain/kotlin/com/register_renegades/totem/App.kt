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
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.register_renegades.totem.disk.GalleryManager
import com.register_renegades.totem.db.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.register_renegades.totem.disk.rememberGalleryManager
import com.register_renegades.totem.network.NetworkAddress
import com.register_renegades.totem.network.NetworkEventListener
import com.register_renegades.totem.network.SocketTCP
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.withContext

fun sendPacket() {
    CoroutineScope(Dispatchers.IO).launch {
        println("sendPacket coroutine launched")
        val sendSocket = SocketTCP()

        val target = NetworkAddress("127.0.0.1", 5004)
        val file = ByteArray(1024)
        val success = sendSocket.initiateFileSave(listOf(target), "test.txt", file)

        println("File Save Success: $success")
    }
}

class AppDelegate(private val saveFile: (String, Int) -> Boolean, private val loadFile: (String) -> Boolean):
    NetworkEventListener {
    override fun onRequestSaveFile(name: String, size: Int): Boolean {
        return saveFile(name, size)
    }

    override fun onRequestLoadFile(name: String): Boolean {
        return loadFile(name)
    }
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var imageSelected by remember { mutableStateOf<Boolean>(false) };
        var fileName by remember {mutableStateOf<String>("File")}
        val dummyList: List<File> = listOf(
            File("Meme A", "Dank meme".toByteArray(), 0),
            File("Meme B", "Dank meme".toByteArray(), 1)
        )
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

        val delegate = AppDelegate({ name, size ->
            run {
                println("Save file $name with size $size")
                return@AppDelegate true
            }
        }, { name ->
            run {
                println("Load file $name")
                return@AppDelegate true
            }
        })

        var port = 5004

        LaunchedEffect(Unit) {
            val socketListener = SocketTCP()
            socketListener.setDelegate(delegate)

            CoroutineScope(Dispatchers.IO).launch {
                socketListener.startListening(port)
            }
        }

        val galleryManager = rememberGalleryManager {
            CoroutineScope(Dispatchers.IO).launch {
                var bytes: ByteArray?
                imageBitmap = withContext(Dispatchers.Default) {
                    bytes = it?.toByteArray()
                    it?.toImageBitmap()
                }

                if(imageBitmap != null){
                    imageSelected = true
                }
            }
        }

        if(imageSelected){
            val dialog = DialogWithImage(BitmapPainter(imageBitmap!!),"",{imageSelected = false})
        }

        Column {
            Text("${Services.shared.interfaceManager?.getInterface()?.ipAddress}:$port")
            Button(onClick = { sendPacket() }) {
                Text("Send")
            }
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
//fun dialogDismiss():Unit{
//    vardialogDismissFlag = true;
//    return dialogDismissFlag
//}
@Composable
fun DialogWithImage(
    painter: Painter,
    imageDescription: String,
    dismissDialog: () -> Unit,
    //nameStoreFunc: () -> Unit
) {
    var currentText by remember { mutableStateOf("")}
    Dialog(onDismissRequest = { dismissDialog() }) {
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
                    text = "Specify an alias for your file:",
                    modifier = Modifier.padding(16.dp),
                )
                TextField(value = currentText, onValueChange = {currentText = it},label = {Text("")})
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = {dismissDialog()},
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        sendPacket()
                        storeFileName()
                        println(currentText)
                    }) {
                        Text("Send")
                    }
//                    TextButton(
//                        onClick = { onConfirmation() },
//                        modifier = Modifier.padding(8.dp),
//                    ) {
//                        Text("Confirm")
//                    }
                }
            }

        }
    }
}

fun storeFileName() {
    TODO("Not yet implemented")
}

