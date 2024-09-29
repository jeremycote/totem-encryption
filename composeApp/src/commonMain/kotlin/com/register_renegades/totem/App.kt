package com.register_renegades.totem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.register_renegades.totem.disk.rememberGalleryManager
import com.register_renegades.totem.network.NetworkAddress
import com.register_renegades.totem.network.NetworkEventListener
import com.register_renegades.totem.network.SocketTCP
import kotlinx.coroutines.withContext

fun sendFile(name: String, targets: List<String>, bytes: ByteArray, onFileSent: (Boolean) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val sendSocket = SocketTCP()

        val targets = targets.map { target -> NetworkAddress(target, 5004)}
        val success = sendSocket.initiateFileSave(targets, name, bytes)

        println("Send file: $success")

        onFileSent(success)
    }
}

fun requestFile(name: String, targets: List<String>, onFileReceived: (ByteArray?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val sendSocket = SocketTCP()

        val targets = targets.map { target -> NetworkAddress(target, 5004)}
        val file = sendSocket.initiateFileLoad(targets, "test.txt")

        if (file != null) {
            println("Successfully loaded $name")
        }

        onFileReceived(file)
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
        var imageSelected by remember { mutableStateOf(false) };
        var fileName by remember {mutableStateOf("File")}
        val files by remember { mutableStateOf(Services.shared.database?.getAllFileNames() ?: listOf()) }

        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
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
                imageBitmap = withContext(Dispatchers.Default) {
                    imageBytes = it?.toByteArray()
                    it?.toImageBitmap()
                }

                if(imageBitmap != null){
                    imageSelected = true
                }
            }
        }

        if(imageSelected){
            val dialog = DialogWithImage(BitmapPainter(imageBitmap!!),"", {imageSelected = false}) { name, targets ->
                println("Sending $name to $targets")

                if (imageBytes == null) {
                    throw IllegalArgumentException("Image bytes is null")
                }

                val realTargets = MutableList(0) { _ -> "" }

                for (target in targets) {
                    if (target != "") {
                        realTargets.add(target)
                    }
                }

                println("targets: $targets $realTargets")

                sendFile(name, realTargets, imageBytes!!) { success ->
                    println("Sent file: $success")
                }

                imageSelected = false
            }
        }

        Column {
            Text("${Services.shared.interfaceManager?.getInterface()?.ipAddress}:$port")
            Button(onClick = { sendFile("meme.png", listOf("127.0.0.1"), ByteArray(1500)) { success ->
                println(
                    "Sent file: $success"
                )
            }
            }) {
                Text("Send")
            }
            Button(onClick = { requestFile("meme.png", listOf("127.0.0.1")) { file ->
                println(
                    "Sent file: $file"
                )
            }
            }) {
                Text("Receive")
            }
            Text(text="Totem Crypto")//, textAlign = Center)
            PlusButton(galleryManager)
            Text(text = "Encrypted Files")
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp)
            ) {
                items(files) { file ->
                    Button(
                        onClick = { requestFile(file, listOf("127.0.0.1")) { file ->
                            println("Received file: $file")
                        }
                    }){
                        Text(file)
                    }
                }
            }
        }
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
    submitDialog: (name: String, targets: List<String>) -> Unit
) {
    var currentText by remember { mutableStateOf("")}
    var destinations by remember { mutableStateOf(mutableListOf("", "", "", "")) }
    Dialog(onDismissRequest = { dismissDialog() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
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
                Text(text = "Specify the destination IP adresses:")

                for (i in 0..<4) {
                    DestinationField(destinations[i]) { newText ->
                        destinations[i] = newText
                    }
                }
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
                        submitDialog(currentText, destinations)
                    }) {
                        Text("Send")
                    }
                }
            }
        }
    }
}
@Composable
fun DestinationField(ip:String, onValueChange: (String) -> Unit){
    var currentText by remember {mutableStateOf("")}
    TextField(value = currentText, onValueChange = {
        currentText = it
        onValueChange(it)
    })
}

fun storeFileName() {
    TODO("Not yet implemented")
}

