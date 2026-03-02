package com.kip2.apkinstaller.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.kip2.apkinstaller.InstallerBundle
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.service.SigningConfig
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.openapi.extensions.ExtensionPointName
import com.kip2.apkinstaller.service.SigningConfigProvider
import com.kip2.apkinstaller.settings.ProjectSigningSettings
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.SimpleListCellRenderer
import java.awt.Dimension

data class AabInstallOptions(
    val selectedDevices: List<Device>,
    val signingConfig: SigningConfig,
    val isUniversalMode: Boolean,
    val localTesting: Boolean,
    val updateExisting: Boolean,
    val allowIncompatibleUpdate: Boolean = false
)

class AabInstallDialog(
    private val project: Project,
    private val devices: List<Device>,
    private val detectedConfigs: List<SigningConfig>,
    private val aabFilePath: String
) : DialogWrapper(project) {

    var installOptions: AabInstallOptions? = null
        private set



    private val checkBoxes = devices.map { device ->
        JBCheckBox("${device.model} [${device.id}] (API ${device.apiLevel})", true).apply {
            putClientProperty("device", device)
        }
    }
    private val configsModel = DefaultComboBoxModel<SigningConfig>().apply {
        detectedConfigs.forEach { addElement(it) }
    }
    private var isUniversalMode = false
    private var localTesting = false
    private var updateExisting = true
    private var allowIncompatibleUpdate = false
    private val storeFileField = TextFieldWithBrowseButton()
    private val storePasswordField = JBTextField()
    private val keyAliasField = JBTextField()
    private val keyPasswordField = JBTextField()
    init {
        title = InstallerBundle.message("dialog.install.aab.title")
        setOKButtonText(InstallerBundle.message("dialog.install.button"))
        val savedConfigs = ProjectSigningSettings.getInstance(project).getConfigs()
        if (detectedConfigs.isEmpty() && savedConfigs.isNotEmpty()) {
            savedConfigs.forEach { configsModel.addElement(it) }
            val first = savedConfigs.first()
            storeFileField.text = first.storeFile ?: ""
            storePasswordField.text = first.storePassword ?: ""
            keyAliasField.text = first.keyAlias ?: ""
            keyPasswordField.text = first.keyPassword ?: ""
        } else if (detectedConfigs.isNotEmpty()) {
            val first = detectedConfigs.first()
            storeFileField.text = first.storeFile ?: ""
            storePasswordField.text = first.storePassword ?: ""
            keyAliasField.text = first.keyAlias ?: ""
            keyPasswordField.text = first.keyPassword ?: ""
        }
        init()
    }
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label(InstallerBundle.message("aab.install.select.devices")).bold()
            }
            row {
                panel {
                    checkBoxes.forEach { cb ->
                        row { cell(cb) }
                    }
                }.align(AlignY.TOP)
            }
            
            separator()
            row(InstallerBundle.message("signing.form.title")) {
                comboBox(configsModel, SimpleListCellRenderer.create("") { it.name }).onChanged { cb ->
                    val config = cb.selectedItem as? SigningConfig ?: return@onChanged
                    storeFileField.text = config.storeFile ?: ""
                    storePasswordField.text = config.storePassword ?: ""
                    keyAliasField.text = config.keyAlias ?: ""
                    keyPasswordField.text = config.keyPassword ?: ""
                }.align(AlignX.FILL)
                button(InstallerBundle.message("signing.form.detect.button")) {
                    val virtualFile = LocalFileSystem.getInstance().findFileByPath(aabFilePath)
                    val module = if (virtualFile != null) ModuleUtilCore.findModuleForFile(virtualFile, project) else null
                    
                    val ep = ExtensionPointName.create<SigningConfigProvider>("com.kip2.apkinstaller.signingConfigProvider")
                    val detected = mutableListOf<SigningConfig>()
                    ep.extensionList.forEach { provider ->
                        if (module != null) {
                            detected.addAll(provider.getSigningConfigs(project, module))
                        } else {
                            detected.addAll(provider.getAllSigningConfigs(project))
                        }
                    }
                    
                    configsModel.removeAllElements()
                    detected.forEach { configsModel.addElement(it) }
                    ProjectSigningSettings.getInstance(project).saveConfigs(detected)
                }
            }
            row(InstallerBundle.message("signing.form.keystore.path")) {
                cell(storeFileField).align(AlignX.FILL).applyToComponent {
                    addBrowseFolderListener(null, null, project, FileChooserDescriptorFactory.createSingleFileDescriptor())
                }
            }
                    row(InstallerBundle.message("signing.form.keystore.password")) { cell(storePasswordField).align(AlignX.FILL) }
            row(InstallerBundle.message("signing.form.key.alias")) {
                cell(keyAliasField).align(AlignX.FILL)
            }
                    row(InstallerBundle.message("signing.form.key.password")) { cell(keyPasswordField).align(AlignX.FILL) }
            separator()
            row {
                label(InstallerBundle.message("aab.install.parameters")).bold()
            }
            row {
                checkBox(InstallerBundle.message("aab.install.mode.universal")).bindSelected(::isUniversalMode)
            }
            row {
                checkBox(InstallerBundle.message("aab.install.local.testing")).bindSelected(::localTesting)
            }
            row {
                checkBox(InstallerBundle.message("aab.install.update")).bindSelected(::updateExisting)
            }
            row {
                checkBox(InstallerBundle.message("aab.install.allow.incompatible.update")).bindSelected(::allowIncompatibleUpdate)
            }
        }.apply {
            preferredSize = Dimension(750, 550)
        }
    }

    override fun doOKAction() {
        val selectedDevices = checkBoxes.filter { it.isSelected }.map { it.getClientProperty("device") as Device }
        val selectedConfig = configsModel.selectedItem as? SigningConfig
        val config = SigningConfig(
            name = selectedConfig?.name ?: "Manual",
            storeFile = storeFileField.text,
            storePassword = storePasswordField.text,
            keyAlias = keyAliasField.text,
            keyPassword = keyPasswordField.text,
            moduleName = selectedConfig?.moduleName ?: "User"
        )
        installOptions = AabInstallOptions(selectedDevices, config, isUniversalMode, localTesting, updateExisting, allowIncompatibleUpdate)
        // Save manual config for future use
        ProjectSigningSettings.getInstance(project).addConfig(config)
        super.doOKAction()
    }
}
