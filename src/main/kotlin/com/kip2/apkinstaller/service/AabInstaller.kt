package com.kip2.apkinstaller.service
import com.kip2.apkinstaller.util.AdbLocator
import com.kip2.apkinstaller.util.BundletoolHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.kip2.apkinstaller.model.Device
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
class AabInstaller(
    private val adbLocator: AdbLocator = AdbLocator(),
    private val bundletoolHelper: BundletoolHelper = BundletoolHelper()
) {
    fun install(aabFile: File, devices: List<Device>, indicator: ProgressIndicator): List<ApkInstaller.InstallResult> {
        val adbResult = adbLocator.findAdb()
        val adbPath = adbResult.path 
            ?: throw IllegalStateException("ADB not found")
            ?: throw IllegalStateException("Bundletool not found. Please configure in settings.")
        val results = mutableListOf<ApkInstaller.InstallResult>()
        devices.forEachIndexed { index, device ->
            if (indicator.isCanceled) return results
            indicator.fraction = (index).toDouble() / devices.size * 0.5
            val apksFile = buildApks(bundletoolPath, aabFile, device)
            indicator.text = "Installing to ${device.name}..."
            indicator.fraction = 0.5 + (index).toDouble() / devices.size * 0.5
            val result = installApks(bundletoolPath, adbPath, apksFile, device)
            results.add(result)
        }
        return results
    }
    
    private fun buildApks(bundletoolPath: String, aabFile: File, device: Device): File {
        val outputFile = File.createTempFile("output", ".apks")
        
        val process = ProcessBuilder(
            "java", "-jar", bundletoolPath,
            "build-apks",
            "--bundle=${aabFile.absolutePath}",
            "--output=${outputFile.absolutePath}",
            "--connected-device",
            "--device-id=${device.id}"
        ).redirectErrorStream(true).start()
        
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Failed to build APKS: $output")
        }
        
        return outputFile
    }
    
    private fun installApks(bundletoolPath: String, adbPath: String, apksFile: File, device: Device): ApkInstaller.InstallResult {
        val process = ProcessBuilder(
            "java", "-jar", bundletoolPath,
            "install-apks",
            "--apks=${apksFile.absolutePath}",
            "--device-id=${device.id}",
            "--adb=$adbPath"
        ).redirectErrorStream(true).start()
        
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return ApkInstaller.InstallResult(
            device = device,
            success = exitCode == 0,
            output = output
        )
    }
}
