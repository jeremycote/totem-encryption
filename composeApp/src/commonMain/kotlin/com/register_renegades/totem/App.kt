package com.register_renegades.totem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        CoroutineScope(Dispatchers.IO).launch {
            listener.startListening()
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                sendPacket()
            }) {
                Text("Send message!")
            }
        }
    }
}