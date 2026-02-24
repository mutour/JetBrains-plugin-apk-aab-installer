package com.kip2.apkinstaller.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kip2.apkinstaller.service.SigningConfig
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory


@Composable
fun SigningForm(
    project: com.intellij.openapi.project.Project?,
    configs: List<SigningConfig>,
    onConfigSelected: (SigningConfig) -> Unit,
    onManualChange: (SigningConfig) -> Unit,
    onDetectConfigs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedConfig by remember { mutableStateOf<SigningConfig?>(if (configs.isNotEmpty()) configs[0] else null) }
    
    val storeFileState = rememberTextFieldState(selectedConfig?.storeFile ?: "")
    val storePasswordState = rememberTextFieldState(selectedConfig?.storePassword ?: "")
    val keyAliasState = rememberTextFieldState(selectedConfig?.keyAlias ?: "")
    val keyPasswordState = rememberTextFieldState(selectedConfig?.keyPassword ?: "")

    // Initial selection propagation
    LaunchedEffect(Unit) {
        if (selectedConfig != null) {
            onConfigSelected(selectedConfig!!)
        }
    }

    LaunchedEffect(storeFileState.text, storePasswordState.text, keyAliasState.text, keyPasswordState.text) {
        val currentStoreFile = storeFileState.text.toString()
        val currentStorePassword = storePasswordState.text.toString()
        val currentKeyAlias = keyAliasState.text.toString()
        val currentKeyPassword = keyPasswordState.text.toString()

        // Check if current values match selected config
        val match = selectedConfig?.let {
            it.storeFile == currentStoreFile &&
            it.storePassword == currentStorePassword &&
            it.keyAlias == currentKeyAlias &&
            it.keyPassword == currentKeyPassword
        } ?: false

        if (!match) {
            // Values changed from preset, switch to custom
            val newConfig = SigningConfig(
                "Custom",
                currentStoreFile,
                currentStorePassword,
                currentKeyAlias,
                currentKeyPassword,
                "User"
            )
            // If we are not already on a custom config, or if we are updating it
            if (selectedConfig?.name != "Custom" || selectedConfig?.moduleName != "User") {
                selectedConfig = newConfig
            } else {
                // We are already on custom, just update the object reference for parent
                selectedConfig = newConfig
            }
            onManualChange(newConfig)
        }
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Signing Configuration", style = JewelTheme.defaultTextStyle)
            Spacer(Modifier.weight(1f))
            if (project != null) {
                DefaultButton(onClick = onDetectConfigs) {
                    Text("Detect Gradle Configs")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        
        if (configs.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Dropdown(
                    menuContent = {
                        configs.forEach { config ->
                            val isSelected = selectedConfig == config
                            // Move theme access inside Composable content lambda or use remember if needed,
                            // but JewelTheme is composable-aware.
                            // The issue is likely accessing JewelTheme properties directly in a non-composable scope
                            // However, menuContent IS a composable lambda.
                            
                            selectableItem(
                                selected = isSelected,
                                onClick = {
                                    selectedConfig = config
                                    storeFileState.setTextAndPlaceCursorAtEnd(config.storeFile ?: "")
                                    storePasswordState.setTextAndPlaceCursorAtEnd(config.storePassword ?: "")
                                    keyAliasState.setTextAndPlaceCursorAtEnd(config.keyAlias ?: "")
                                    keyPasswordState.setTextAndPlaceCursorAtEnd(config.keyPassword ?: "")
                                    onConfigSelected(config)
                                }
                            ) {
                                Text("${config.moduleName}: ${config.name}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedConfig?.let { "${it.moduleName}: ${it.name}" } ?: "Select from existing configs...",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Text("Keystore Path", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp))
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                state = storeFileState,
                modifier = Modifier.weight(1f)
            )
            if (project != null) {
                Spacer(Modifier.width(8.dp))
                DefaultButton(onClick = {
                    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                    descriptor.title = "Select Keystore File"
                    FileChooser.chooseFile(descriptor, project, null) { file ->
                        storeFileState.setTextAndPlaceCursorAtEnd(file.path)
                    }
                }) {
                    Text("...")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Keystore Password", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                TextField(
                    state = storePasswordState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Key Password", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp))
                Spacer(Modifier.height(4.dp))
                TextField(
                    state = keyPasswordState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Key Alias", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp))
        Spacer(Modifier.height(4.dp))
        TextField(
            state = keyAliasState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
