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
        val paths = findAdbPaths()
        return if (paths.isNotEmpty()) {
            AdbResult(paths.first(), "Auto-detected")
        } else {
            AdbResult(null, "Not found")
        }
    }

    fun findAdbPaths(): List<String> {
        val paths = mutableSetOf<String>()
        // 1. Check IDE Settings
        val settings = com.kip2.apkinstaller.settings.PluginSettings.getInstance()
        if (settings.adbPath.isNotBlank() && File(settings.adbPath).exists()) {
            paths.add(settings.adbPath)
        }
        
        // 2. Check local.properties
        findInLocalProperties()?.path?.let { paths.add(it) }
        
        // 3. Check ANDROID_HOME
        findInAndroidHome()?.path?.let { paths.add(it) }
        // 4. Check system PATH
        val pathEnv = System.getenv("PATH")
        if (pathEnv != null) {
            val systemPaths = pathEnv.split(File.pathSeparator)
            for (p in systemPaths) {
                val adb = File(p, "adb")
                if (adb.exists() && adb.isFile) {
                    paths.add(adb.absolutePath)
                }
            }
        }

        // 5. Common locations on macOS/Linux/Windows
        val commonLocations = listOf(
            File(System.getProperty("user.home"), "Library/Android/sdk/platform-tools/adb"),
            File(System.getProperty("user.home"), "AppData/Local/Android/Sdk/platform-tools/adb.exe"),
            File("/usr/local/bin/adb"),
            File("/usr/bin/adb")
        )
        commonLocations.filter { it.exists() }.forEach { paths.add(it.absolutePath) }
        
        return paths.toList()
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
