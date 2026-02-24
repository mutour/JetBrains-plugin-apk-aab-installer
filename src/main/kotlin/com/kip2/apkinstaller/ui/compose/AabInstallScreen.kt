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
import com.kip2.apkinstaller.InstallerBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var selectedDevices by remember { mutableStateOf(if (devices.isNotEmpty()) listOf(devices[0]) else emptyList<Device>()) }
    // Initialize with first config if available, otherwise default manual
    var signingConfig by remember {
        mutableStateOf(if (detectedConfigs.isNotEmpty()) detectedConfigs[0] else SigningConfig("manual", "", "", "", "", "manual"))
    }
    var configs by remember { mutableStateOf(detectedConfigs) }
    val coroutineScope = rememberCoroutineScope()

    var isUniversalMode by remember { mutableStateOf(false) }
    var localTesting by remember { mutableStateOf(false) }
    var updateExisting by remember { mutableStateOf(true) }

    Column(modifier = Modifier.width(700.dp).padding(16.dp)) {
        Row(modifier = Modifier.heightIn(max = 400.dp)) {
            Column(modifier = Modifier.weight(0.4f)) {
                Text(InstallerBundle.message("aab.install.select.devices"), style = JewelTheme.defaultTextStyle)
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
                    configs = configs,
                    onConfigSelected = { signingConfig = it },
                    onManualChange = { newConfig ->
                        signingConfig = newConfig
                        // Update configs list to include this custom config
                        val mutable = configs.toMutableList()
                        // Replace existing Custom config or add new one
                        val existingIndex = mutable.indexOfFirst { it.name == newConfig.name && it.moduleName == newConfig.moduleName }
                        if (existingIndex >= 0) {
                            mutable[existingIndex] = newConfig
                        } else {
                            mutable.add(newConfig)
                        }
                        configs = mutable.toList()

                        // Save immediately
                        if (project != null) {
                            com.kip2.apkinstaller.settings.ProjectSigningSettings.getInstance(project).addConfig(newConfig)
                        }
                    },
                    onDetectConfigs = {
                        if (project != null) {
                            coroutineScope.launch {
                                val detected = withContext(Dispatchers.IO) {
                                    com.kip2.apkinstaller.service.GradleSigningService(project).getAllSigningConfigs()
                                }
                                // Merge detected with existing custom ones
                                val currentCustoms = configs.filter { it.moduleName == "User" || it.name == "Custom" }
                                val merged = (detected + currentCustoms).distinctBy { "${it.moduleName}:${it.name}" }
                                configs = merged

                                // Auto-select first if available and current is invalid or manual
                                if (merged.isNotEmpty() && (signingConfig.name == "manual" || signingConfig.name.isEmpty())) {
                                    signingConfig = merged[0]
                                }

                                // Save to project settings
                                com.kip2.apkinstaller.settings.ProjectSigningSettings.getInstance(project).saveConfigs(merged)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))
                Divider(Orientation.Horizontal)
                Spacer(Modifier.height(16.dp))

                Text(InstallerBundle.message("aab.install.parameters"), style = JewelTheme.defaultTextStyle)
                Spacer(Modifier.height(8.dp))

                CheckboxRow(
                    text = InstallerBundle.message("aab.install.mode.universal"),
                    checked = isUniversalMode,
                    onCheckedChange = { isUniversalMode = it }
                )
                CheckboxRow(
                    text = InstallerBundle.message("aab.install.local.testing"),
                    checked = localTesting,
                    onCheckedChange = { localTesting = it }
                )
                CheckboxRow(
                    text = InstallerBundle.message("aab.install.update"),
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
            OutlinedButton(onClick = onCancel) {
                Text(InstallerBundle.message("dialog.cancel.button"))
            }
            Spacer(Modifier.width(12.dp))
            DefaultButton(
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
                Text(InstallerBundle.message("aab.install.button"))
            }
        }
    }
}
