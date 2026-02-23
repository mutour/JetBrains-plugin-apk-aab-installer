package com.kip2.apkinstaller.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class SettingsConfigurable : Configurable {
    private var settingsComponent: SettingsComponent? = null

    override fun getDisplayName(): String = "Apk/Aab Installer"

    override fun createComponent(): JComponent? {
        settingsComponent = SettingsComponent()
        return settingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings = PluginSettings.getInstance()
        return settingsComponent?.getAdbPath() != settings.adbPath ||
               settingsComponent?.getBundletoolPath() != settings.bundletoolPath
    }

    override fun apply() {
        val settings = PluginSettings.getInstance()
        settings.adbPath = settingsComponent?.getAdbPath() ?: ""
        settings.bundletoolPath = settingsComponent?.getBundletoolPath() ?: ""
    }

    override fun reset() {
        val settings = PluginSettings.getInstance()
        settingsComponent?.setAdbPath(settings.adbPath)
        settingsComponent?.setBundletoolPath(settings.bundletoolPath)
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
