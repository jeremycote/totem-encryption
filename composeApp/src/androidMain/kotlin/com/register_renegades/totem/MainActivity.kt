package com.register_renegades.totem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.register_renegades.totem.network.AndroidSystemInterfaceManager
import com.register_renegades.totem.network.InterfaceManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val systemInterfaceManager = AndroidSystemInterfaceManager()
        Services.initShared(InterfaceManager(systemInterfaceManager))

        super.onCreate(savedInstanceState)

        setContent {
           OtherApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    OtherApp()
}