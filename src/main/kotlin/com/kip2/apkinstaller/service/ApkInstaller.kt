package com.kip2.apkinstaller.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.util.AdbLocator
import com.intellij.openapi.progress.ProgressIndicator
import java.io.File
import java.nio.charset.StandardCharsets

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
        val commandLine = GeneralCommandLine()
            .withExePath(adbPath)
            .withParameters("-s", device.id, "install", "-r", apkFile.absolutePath)
            .withCharset(StandardCharsets.UTF_8) // 明确指定编码，防止 Windows 乱码

        // ExecUtil 帮你处理了 waitFor, stream reading, buffer size 等所有脏活累活
        val output = ExecUtil.execAndGetOutput(commandLine)

        return InstallResult(
            device = device,
            success = output.exitCode == 0,
            // stdout 是标准输出，stderr 是错误输出，ExecUtil 把它们分开了，非常清晰
            output = if (output.exitCode == 0) output.stdout else output.stderr
        )
    }
}
