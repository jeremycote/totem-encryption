package com.register_renegades.totem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.register_renegades.totem.network.NetworkAddress
import com.register_renegades.totem.network.NetworkEventListener
import com.register_renegades.totem.network.SocketTCP
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

fun sendPacket() {
    CoroutineScope(Dispatchers.IO).launch {
        println("sendPacket coroutine launched")
        val sendSocket = SocketTCP()

        val target = NetworkAddress("127.0.0.1", 5004)
        val file = ByteArray(1024)
        sendSocket.initiateFileSave(listOf(target), "test.txt", file)
    }
}

class AppDelegate(private val saveFile: (String, UInt) -> Boolean, private val loadFile: (String) -> Boolean): NetworkEventListener {
    override fun onRequestSaveFile(name: String, size: UInt): Boolean {
        return saveFile(name, size)
    }

    override fun onRequestLoadFile(name: String): Boolean {
        return loadFile(name)
    }
}

@Composable
@Preview
fun App() {

    var showText by remember { mutableStateOf(false) }

    val delegate = AppDelegate({ name, size ->
        run {
            println("Save file $name with size $size")
            showText = true
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

    MaterialTheme {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("IP Address: ${Services.shared.interfaceManager?.getInterface()?.ipAddress}")
            Text("Port: $port")

            Button(onClick = {
                sendPacket()
            }) {
                Text("Send message!")
            }

            Button(onClick = {
                showText = !showText
            }) {
                Text("Toggle")
            }

            if (showText) {
                Text("Received Save!")
            }
        }
    }
}