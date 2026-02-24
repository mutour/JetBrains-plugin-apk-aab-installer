package com.kip2.apkinstaller.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.service.SigningConfig
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*

data class AabInstallOptions(
    val selectedDevices: List<com.kip2.apkinstaller.model.Device>,
    val signingConfig: com.kip2.apkinstaller.service.SigningConfig,
    val isUniversalMode: Boolean,
    val localTesting: Boolean,
    val updateExisting: Boolean
)

@Composable
fun AabInstallScreen(
    project: com.intellij.openapi.project.Project?,
    devices: List<Device>,
    detectedConfigs: List<SigningConfig>,
    onInstall: (AabInstallOptions) -> Unit,
    onCancel: () -> Unit
) {
    var selectedDevices by remember { mutableStateOf(emptyList<Device>()) }
    var signingConfig by remember { mutableStateOf(SigningConfig("manual", "", "", "", "", "manual")) }
    var isUniversalMode by remember { mutableStateOf(false) }
    var localTesting by remember { mutableStateOf(false) }
    var updateExisting by remember { mutableStateOf(true) }

    Column(modifier = Modifier.width(700.dp).padding(16.dp)) {
        Row(modifier = Modifier.heightIn(max = 400.dp)) {
            Column(modifier = Modifier.weight(0.4f)) {
                Text("Select Devices", style = JewelTheme.defaultTextStyle)
                Spacer(Modifier.height(8.dp))
                DeviceSelector(
                    devices = devices,
                    onSelectionChanged = { selectedDevices = it },
                    modifier = Modifier.fillMaxHeight()
                )
            }

            Spacer(Modifier.width(24.dp))
            Divider(Orientation.Vertical)
            Spacer(Modifier.width(24.dp))

            Column(modifier = Modifier.weight(0.6f)) {
                SigningForm(
                    project = project,
                    configs = detectedConfigs,
                    onConfigSelected = { signingConfig = it },
                    onManualChange = { signingConfig = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))
                Divider(Orientation.Horizontal)
                Spacer(Modifier.height(16.dp))

                Text("Installation Parameters", style = JewelTheme.defaultTextStyle)
                Spacer(Modifier.height(8.dp))

                CheckboxRow(
                    text = "Universal Mode (--mode=universal)",
                    checked = isUniversalMode,
                    onCheckedChange = { isUniversalMode = it }
                )
                CheckboxRow(
                    text = "Local Testing (--local-testing)",
                    checked = localTesting,
                    onCheckedChange = { localTesting = it }
                )
                CheckboxRow(
                    text = "Update Existing (--update)",
                    checked = updateExisting,
                    onCheckedChange = { updateExisting = it }
                )
            }
        }

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
                onClick = {
                    onInstall(
                        AabInstallOptions(
                            selectedDevices,
                            signingConfig,
                            isUniversalMode,
                            localTesting,
                            updateExisting
                        )
                    )
                },
                enabled = selectedDevices.isNotEmpty()
            ) {
                Text("Install AAB")
            }
        }
    }
}
