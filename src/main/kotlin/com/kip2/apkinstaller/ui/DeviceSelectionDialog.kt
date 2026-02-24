package com.kip2.apkinstaller.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.ui.compose.ApkInstallScreen
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import javax.swing.JComponent

class DeviceSelectionDialog(
    project: Project,
    private val devices: List<Device>
) : DialogWrapper(project) {

    private var selectedDevices: List<Device> = emptyList()

    init {
        title = "Select Target Devices"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JewelComposePanel(focusOnClickInside = true) {
            @Suppress("DEPRECATION_ERROR")
            SwingBridgeTheme {
                ApkInstallScreen(
                    devices = devices,
                    onInstall = {
                        selectedDevices = it
                        close(OK_EXIT_CODE)
                    },
                    onCancel = {
                        close(CANCEL_EXIT_CODE)
                    }
                )
            }
        }
    }

    override fun createActions(): Array<javax.swing.Action> = emptyArray()

    fun getSelectedDevices(): List<Device> {
        return selectedDevices
    }
}
