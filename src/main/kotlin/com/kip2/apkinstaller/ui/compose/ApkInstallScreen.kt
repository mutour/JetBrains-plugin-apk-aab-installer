package com.kip2.apkinstaller.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kip2.apkinstaller.model.Device
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

@Composable
fun ApkInstallScreen(
    devices: List<Device>,
    onInstall: (List<Device>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedDevices by remember { mutableStateOf(if (devices.isNotEmpty()) listOf(devices[0]) else emptyList<Device>()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Select Target Devices", style = JewelTheme.defaultTextStyle)
        Spacer(Modifier.height(8.dp))
        
        DeviceSelector(
            devices = devices,
            onSelectionChanged = { selectedDevices = it },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Divider(Orientation.Horizontal)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            DefaultButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = { onInstall(selectedDevices) },
                enabled = selectedDevices.isNotEmpty()
            ) {
                Text("Install APK")
            }
        }
    }
}
