package com.kip2.apkinstaller.util

import com.intellij.openapi.project.ProjectManager
import java.io.File
import java.util.Properties

class AdbLocator {
    
    data class AdbResult(
        val path: String?,
        val source: String
    )
    
    fun findAdb(): AdbResult {
        // 1. Check IDE Settings
        val settings = com.kip2.apkinstaller.settings.PluginSettings.getInstance()
        if (settings.adbPath.isNotBlank() && File(settings.adbPath).exists()) {
            return AdbResult(settings.adbPath, "IDE Settings")
        }
        
        // 2. Check local.properties from open projects
        val localProperties = findInLocalProperties()
        if (localProperties != null) return localProperties
        
        // 3. Check ANDROID_HOME environment variable
        val envAdb = findInAndroidHome()
        if (envAdb != null) return envAdb
        
        // 4. Check system PATH
        val pathAdb = findInSystemPath()
        if (pathAdb != null) return pathAdb
        
        return AdbResult(null, "Not found")
    }
    
    private fun findInLocalProperties(): AdbResult? {
        val projects = ProjectManager.getInstance().openProjects
        for (project in projects) {
            val basePath = project.basePath ?: continue
            val localPropsFile = File(basePath, "local.properties")
            if (localPropsFile.exists()) {
                val properties = Properties()
                localPropsFile.inputStream().use { properties.load(it) }
                val sdkDir = properties.getProperty("sdk.dir")
                if (sdkDir != null) {
                    val adb = File(sdkDir, "platform-tools/adb")
                    if (adb.exists()) {
                        return AdbResult(adb.absolutePath, "local.properties")
                    }
                }
            }
        }
        return null
    }
    
    private fun findInAndroidHome(): AdbResult? {
        val androidHome = System.getenv("ANDROID_HOME") 
            ?: System.getenv("ANDROID_SDK_ROOT") 
            ?: return null
        val adb = File(androidHome, "platform-tools/adb")
        return if (adb.exists()) AdbResult(adb.absolutePath, "ANDROID_HOME") else null
    }
    
    private fun findInSystemPath(): AdbResult? {
        val pathEnv = System.getenv("PATH") ?: return null
        val paths = pathEnv.split(File.pathSeparator)
        for (p in paths) {
            val adb = File(p, "adb")
            if (adb.exists() && adb.isFile) {
                return AdbResult(adb.absolutePath, "System PATH")
            }
        }
        return null
    }
}
