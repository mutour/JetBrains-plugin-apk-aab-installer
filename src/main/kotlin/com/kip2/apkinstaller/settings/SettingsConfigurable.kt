package com.kip2.apkinstaller.settings

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.Configurable
import com.kip2.apkinstaller.ui.compose.SettingsScreen
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import javax.swing.JComponent

class SettingsConfigurable : Configurable {
    private val LOG = logger<SettingsConfigurable>()
    private var adbPath = ""
    private var bundletoolPath = ""
    private var panel: JComponent? = null

    override fun getDisplayName(): String = "Apk/Aab Installer"

    override fun createComponent(): JComponent {
        LOG.info("Creating settings component")
        val settings = PluginSettings.getInstance()
        adbPath = settings.adbPath
        bundletoolPath = settings.bundletoolPath
        
        val newPanel = JewelComposePanel {
            SwingBridgeTheme {
                SettingsScreen(
                    initialAdbPath = adbPath,
                    initialBundletoolPath = bundletoolPath,
                    onAdbPathChange = { adbPath = it },
                    onBundletoolPathChange = { bundletoolPath = it }
                )
            }
        }
        panel = newPanel
        return newPanel
    }

    override fun isModified(): Boolean {
        val settings = PluginSettings.getInstance()
        return adbPath != settings.adbPath || bundletoolPath != settings.bundletoolPath
    }

    override fun apply() {
        val settings = PluginSettings.getInstance()
        settings.adbPath = adbPath
        settings.bundletoolPath = bundletoolPath
    }

    override fun reset() {
        val settings = PluginSettings.getInstance()
        adbPath = settings.adbPath
        bundletoolPath = settings.bundletoolPath
    }

    override fun disposeUIResources() {
        panel = null
    }
}
