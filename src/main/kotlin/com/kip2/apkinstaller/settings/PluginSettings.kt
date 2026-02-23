package com.kip2.apkinstaller.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent

@State(
    name = "ApkInstallerSettings",
    storages = [Storage("apk-installer-settings.xml")]
)
class PluginSettings : PersistentStateComponent<PluginSettings.State> {
    class State {
        var adbPath: String = ""
        var bundletoolPath: String = ""
    }

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    var adbPath: String
        get() = myState.adbPath
        set(value) { myState.adbPath = value }

    var bundletoolPath: String
        get() = myState.bundletoolPath
        set(value) { myState.bundletoolPath = value }

    companion object {
        fun getInstance(): PluginSettings = ApplicationManager.getApplication().getService(PluginSettings::class.java)
    }
}
