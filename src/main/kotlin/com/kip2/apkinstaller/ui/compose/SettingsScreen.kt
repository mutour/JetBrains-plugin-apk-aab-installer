package com.kip2.apkinstaller.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kip2.apkinstaller.util.AdbLocator
import com.kip2.apkinstaller.util.BundletoolHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

@Composable
fun SettingsScreen(
    initialAdbPath: String,
    initialBundletoolPath: String,
    onAdbPathChange: (String) -> Unit,
    onBundletoolPathChange: (String) -> Unit
) {
    val adbPathState = rememberTextFieldState(initialAdbPath)
    val bundletoolPathState = rememberTextFieldState(initialBundletoolPath)
    
    var detectedAdbPaths by remember { mutableStateOf(emptyList<String>()) }
    var detectedBundletoolPaths by remember { mutableStateOf(emptyList<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val adbPaths = withContext(Dispatchers.IO) { AdbLocator().findAdbPaths() }
            val btPaths = withContext(Dispatchers.IO) { BundletoolHelper().findBundletoolPaths() }
            
            detectedAdbPaths = adbPaths
            detectedBundletoolPaths = btPaths
            
            if (adbPathState.text.isEmpty() && adbPaths.isNotEmpty()) {
                val first = adbPaths.first()
                adbPathState.setTextAndPlaceCursorAtEnd(first)
                onAdbPathChange(first)
            }
            if (bundletoolPathState.text.isEmpty() && btPaths.isNotEmpty()) {
                val first = btPaths.first()
                bundletoolPathState.setTextAndPlaceCursorAtEnd(first)
                onBundletoolPathChange(first)
            }
        } catch (e: Throwable) {
            errorMessage = "Error detecting environment: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(adbPathState.text) {
        onAdbPathChange(adbPathState.text.toString())
    }
    
    LaunchedEffect(bundletoolPathState.text) {
        onBundletoolPathChange(bundletoolPathState.text.toString())
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Plugin Settings", 
            style = JewelTheme.defaultTextStyle.copy(fontSize = 18.sp)
        )
        Spacer(Modifier.height(24.dp))

        if (errorMessage != null) {
            Text(errorMessage!!, color = JewelTheme.globalColors.text.error)
            Spacer(Modifier.height(16.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                Text("Searching for environment...", modifier = Modifier.align(Alignment.Center))
            }
        } else {
            Text("ADB Path", style = JewelTheme.defaultTextStyle)
            Spacer(Modifier.height(8.dp))
            
            TextField(
                state = adbPathState,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (detectedAdbPaths.isNotEmpty() && adbPathState.text.toString() !in detectedAdbPaths) {
                Spacer(Modifier.height(4.dp))
                Text("Detected paths:", style = JewelTheme.defaultTextStyle)
                detectedAdbPaths.forEach { path ->
                    Link(text = path, onClick = { 
                        adbPathState.setTextAndPlaceCursorAtEnd(path)
                        onAdbPathChange(path)
                    })
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Bundletool Path", style = JewelTheme.defaultTextStyle)
            Spacer(Modifier.height(8.dp))
            
            TextField(
                state = bundletoolPathState,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (detectedBundletoolPaths.isNotEmpty() && bundletoolPathState.text.toString() !in detectedBundletoolPaths) {
                Spacer(Modifier.height(4.dp))
                Text("Detected paths:", style = JewelTheme.defaultTextStyle)
                detectedBundletoolPaths.forEach { path ->
                    Link(text = path, onClick = { 
                        bundletoolPathState.setTextAndPlaceCursorAtEnd(path)
                        onBundletoolPathChange(path)
                    })
                }
            }
        }
    }
}
