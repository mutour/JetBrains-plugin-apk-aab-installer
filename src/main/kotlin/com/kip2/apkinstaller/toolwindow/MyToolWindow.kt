package com.kip2.apkinstaller.toolwindow

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.service.AabInstaller
import com.kip2.apkinstaller.service.ApkInstaller
import com.kip2.apkinstaller.service.DeviceManager
import com.kip2.apkinstaller.service.GradleSigningService
import com.kip2.apkinstaller.settings.PluginSettings
import com.kip2.apkinstaller.ui.AabInstallDialog
import com.kip2.apkinstaller.ui.DeviceSelectionDialog
import com.kip2.apkinstaller.ui.compose.AabInstallOptions
import com.kip2.apkinstaller.util.AdbLocator
import com.kip2.apkinstaller.util.BundletoolHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.TransferHandler
import javax.swing.TransferHandler.TransferSupport

class MyToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val isDragging = mutableStateOf(false)

        val onFileSelect = {
            val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
                .withTitle("Select APK or AAB")
                .withDescription("Choose an .apk or .aab file to install")
                .withFileFilter { file -> file.extension?.lowercase() in listOf("apk", "aab") }
            
            val file = FileChooser.chooseFile(descriptor, project, null)
            if (file != null) {
                handleFileInstall(project, File(file.path))
            }
        }

        val composePanel = JewelComposePanel(focusOnClickInside = true) {
            MyToolWindowContent(project, isDragging, onFileSelect)
        }

        // Wrap ComposePanel in a standard JPanel to support TransferHandler
        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(composePanel, BorderLayout.CENTER)

        // Use TransferHandler instead of DropTarget for reliable DnD
        mainPanel.transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                val canImport = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                isDragging.value = canImport
                return canImport
            }

            override fun importData(support: TransferSupport): Boolean {
                isDragging.value = false
                if (!canImport(support)) return false

                return try {
                    val files = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                    val file = files.firstOrNull() as? File
                    if (file != null) {
                        handleFileInstall(project, file)
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }

        val content = ContentFactory.getInstance().createContent(mainPanel, "Installer & Settings", false)
        toolWindow.contentManager.addContent(content)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MyToolWindowContent(
    project: Project, 
    isDraggingState: MutableState<Boolean>,
    onFileSelect: () -> Unit
) {
    val settings = PluginSettings.getInstance()
    val adbPathState = rememberTextFieldState(settings.adbPath)
    val bundletoolPathState = rememberTextFieldState(settings.bundletoolPath)

    var detectedAdbPaths by remember { mutableStateOf(emptyList<String>()) }
    var detectedBundletoolPaths by remember { mutableStateOf(emptyList<String>()) }
    var isEnvLoading by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            detectedAdbPaths = AdbLocator().findAdbPaths()
            detectedBundletoolPaths = BundletoolHelper().findBundletoolPaths()
            isEnvLoading = false
        }
    }

    LaunchedEffect(adbPathState.text) {
        settings.adbPath = adbPathState.text.toString()
    }

    LaunchedEffect(bundletoolPathState.text) {
        settings.bundletoolPath = bundletoolPathState.text.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Environment Settings",
            style = JewelTheme.defaultTextStyle.copy(fontSize = 16.sp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ADB Path", style = JewelTheme.defaultTextStyle)
            TextField(
                state = adbPathState,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Path to adb executable") }
            )

            if (detectedAdbPaths.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Detected:", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp))
                    detectedAdbPaths.take(3).forEach { path ->
                        Link(path, onClick = { adbPathState.setTextAndPlaceCursorAtEnd(path) })
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bundletool Path", style = JewelTheme.defaultTextStyle)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    state = bundletoolPathState,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Path to bundletool.jar") }
                )
                OutlinedButton(onClick = {
                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Downloading bundletool", true) {
                        override fun run(indicator: ProgressIndicator) {
                            try {
                                val file = BundletoolHelper().downloadBundletool(indicator)
                                ApplicationManager.getApplication().invokeLater {
                                    bundletoolPathState.setTextAndPlaceCursorAtEnd(file.absolutePath)
                                }
                            } catch (e: Exception) {
                                NotificationGroupManager.getInstance()
                                    .getNotificationGroup("ApkInstaller.NotificationGroup")
                                    .createNotification("Download Failed", e.message ?: "Unknown error", NotificationType.ERROR)
                                    .notify(project)
                            }
                        }
                    })
                }) {
                    Text("Download")
                }
            }

            if (detectedBundletoolPaths.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Detected:", style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp))
                    detectedBundletoolPaths.take(3).forEach { path ->
                        Link(path, onClick = { bundletoolPathState.setTextAndPlaceCursorAtEnd(path) })
                    }
                }
            }
        }

        Divider(Orientation.Horizontal)

        Text(
            text = "Install APK / AAB",
            style = JewelTheme.defaultTextStyle.copy(fontSize = 16.sp)
        )

        val isDragging by isDraggingState

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 2.dp,
                    color = if (isDragging) JewelTheme.globalColors.borders.focused else JewelTheme.globalColors.borders.normal,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(onClick = onFileSelect),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isDragging) "Drop to Install" else "Drag APK or AAB here to install\nOr click to select",
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 14.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Supports .apk and .aab files",
                    style = JewelTheme.defaultTextStyle.copy(fontSize = 12.sp, color = Color.Gray)
                )
            }
        }
    }
}

