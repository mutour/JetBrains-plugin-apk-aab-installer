package com.kip2.apkinstaller.settings

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.Configurable
import com.intellij.structuralsearch.plugin.ui.UIUtil.setContent
import com.kip2.apkinstaller.ui.compose.SettingsScreen
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import javax.swing.JComponent

class SettingsConfigurable : Configurable {
    private val LOG = logger<SettingsConfigurable>()

    // 1. 保存 ComposePanel 的引用
    private var composePanel: ComposePanel? = null

    private var adbPath = ""
    private var bundletoolPath = ""

    override fun getDisplayName(): String = "Apk/Aab Installer"

    override fun createComponent(): JComponent {
        LOG.info("Creating settings component")
        val settings = PluginSettings.getInstance()
        adbPath = settings.adbPath
        bundletoolPath = settings.bundletoolPath

        // 2. 使用 JewelComposePanel 并保存引用
        val panel = JewelComposePanel(focusOnClickInside = true) {
            @Suppress("DEPRECATION_ERROR")
            SwingBridgeTheme {
                SettingsScreen(
                    initialAdbPath = adbPath,
                    initialBundletoolPath = bundletoolPath,
                    onAdbPathChange = { adbPath = it },
                    onBundletoolPathChange = { bundletoolPath = it }
                )
            }
        }

        this.composePanel = panel as? ComposePanel
        return panel
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

    @OptIn(ExperimentalComposeUiApi::class)
    override fun disposeUIResources() {
        // 3. 关键：显式释放资源
        composePanel?.dispose()
        composePanel = null
    }
}
