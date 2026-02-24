package com.kip2.apkinstaller.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.kip2.apkinstaller.InstallerBundle
import com.kip2.apkinstaller.service.DeviceManager
import com.kip2.apkinstaller.service.ApkInstaller
import com.kip2.apkinstaller.service.AabInstaller
import com.kip2.apkinstaller.ui.DeviceSelectionDialog
import com.kip2.apkinstaller.ui.AabInstallDialog
import com.kip2.apkinstaller.service.GradleSigningService
import com.kip2.apkinstaller.ui.compose.AabInstallOptions

class InstallAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = getVirtualFile(e)
        val isApkOrAab = file?.extension?.lowercase() in listOf("apk", "aab")

        // 让菜单项始终可见（或者至少在选中了文件的情况下可见），但根据类型决定是否启用
//        e.presentation.isVisible = file != null
//        e.presentation.isEnabled = isApkOrAab
        // 只对指定文件显示
        e.presentation.isVisible = isApkOrAab
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = getVirtualFile(e) ?: run {
            showError(project, "No file selected")
            return
        }

        val extension = file.extension?.lowercase() ?: return
        if (extension !in listOf("apk", "aab")) return

        // Use Task.Modal to fetch devices to prevent EDT freeze
        com.intellij.openapi.progress.ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Modal(project, "Scanning for Devices...", true) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                indicator.isIndeterminate = true
                val deviceManager = DeviceManager()
                val devices = try {
                    deviceManager.getDevices()
                } catch (ex: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        showError(project, "Failed to get devices: ${ex.message}")
                    }
                    return
                }

                if (devices.isEmpty()) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        showError(project, "No devices connected")
                    }
                    return
                }

                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    handleDevices(project, file, devices, extension)
                }
            }
        })
    }

    private fun handleDevices(project: Project, file: VirtualFile, devices: List<com.kip2.apkinstaller.model.Device>, extension: String) {
        if (extension == "aab") {
            val bundletoolPath = com.kip2.apkinstaller.util.BundletoolHelper().getBundletoolPath()
            if (bundletoolPath == null) {
                val result = com.intellij.openapi.ui.Messages.showOkCancelDialog(
                    project,
                    "Bundletool is required for AAB installation. Would you like to configure it in settings?",
                    "Bundletool Not Found",
                    "Go to Settings",
                    "Cancel",
                    com.intellij.openapi.ui.Messages.getErrorIcon()
                )
                if (result == com.intellij.openapi.ui.Messages.OK) {
                    com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(project, "Apk/Aab Installer")
                }
                return
            }
        }

        val apkFile = file.toNioPath().toFile()
        val isAab = extension == "aab"

        var aabOptions: AabInstallOptions? = null
        var finalTargetDevices = emptyList<com.kip2.apkinstaller.model.Device>()

        if (isAab) {
            // Load saved configs
            val signingService = com.kip2.apkinstaller.settings.ProjectSigningSettings.getInstance(project)
            val configs = signingService.getConfigs()

            val dialog = AabInstallDialog(project, devices, configs)
            if (!dialog.showAndGet()) return
            aabOptions = dialog.installOptions ?: return
            finalTargetDevices = aabOptions.selectedDevices
        } else {
            // If only one device, skip selection dialog
            if (devices.size == 1) {
                finalTargetDevices = devices
            } else {
                val dialog = DeviceSelectionDialog(project, devices)
                if (!dialog.showAndGet()) return
                finalTargetDevices = dialog.getSelectedDevices()
            }
        }

        if (finalTargetDevices.isEmpty()) return

        com.intellij.openapi.progress.ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(
            project,
            "Installing package...",
            true,
            com.intellij.openapi.progress.PerformInBackgroundOption.DEAF
        ) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                val results = try {
                    if (isAab) {
                        AabInstaller().install(apkFile, aabOptions!!, indicator)
                    } else {
                        ApkInstaller().install(apkFile, finalTargetDevices, indicator)
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

    private fun getVirtualFile(e: AnActionEvent): VirtualFile? {
        // 1. 尝试标准 VIRTUAL_FILE 键
        e.getData(CommonDataKeys.VIRTUAL_FILE)?.let { return it }

        // 2. 尝试 VIRTUAL_FILE_ARRAY (多选情况)
        e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.firstOrNull()?.let { return it }

        // 3. 尝试 PSI_FILE (在 Android 视图中通常更可靠)
        e.getData(CommonDataKeys.PSI_FILE)?.virtualFile?.let { return it }

        // 4. 尝试 PSI_ELEMENT (逻辑节点可能只提供此键)
        e.getData(CommonDataKeys.PSI_ELEMENT)?.let { element ->
            if (element is PsiFile) return element.virtualFile
            if (element is PsiDirectory) return element.virtualFile
        }

        // 5. 尝试 NAVIGATABLE
        val navigatable = e.getData(CommonDataKeys.NAVIGATABLE)
        if (navigatable is PsiFile) return navigatable.virtualFile

        // 6. 尝试从 Editor 获取 (用于 Editor Tab 右键菜单)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            return FileDocumentManager.getInstance().getFile(editor.document)
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
