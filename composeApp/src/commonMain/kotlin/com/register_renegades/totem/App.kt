package com.register_renegades.totem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement.Absolute.Center
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.register_renegades.totem.entity.File
import com.register_renegades.totem.network.InterfaceManager
import com.register_renegades.totem.network.SocketUDPListener
import com.register_renegades.totem.network.UDPPacketSender
import io.ktor.client.HttpClient
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.component.KoinComponent

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
fun App() {
    MaterialTheme {
        val listener = SocketUDPListener(port = 5000)
        val dummyList: List<File> = listOf(File(1, "Dank meme"), File(1,"Homework"))
//        CoroutineScope(Dispatchers.IO).launch {
//            listener.startListening()
//        }

//        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//            Button(onClick = {
//                sendPacket()
//            }) {
//                Text("Send message!")
//            }
//        }
        Column {
            Text(text="Totem Crypto")//, textAlign = Center)
            PlusButton()
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
fun PlusButton(){
    Button(onClick = {},
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Blue)){
        Text(text="Encrypt file")
    }
}
//@Composable
//fun()