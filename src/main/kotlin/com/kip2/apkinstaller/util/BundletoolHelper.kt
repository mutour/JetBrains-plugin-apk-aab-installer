package com.kip2.apkinstaller.util

import com.google.gson.JsonParser
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.HttpRequests
import com.kip2.apkinstaller.settings.PluginSettings
import com.kip2.apkinstaller.InstallerBundle
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class BundletoolHelper {

    companion object {
        private val LOG = Logger.getInstance(BundletoolHelper::class.java)
    }
    
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
                val names = if (SystemInfo.isWindows) listOf("bundletool.exe", "bundletool.bat", "bundletool.cmd", "bundletool") else listOf("bundletool")
                val bt = names.map { File(p, it) }.firstOrNull { it.exists() && it.isFile }
                if (bt != null) {
                    paths.add(bt.absolutePath)
                }
            }
        }
        
        return paths.toList()
    }
    
    fun downloadBundletool(progressIndicator: com.intellij.openapi.progress.ProgressIndicator): File {
        val cacheDir = getCacheDir()
        cacheDir.mkdirs()
        
        val bundletoolUrl = getLatestBundletoolUrl()
        val fileName = bundletoolUrl.substringAfterLast("/")
        val versionedFile = File(cacheDir, fileName)
        val symlinkFile = File(cacheDir, "bundletool.jar")
        
        if (!versionedFile.exists()) {
            val url = URL(bundletoolUrl)
            progressIndicator.text = InstallerBundle.message("settings.downloading.bundletool") + " ($fileName)..."
            
            try {
                url.openStream().use { input ->
                    input.copyTo(versionedFile.outputStream())
                }
            } catch (e: Exception) {
                if (versionedFile.exists()) {
                    versionedFile.delete()
                }
                throw e
            }
        }
        
        try {
            val link = symlinkFile.toPath()
            val target = Paths.get(versionedFile.name)
            
            if (Files.exists(link, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                Files.delete(link)
            }
            
            try {
                Files.createSymbolicLink(link, target)
            } catch (e: UnsupportedOperationException) {
                LOG.warn("Symlinks not supported, copying file instead: ${e.message}")
                Files.copy(versionedFile.toPath(), link, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                LOG.warn("Failed to create symlink, copying file instead: ${e.message}")
                Files.copy(versionedFile.toPath(), link, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            LOG.error("Failed to update bundletool.jar link/copy", e)
        }
        
        return symlinkFile
    }

    private fun getLatestBundletoolUrl(): String {
        try {
            val apiUrl = "https://api.github.com/repos/google/bundletool/releases/latest"
            return HttpRequests.request(apiUrl)
                .userAgent("ApkInstaller-Plugin")
                .connect { request ->
                    val response = request.readString()
                    val json = JsonParser.parseString(response).asJsonObject
                    val assets = json.getAsJsonArray("assets")
                    for (asset in assets) {
                        val name = asset.asJsonObject.get("name").asString
                        if (name.startsWith("bundletool-all-") && name.endsWith(".jar")) {
                            return@connect asset.asJsonObject.get("browser_download_url").asString
                        }
                    }
                    throw Exception("Bundletool JAR not found in assets")
                }
        } catch (e: Exception) {
            LOG.warn("Failed to get latest bundletool via API: ${e.message}")
        }

        try {
            val latestUrl = "https://github.com/google/bundletool/releases/latest"
            val connection = URL(latestUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connect()
            val redirectUrl = connection.getHeaderField("Location")
            if (redirectUrl != null) {
                val tag = redirectUrl.substringAfterLast("/")
                return "https://github.com/google/bundletool/releases/download/$tag/bundletool-all-$tag.jar"
            }
        } catch (e: Exception) {
            LOG.warn("Failed to get latest bundletool via fallback: ${e.message}")
        }

        return "https://github.com/google/bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar"
    }
    
    private fun getCacheDir(): File {
        return File(System.getProperty("user.home"), ".apk-installer/bundletool")
    }
    
    private fun getDefaultCachePath(): String {
        return File(getCacheDir(), "bundletool.jar").absolutePath
    }
}
