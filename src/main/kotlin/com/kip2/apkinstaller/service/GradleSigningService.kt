package com.kip2.apkinstaller.service

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.ModuleManager
import java.io.File

data class SigningConfig(
    val name: String,
    val storeFile: String?,
    val storePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?
)

class GradleSigningService(private val project: Project) {
    
    fun getSigningConfigs(module: Module): List<SigningConfig> {
        return try {
            val modelClass = Class.forName("com.android.tools.idea.gradle.project.model.AndroidModuleModel")
            val getMethod = modelClass.getMethod("get", Module::class.java)
            val model = getMethod.invoke(null, module) ?: return emptyList()
            
            val getAndroidProjectMethod = model.javaClass.getMethod("getAndroidProject")
            val androidProject = getAndroidProjectMethod.invoke(model)
            
            val getSigningConfigsMethod = androidProject.javaClass.getMethod("getSigningConfigs")
            val configs = getSigningConfigsMethod.invoke(androidProject) as Collection<*>
            
            configs.map { config ->
                val name = config!!.javaClass.getMethod("getName").invoke(config) as String
                val storeFile = config.javaClass.getMethod("getStoreFile").invoke(config) as File?
                val storePassword = config.javaClass.getMethod("getStorePassword").invoke(config) as String?
                val keyAlias = config.javaClass.getMethod("getKeyAlias").invoke(config) as String?
                val keyPassword = config.javaClass.getMethod("getKeyPassword").invoke(config) as String?
                
                SigningConfig(name, storeFile?.absolutePath, storePassword, keyAlias, keyPassword)
            }
        } catch (e: Exception) {
            // Android plugin not available or version mismatch
            emptyList()
        }
    }
    
    fun findModuleForFile(filePath: String): Module? {
        val modules = ModuleManager.getInstance(project).modules
        return modules.find { module ->
            val moduleFile = module.moduleFile ?: return@find false
            filePath.startsWith(moduleFile.parent.path)
        }
    }
}
