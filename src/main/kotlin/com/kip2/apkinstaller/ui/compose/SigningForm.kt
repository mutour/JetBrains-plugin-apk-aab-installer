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

@Composable
fun SigningForm(
    project: com.intellij.openapi.project.Project?,
    configs: List<SigningConfig>,
    onConfigSelected: (SigningConfig) -> Unit,
    onManualChange: (SigningConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedConfig by remember { mutableStateOf<SigningConfig?>(null) }
    
    val storeFileState = rememberTextFieldState("")
    val storePasswordState = rememberTextFieldState("")
    val keyAliasState = rememberTextFieldState("")
    val keyPasswordState = rememberTextFieldState("")

    LaunchedEffect(storeFileState.text, storePasswordState.text, keyAliasState.text, keyPasswordState.text) {
        onManualChange(SigningConfig(
            "manual",
            storeFileState.text.toString(),
            storePasswordState.text.toString(),
            keyAliasState.text.toString(),
            keyPasswordState.text.toString(),
            "manual"
        ))
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Signing Configuration", style = JewelTheme.defaultTextStyle)
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
        TextField(
            state = storeFileState,
            modifier = Modifier.fillMaxWidth()
        )
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
