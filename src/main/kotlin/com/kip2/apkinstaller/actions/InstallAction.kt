package com.kip2.apkinstaller.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.kip2.apkinstaller.InstallerBundle
import com.kip2.apkinstaller.service.DeviceManager
import com.kip2.apkinstaller.service.ApkInstaller
import com.kip2.apkinstaller.service.AabInstaller
import com.kip2.apkinstaller.ui.DeviceSelectionDialog

class InstallAction : AnAction(InstallerBundle.message("install.action.text")) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val file = getVirtualFile(e)
        if (file != null) {
            val ext = file.extension?.lowercase()
            e.presentation.isVisible = ext in listOf("apk", "aab")
        } else {
            e.presentation.isVisible = false
        }
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = getVirtualFile(e) ?: run {
            showError(project, "No file selected")
            return
        }
        
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

        val apkFile = file.toNioPath().toFile()
        
        com.intellij.openapi.progress.ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(
            project,
            "Installing package...",
            true,
            com.intellij.openapi.progress.PerformInBackgroundOption.DEAF
        ) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                val results = try {
                    if (apkFile.extension.equals("aab", ignoreCase = true)) {
                        AabInstaller().install(apkFile, targetDevices, indicator)
                    } else {
                        ApkInstaller().install(apkFile, targetDevices, indicator)
                    }
                } catch (ex: Exception) {
                    showError(project, "Installation failed: ${ex.message}")
                    return
                }
                
                if (indicator.isCanceled) return
                
                val successCount = results.count { it.success }
                val failedResults = results.filter { !it.success }
                
                if (failedResults.isEmpty()) {
                    showInfo(project, "Successfully installed to $successCount device(s).")
                } else {
                    val errorMsg = failedResults.joinToString("\n") { "${it.device.name}: ${it.output.trim()}" }
                    showError(project, "Installed to $successCount device(s). Failed on ${failedResults.size} device(s):\n$errorMsg")
                }
            }
        })
    }

    private fun getVirtualFile(e: AnActionEvent): com.intellij.openapi.vfs.VirtualFile? {
        // 1. Try standard VIRTUAL_FILE key
        var file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (file != null) return file

        // 2. Try from Editor (for Editor Tab context menu)
        val editor = e.getData(LangDataKeys.EDITOR)
        if (editor != null) {
            val document = editor.document
            file = FileDocumentManager.getInstance().getFile(document)
            if (file != null) return file
        }

        // 3. Try VIRTUAL_FILE_ARRAY (fallback for older versions or different contexts)
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (!files.isNullOrEmpty()) {
            return files.firstOrNull()
        }

        return null
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
