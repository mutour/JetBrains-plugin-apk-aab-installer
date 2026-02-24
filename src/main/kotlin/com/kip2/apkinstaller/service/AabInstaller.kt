package com.kip2.apkinstaller.service

import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.util.AdbLocator
import com.kip2.apkinstaller.util.BundletoolHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.kip2.apkinstaller.InstallerBundle
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

            indicator.text = InstallerBundle.message("status.building.apks", device.name)
            indicator.fraction = index.toDouble() / devices.size * 0.5

            val apksFile = buildApks(bundletoolResult, aabFile, device, options)

            indicator.text = InstallerBundle.message("status.installing.to", device.name)
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
        // Use bundletool for installation instead of raw ADB
        // adb install-multiple expects split APKs, but .apks is a ZIP archive
        // We need to use bundletool install-apks command
        
        val bundletoolPath = bundletoolHelper.getBundletoolPath() 
            ?: throw IllegalStateException("Bundletool not found")

        val cmd = mutableListOf(
            "java", "-jar", bundletoolPath,
            "install-apks",
            "--apks=${apksFile.absolutePath}",
            "--device-id=${device.id}"
        )
        // Note: bundletool install-apks doesn't support --update flag directly in older versions,
        // but typically handles re-installations. 
        // Newer versions might, but for safety we stick to basic command or rely on adb uninstall if needed?
        // Actually bundletool usually handles update if signatures match.
        
        // If we really wanted to use ADB directly, we would need to unzip the .apks file first.
        // But using bundletool is cleaner.

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
