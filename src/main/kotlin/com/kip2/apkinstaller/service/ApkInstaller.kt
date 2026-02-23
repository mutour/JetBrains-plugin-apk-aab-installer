package com.kip2.apkinstaller.service

import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.util.AdbLocator
import com.intellij.openapi.progress.ProgressIndicator
import java.io.File

class ApkInstaller(
    private val adbLocator: AdbLocator = AdbLocator()
) {
    data class InstallResult(
        val device: Device,
        val success: Boolean,
        val output: String
    )
    
    fun install(apkFile: File, devices: List<Device>, indicator: ProgressIndicator): List<InstallResult> {
        val adbResult = adbLocator.findAdb()
        val adbPath = adbResult.path 
            ?: throw IllegalStateException("ADB not found")
        
        val results = mutableListOf<InstallResult>()
        
        devices.forEachIndexed { index, device ->
            if (indicator.isCanceled) return results
            
            indicator.text = "Installing to ${device.name} (${device.id})..."
            indicator.fraction = (index).toDouble() / devices.size
            
            val result = installToDevice(adbPath, apkFile, device)
            results.add(result)
        }
        indicator.fraction = 1.0
        
        return results
    }
    
    private fun installToDevice(adbPath: String, apkFile: File, device: Device): InstallResult {
        val process = ProcessBuilder(
            adbPath, "-s", device.id, "install", "-r", apkFile.absolutePath
        ).redirectErrorStream(true).start()
        
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        return InstallResult(
            device = device,
            success = exitCode == 0,
            output = output
        )
    }
}
