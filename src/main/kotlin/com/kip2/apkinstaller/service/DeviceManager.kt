package com.kip2.apkinstaller.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.model.DeviceState
import com.kip2.apkinstaller.util.AdbLocator

class DeviceManager(private val adbLocator: AdbLocator = AdbLocator()) {
    
    fun getDevices(): List<Device> {
        val adbPath = adbLocator.findAdb().path ?: return emptyList()

        val commandLine = GeneralCommandLine(adbPath, "devices")
        val output = ExecUtil.execAndGetOutput(commandLine)
        val devices = mutableListOf<Device>()
        output.stdoutLines.drop(1).filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val id = parts[0]
                val state = when (parts[1]) {
                    "device" -> DeviceState.ONLINE
                    "offline" -> DeviceState.OFFLINE
                    else -> DeviceState.UNAUTHORIZED
                }

                if (state == DeviceState.ONLINE) {
                    val model = getDeviceProperty(adbPath, id, "ro.product.model") ?: "Unknown"
                    val apiLevel = getDeviceProperty(adbPath, id, "ro.build.version.sdk") ?: "Unknown"
                    devices.add(Device(id, id, model, apiLevel, state))
                } else {
                    devices.add(Device(id, id, "Unknown", "Unknown", state))
                }
            }
        }
        return devices
    }

    private fun getDeviceProperty(adbPath: String, deviceId: String, property: String): String? {
        val commandLine = GeneralCommandLine(adbPath, "-s", deviceId, "shell", "getprop", property)
        val output = ExecUtil.execAndGetOutput(commandLine)
        return if (output.exitCode == 0) output.stdout.trim() else null
    }
}
