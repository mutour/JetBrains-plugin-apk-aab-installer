package com.kip2.apkinstaller.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kip2.apkinstaller.service.SigningConfig
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

@Composable
fun SigningForm(
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
            keyPasswordState.text.toString()
        ))
    }

    Column(modifier = modifier) {
        Text("Signing Configuration", style = JewelTheme.defaultTextStyle)
        Spacer(Modifier.height(8.dp))
        
        if (configs.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                configs.forEach { config ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        RadioButton(
                            selected = selectedConfig == config,
                            onClick = {
                                selectedConfig = config
                                storeFileState.setTextAndPlaceCursorAtEnd(config.storeFile ?: "")
                                storePasswordState.setTextAndPlaceCursorAtEnd(config.storePassword ?: "")
                                keyAliasState.setTextAndPlaceCursorAtEnd(config.keyAlias ?: "")
                                keyPasswordState.setTextAndPlaceCursorAtEnd(config.keyPassword ?: "")
                                onConfigSelected(config)
                            }
                        )
                        Text(text = config.name, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        TextField(
            state = storeFileState,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                state = storePasswordState,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                state = keyAliasState,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        TextField(
            state = keyPasswordState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
