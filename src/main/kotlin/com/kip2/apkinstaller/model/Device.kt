package com.kip2.apkinstaller.model

enum class DeviceState {
    ONLINE, OFFLINE, UNAUTHORIZED
}

data class Device(
    val id: String,
    val name: String,
    val state: DeviceState
)
