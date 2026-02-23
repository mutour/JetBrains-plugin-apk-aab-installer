package com.kip2.apkinstaller.service

import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.model.DeviceState
import com.kip2.apkinstaller.util.AdbLocator
import java.io.BufferedReader
import java.io.InputStreamReader

class DeviceManager(private val adbLocator: AdbLocator = AdbLocator()) {
    
    fun getDevices(): List<Device> {
        val adbResult = adbLocator.findAdb()
        val adbPath = adbResult.path 
            ?: throw IllegalStateException("ADB not found: ${adbResult.source}")
        
        val process = ProcessBuilder(adbPath, "devices")
            .redirectErrorStream(true)
            .start()
        
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val devices = mutableListOf<Device>()
        
        reader.useLines { lines ->
            lines.drop(1) // Skip "List of devices attached"
                .filter { it.isNotBlank() }
                .forEach { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val id = parts[0]
                        val stateStr = parts[1]
                        val state = when (stateStr) {
                            "device" -> DeviceState.ONLINE
                            "offline" -> DeviceState.OFFLINE
                            else -> DeviceState.UNAUTHORIZED
                        }
                        devices.add(Device(id = id, name = id, state = state))
                    }
                }
        }
        
        process.waitFor()
        
        return devices
    }
}
