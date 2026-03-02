package com.kip2.apkinstaller.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.kip2.apkinstaller.InstallerBundle
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.service.AabInstaller
import com.kip2.apkinstaller.service.ApkInstaller
import com.kip2.apkinstaller.service.DeviceManager
import com.kip2.apkinstaller.service.SigningConfigProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.kip2.apkinstaller.service.SigningConfig
import com.kip2.apkinstaller.settings.PluginSettings
import com.kip2.apkinstaller.ui.AabInstallDialog
import com.kip2.apkinstaller.ui.DeviceSelectionDialog
import com.kip2.apkinstaller.ui.AabInstallOptions
import com.kip2.apkinstaller.util.AdbLocator
import com.kip2.apkinstaller.util.BundletoolHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.intellij.openapi.Disposable

import java.io.File


class MyToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowPanel = MyToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(toolWindowPanel, InstallerBundle.message("settings.display.name"), false)
        toolWindow.contentManager.addContent(content)
    }
}

class MyToolWindowPanel(private val project: Project) : SimpleToolWindowPanel(false, true), Disposable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val settings = PluginSettings.getInstance()
    
    init {
        val panel = panel {
            group(InstallerBundle.message("settings.environment.title")) {
                row(InstallerBundle.message("settings.adb.path.label")) {
                    val adbField = com.intellij.openapi.ui.TextFieldWithBrowseButton()
                    adbField.addBrowseFolderListener(null, null, project, FileChooserDescriptorFactory.createSingleFileDescriptor())
                    cell(adbField)
                        .bindText(settings::adbPath)
                        .align(AlignX.FILL)
                }
                row(InstallerBundle.message("settings.detected.label")) {
                    val adbLocator = AdbLocator()
                    val adbPaths = adbLocator.findAdbPaths()
                    adbPaths.take(3).forEach { path ->
                        cell(ActionLink(path) { settings.adbPath = path })
                    }
                }
                row(InstallerBundle.message("settings.bundletool.path.label")) {
                    val btField = com.intellij.openapi.ui.TextFieldWithBrowseButton()
                    btField.addBrowseFolderListener(null, null, project, FileChooserDescriptorFactory.createSingleFileDescriptor())
                    cell(btField)
                        .bindText(settings::bundletoolPath)
                        .align(AlignX.FILL)
                    button(InstallerBundle.message("settings.download.button")) {
                        downloadBundletool()
                    }
                }
                row(InstallerBundle.message("settings.detected.label")) {
                    val bundletoolHelper = BundletoolHelper()
                    val btPaths = bundletoolHelper.findBundletoolPaths()
                    btPaths.take(3).forEach { path ->
                        cell(ActionLink(path) { settings.bundletoolPath = path })
                    }
                }
            } // Removed .align(AlignX.FILL) here as group handles it or it's not applicable to group builder directly like this in some versions, but let's see. Actually, Align.FILL on the row or component is better.
            
            group(InstallerBundle.message("toolwindow.install.title")) {
                row {
                    label(InstallerBundle.message("toolwindow.select.file.text")).bold()
                }
                row {
                    button("Select File") {
                        selectAndInstallFile()
                    }
                }
                row {
                    comment(InstallerBundle.message("toolwindow.supports.text"))
                }
            }
        }
        panel.registerValidators(this@MyToolWindowPanel)
        
        setContent(ScrollPaneFactory.createScrollPane(panel))
    }
    
    private fun selectAndInstallFile() {
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle("Select APK or AAB")
            .withDescription("Choose an .apk or .aab file to install")
                .withFileFilter { file -> file.extension?.lowercase() in listOf("apk", "aab") }
        val file = FileChooser.chooseFile(descriptor, project, null)
        if (file != null) {
            handleFileInstall(project, File(file.path))
        }
    }

    
    private fun downloadBundletool() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, InstallerBundle.message("settings.downloading.bundletool"), true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val file = BundletoolHelper().downloadBundletool(indicator)
                    ApplicationManager.getApplication().invokeLater {
                        settings.bundletoolPath = file.absolutePath
                        // Need a way to refresh the UI if bound properties change manually
                    }
                } catch (e: Exception) {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("ApkInstaller.NotificationGroup")
                        .createNotification(InstallerBundle.message("settings.download.failed"), e.message ?: "Unknown error", NotificationType.ERROR)
                        .notify(project)
                }
            }
        })
    }
    
    override fun dispose() {
        scope.cancel()

    }
}


