package com.kip2.apkinstaller.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.service.SigningConfig
import com.kip2.apkinstaller.ui.compose.AabInstallOptions
import com.kip2.apkinstaller.ui.compose.AabInstallScreen
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import javax.swing.JComponent

class AabInstallDialog(
    project: Project,
    private val devices: List<Device>,
    private val detectedConfigs: List<SigningConfig>
) : DialogWrapper(project) {

    var installOptions: AabInstallOptions? = null
        private set

    init {
        title = "Install AAB to Device(s)"
        setOKButtonText("Install")
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JewelComposePanel {
            SwingBridgeTheme {
                AabInstallScreen(
                    devices = devices,
                    detectedConfigs = detectedConfigs,
                    onInstall = {
                        installOptions = it
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
}
