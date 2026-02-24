package com.kip2.apkinstaller.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.kip2.apkinstaller.service.SigningConfig
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
@State(
    name = "ProjectSigningSettings",
    storages = [Storage("apkInstallerSigning.xml")]
)
class ProjectSigningSettings : PersistentStateComponent<ProjectSigningSettings.State> {

    data class State(
        @get:Tag("configs")
        var configs: MutableList<SigningConfigState> = mutableListOf()
    )

    data class SigningConfigState(
        var name: String = "",
        var storeFile: String? = null,
        var storePassword: String? = null,
        var keyAlias: String? = null,
        var keyPassword: String? = null,
        var moduleName: String = "unknown"
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getConfigs(): List<SigningConfig> {
        return myState.configs.map {
            SigningConfig(it.name, it.storeFile, it.storePassword, it.keyAlias, it.keyPassword, it.moduleName)
        }
    }

    fun saveConfigs(configs: List<SigningConfig>) {
        myState.configs.clear()
        myState.configs.addAll(configs.map {
            SigningConfigState(it.name, it.storeFile, it.storePassword, it.keyAlias, it.keyPassword, it.moduleName)
        })
    }

    fun addConfig(config: SigningConfig) {
        myState.configs.removeIf { it.name == config.name && it.moduleName == config.moduleName }
        myState.configs.add(
            SigningConfigState(config.name, config.storeFile, config.storePassword, config.keyAlias, config.keyPassword, config.moduleName)
        )
    }

    companion object {
        fun getInstance(project: Project): ProjectSigningSettings = project.getService(ProjectSigningSettings::class.java)
    }
}
