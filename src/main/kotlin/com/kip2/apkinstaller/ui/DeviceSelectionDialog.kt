package com.kip2.apkinstaller.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.kip2.apkinstaller.model.Device

import com.kip2.apkinstaller.InstallerBundle
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.components.JBCheckBox
import javax.swing.JComponent

import java.awt.Dimension

class DeviceSelectionDialog(
    project: Project,
    private val devices: List<Device>
) : DialogWrapper(project) {

    private var selectedDevices: List<Device> = emptyList()



    private val checkBoxes = devices.map { device ->
        JBCheckBox("${device.model} [${device.id}] (API ${device.apiLevel})", true).apply {
            putClientProperty("device", device)
        }
    }

    init {
        title = InstallerBundle.message("dialog.select.devices.title")
        init()
    }
    override fun createCenterPanel(): JComponent {
        return panel {
            checkBoxes.forEach { cb ->
                row {
                    cell(cb)
                }
            }
        }.apply {
            preferredSize = Dimension(400, 300)
        }
    }

    override fun doOKAction() {
        selectedDevices = checkBoxes.filter { it.isSelected }.map { it.getClientProperty("device") as Device }
        super.doOKAction()
    }

    fun getSelectedDevices(): List<Device> = selectedDevices
}