private fun handleFileInstall(project: Project, file: File) {
    if (!file.exists()) return
    val extension = file.extension.lowercase()
    if (extension !in listOf("apk", "aab")) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ApkInstaller.NotificationGroup")
            .createNotification(InstallerBundle.message("apk.installer.info"), InstallerBundle.message("status.unsupported.file", extension), NotificationType.ERROR)
            .notify(project)
        return
    }


    val virtualFile = VfsUtil.findFileByIoFile(file, true) ?: return

    val deviceManager = DeviceManager()
    val devices = try {
        deviceManager.getDevices()
    } catch (ex: Exception) {
        showError(project, InstallerBundle.message("status.install.failed", ex.message ?: "Unknown error"))
        return
    }
    if (devices.isEmpty()) {
        showError(project, InstallerBundle.message("status.no.devices"))
        return
    }

    if (extension == "aab") {
        val bundletoolPath = BundletoolHelper().getBundletoolPath()
        if (bundletoolPath == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("ApkInstaller.NotificationGroup")
                .createNotification(InstallerBundle.message("status.bundletool.required.title"), InstallerBundle.message("status.bundletool.required.msg"), NotificationType.ERROR)
                .notify(project)
            return
        }
    }

    var aabOptions: AabInstallOptions? = null
    var finalTargetDevices = emptyList<Device>()

    if (extension == "aab") {
        val virtualFileForAab = VfsUtil.findFileByIoFile(file, true)
        val module = if (virtualFileForAab != null) com.intellij.openapi.module.ModuleUtilCore.findModuleForFile(virtualFileForAab, project) else null
        
        val configs = mutableListOf<SigningConfig>()
        if (module != null) {
            val ep = ExtensionPointName.create<SigningConfigProvider>("com.kip2.apkinstaller.signingConfigProvider")
            ep.extensionList.forEach { provider ->
                configs.addAll(provider.getSigningConfigs(project, module))
            }
        }

        val dialog = AabInstallDialog(project, devices, configs, virtualFile.path)
        if (!dialog.showAndGet()) return
        aabOptions = dialog.installOptions ?: return
        finalTargetDevices = aabOptions.selectedDevices
    } else {
        val dialog = DeviceSelectionDialog(project, devices)
        if (!dialog.showAndGet()) return
        finalTargetDevices = dialog.getSelectedDevices()
    }

    if (finalTargetDevices.isEmpty()) return

    ProgressManager.getInstance().run(object : Task.Backgroundable(project, InstallerBundle.message("status.installing"), true) {
        override fun run(indicator: ProgressIndicator) {
            val results = try {
                if (extension == "aab") {
                    AabInstaller().install(file, aabOptions!!, indicator)
                } else {
                    ApkInstaller().install(file, finalTargetDevices, indicator)
                }
            } catch (ex: Exception) {
                showError(project, InstallerBundle.message("status.install.failed", ex.message ?: "Unknown error"))
                return
            }

            if (indicator.isCanceled) return
            val successCount = results.count { it.success }
            val failedResults = results.filter { !it.success }
            if (failedResults.isEmpty()) {
                showInfo(project, InstallerBundle.message("status.success", successCount))
            } else {
                val errorMsg = failedResults.joinToString("\n") { "${it.device.name}: ${it.output.trim()}" }
                showError(project, InstallerBundle.message("status.partial.failure", successCount, failedResults.size, errorMsg))
            }
        }
    })
}

private fun showError(project: Project, message: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("ApkInstaller.NotificationGroup")
        .createNotification(InstallerBundle.message("apk.installer.error"), message, NotificationType.ERROR)
        .notify(project)
}

private fun showInfo(project: Project, message: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("ApkInstaller.NotificationGroup")
        .createNotification(InstallerBundle.message("apk.installer.info"), message, NotificationType.INFORMATION)
        .notify(project)
}
