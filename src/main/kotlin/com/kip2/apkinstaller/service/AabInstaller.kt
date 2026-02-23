package com.kip2.apkinstaller.service

import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.util.AdbLocator
import com.kip2.apkinstaller.util.BundletoolHelper
import com.intellij.openapi.progress.ProgressIndicator
import java.io.File

class AabInstaller(
    private val adbLocator: AdbLocator = AdbLocator(),
    private val bundletoolHelper: BundletoolHelper = BundletoolHelper()
) {

    fun install(aabFile: File, devices: List<Device>, indicator: ProgressIndicator): List<ApkInstaller.InstallResult> {
        val adbResult = adbLocator.findAdb()
        val adbPath = adbResult.path
            ?: throw IllegalStateException("ADB not found")

        val bundletoolResult = bundletoolHelper.getBundletoolPath()
            ?: throw IllegalStateException("Bundletool not found. Please configure in settings.")

        val results = mutableListOf<ApkInstaller.InstallResult>()

        devices.forEachIndexed { index, device ->
            if (indicator.isCanceled) return results

            indicator.text = "Building APKS for ${device.name}..."
            indicator.fraction = (index).toDouble() / devices.size * 0.5

            val apksFile = buildApks(bundletoolResult, aabFile, device)

            indicator.text = "Installing to ${device.name}..."
            indicator.fraction = 0.5 + (index).toDouble() / devices.size * 0.5

            val result = installApks(adbPath, apksFile, device)
            results.add(result)

            apksFile.delete()
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

    private fun installApks(adbPath: String, apksFile: File, device: Device): ApkInstaller.InstallResult {
        val process = ProcessBuilder(
            adbPath, "-s", device.id, "install-multiple",
            "-r", apksFile.absolutePath
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