private fun handleFileInstall(project: Project, file: File) {
    if (!file.exists()) return
    val extension = file.extension.lowercase()
    if (extension !in listOf("apk", "aab")) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ApkInstaller.NotificationGroup")
            .createNotification("ApkInstaller", "Unsupported file type: $extension", NotificationType.ERROR)
            .notify(project)
        return
    }

    val virtualFile = VfsUtil.findFileByIoFile(file, true) ?: return

    val deviceManager = DeviceManager()
    val devices = try {
        deviceManager.getDevices()
    } catch (ex: Exception) {
        showError(project, "Failed to get devices: ${ex.message}")
        return
    }

    if (devices.isEmpty()) {
        showError(project, "No devices connected")
        return
    }

    if (extension == "aab") {
        val bundletoolPath = BundletoolHelper().getBundletoolPath()
        if (bundletoolPath == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("ApkInstaller.NotificationGroup")
                .createNotification("Bundletool Required", "Please configure or download bundletool in settings above.", NotificationType.ERROR)
                .notify(project)
            return
        }
    }

    var aabOptions: AabInstallOptions? = null
    var finalTargetDevices = emptyList<Device>()

    if (extension == "aab") {
        val signingService = GradleSigningService(project)
        val module = signingService.findModuleForFile(virtualFile.path)
        val configs = if (module != null) signingService.getSigningConfigs(module) else emptyList()

        val dialog = AabInstallDialog(project, devices, configs)
        if (!dialog.showAndGet()) return
        aabOptions = dialog.installOptions ?: return
        finalTargetDevices = aabOptions.selectedDevices
    } else {
        val dialog = DeviceSelectionDialog(project, devices)
        if (!dialog.showAndGet()) return
        finalTargetDevices = dialog.getSelectedDevices()
    }

    if (finalTargetDevices.isEmpty()) return

    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Installing ${file.name}...", true) {
        override fun run(indicator: ProgressIndicator) {
            val results = try {
                if (extension == "aab") {
                    AabInstaller().install(file, aabOptions!!, indicator)
                } else {
                    ApkInstaller().install(file, finalTargetDevices, indicator)
                }
            } catch (ex: Exception) {
                showError(project, "Installation failed: ${ex.message}")
                return
            }

            if (indicator.isCanceled) return

            val successCount = results.count { it.success }
            val failedResults = results.filter { !it.success }

            if (failedResults.isEmpty()) {
                showInfo(project, "Successfully installed to $successCount device(s).")
            } else {
                val errorMsg = failedResults.joinToString("\n") { "${it.device.name}: ${it.output.trim()}" }
                showError(project, "Installed to $successCount device(s). Failed on ${failedResults.size} device(s):\n$errorMsg")
            }
        }
    })
}

private fun showError(project: Project, message: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("ApkInstaller.NotificationGroup")
        .createNotification("ApkInstaller Error", message, NotificationType.ERROR)
        .notify(project)
}

private fun showInfo(project: Project, message: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("ApkInstaller.NotificationGroup")
        .createNotification("ApkInstaller", message, NotificationType.INFORMATION)
        .notify(project)
}