package com.kip2.apkinstaller.util

import com.kip2.apkinstaller.settings.PluginSettings
import java.io.File
import java.net.URL

class BundletoolHelper {
    
    fun getBundletoolPath(): String? {
        return findBundletoolPaths().firstOrNull()
    }

    fun findBundletoolPaths(): List<String> {
        val paths = mutableSetOf<String>()
        val settings = PluginSettings.getInstance()
        
        if (settings.bundletoolPath.isNotBlank() && File(settings.bundletoolPath).exists()) {
            paths.add(settings.bundletoolPath)
        }
        
        val defaultPath = getDefaultCachePath()
        if (File(defaultPath).exists()) {
            paths.add(defaultPath)
        }

        // Check system path
        val pathEnv = System.getenv("PATH")
        if (pathEnv != null) {
            val systemPaths = pathEnv.split(File.pathSeparator)
            for (p in systemPaths) {
                val bt = File(p, "bundletool")
                if (bt.exists() && bt.isFile) {
                    paths.add(bt.absolutePath)
                }
            }
        }
        
        return paths.toList()
    }
    
    fun downloadBundletool(progressIndicator: com.intellij.openapi.progress.ProgressIndicator): File {
        val cacheDir = getCacheDir()
        cacheDir.mkdirs()
        
        val targetFile = File(cacheDir, "bundletool.jar")
        
        if (targetFile.exists()) {
            return targetFile
        }
        
        val url = URL("https://github.com/google/bundletool/releases/download/1.16.0/bundletool-all-1.16.0.jar")
        
        progressIndicator.text = "Downloading bundletool..."
        
        url.openStream().use { input ->
            input.copyTo(targetFile.outputStream())
        }
        
        return targetFile
    }
    
    private fun getCacheDir(): File {
        return File(System.getProperty("user.home"), ".apk-installer/bundletool")
    }
    
    private fun getDefaultCachePath(): String {
        return File(getCacheDir(), "bundletool.jar").absolutePath
    }
}
