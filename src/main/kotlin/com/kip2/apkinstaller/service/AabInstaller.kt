package com.kip2.apkinstaller.service

import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.util.AdbLocator
import com.kip2.apkinstaller.util.BundletoolHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.kip2.apkinstaller.ui.compose.AabInstallOptions
import java.io.File

class AabInstaller(
    private val adbLocator: AdbLocator = AdbLocator(),
    private val bundletoolHelper: BundletoolHelper = BundletoolHelper()
) {

    fun install(
        aabFile: File,
        options: AabInstallOptions,
        indicator: ProgressIndicator
    ): List<ApkInstaller.InstallResult> {
        val adbResult = adbLocator.findAdb()
        val adbPath = adbResult.path ?: throw IllegalStateException("ADB not found")

        val bundletoolResult = bundletoolHelper.getBundletoolPath()
            ?: throw IllegalStateException("Bundletool not found. Please configure in settings.")

        val results = mutableListOf<ApkInstaller.InstallResult>()
        val devices = options.selectedDevices

        devices.forEachIndexed { index, device ->
            if (indicator.isCanceled) return results

            indicator.text = "Building APKS for ${device.name}..."
            indicator.fraction = index.toDouble() / devices.size * 0.5

            val apksFile = buildApks(bundletoolResult, aabFile, device, options)

            indicator.text = "Installing to ${device.name}..."
            indicator.fraction = 0.5 + index.toDouble() / devices.size * 0.5

            // Ensure file is deleted even if installation fails
            try {
                val result = installApks(adbPath, apksFile, device, options.updateExisting)
                results.add(result)
            } finally {
                if (apksFile.exists()) {
                    apksFile.delete()
                }
            }
        }
        return results
    }

    private fun buildApks(
        bundletoolPath: String,
        aabFile: File,
        device: Device,
        options: AabInstallOptions
    ): File {
        val outputFile = File.createTempFile("output", ".apks")
        // Bundletool expects the file to NOT exist, so we delete it immediately after creating the handle
        outputFile.delete()
        
        val cmd = mutableListOf(
            "java", "-jar", bundletoolPath,
            "build-apks",
            "--bundle=${aabFile.absolutePath}",
            "--output=${outputFile.absolutePath}",
            "--connected-device",
            "--device-id=${device.id}"
        )

        if (options.isUniversalMode) {
            cmd.add("--mode=universal")
        }
        if (options.localTesting) {
            cmd.add("--local-testing")
        }

        val signing = options.signingConfig
        if (!signing.storeFile.isNullOrBlank()) {
            cmd.add("--ks=${signing.storeFile}")
            cmd.add("--ks-pass=pass:${signing.storePassword}")
            cmd.add("--ks-key-alias=${signing.keyAlias}")
            
            val kp = if (signing.keyPassword.isNullOrBlank()) signing.storePassword else signing.keyPassword
            cmd.add("--key-pass=pass:${kp}")
        }

        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            // Cleanup on failure
            if (outputFile.exists()) {
                outputFile.delete()
            }
            throw RuntimeException("Failed to build APKS: $output")
        }

        return outputFile
    }

    private fun installApks(adbPath: String, apksFile: File, device: Device, update: Boolean): ApkInstaller.InstallResult {
        val cmd = mutableListOf(adbPath, "-s", device.id, "install-multiple")
        if (update) {
            cmd.add("-r")
        }
        cmd.add(apksFile.absolutePath)

        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        return ApkInstaller.InstallResult(
            device = device,
            success = exitCode == 0,
            output = output
        )
    }
}
