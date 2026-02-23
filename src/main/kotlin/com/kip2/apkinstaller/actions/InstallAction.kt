package com.kip2.apkinstaller.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.kip2.apkinstaller.InstallerBundle
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.service.DeviceManager
import com.kip2.apkinstaller.ui.DeviceSelectionDialog

class InstallAction : AnAction(InstallerBundle.message("install.action.text")) {
    
    override fun update(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val visible = files?.any { f -> 
            f.extension?.lowercase() in listOf("apk", "aab")
        } ?: false
        e.presentation.isVisible = visible
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        val extension = file.extension?.lowercase() ?: return
        if (extension !in listOf("apk", "aab")) return
        
        val deviceManager = DeviceManager()
        val devices = try {
            deviceManager.getDevices()
        } catch (ex: Exception) {
            showError(project, "Failed to get devices: ${ex.message}")
            return
        }
        
        if (devices.isEmpty()) {
            showError(project, "No devices connected")
            return
        }
        
        val targetDevices = if (devices.size == 1) {
            devices
        } else {
            val dialog = DeviceSelectionDialog(devices)
            if (!dialog.showAndGet()) return
            dialog.getSelectedDevices()
        }
        
        if (targetDevices.isEmpty()) return

        // TODO: Perform installation (Task 8)
        showInfo(project, "Selected devices: ${targetDevices.joinToString { it.name }}")
    }
    
    private fun showError(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ApkInstaller.NotificationGroup")
            .createNotification("ApkInstaller Error", message, NotificationType.ERROR)
            .notify(project)
    }

    private fun showInfo(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ApkInstaller.NotificationGroup")
            .createNotification("ApkInstaller", message, NotificationType.INFORMATION)
            .notify(project)
    }
}
