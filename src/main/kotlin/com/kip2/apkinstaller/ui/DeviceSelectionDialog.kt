package com.kip2.apkinstaller.ui

import com.intellij.openapi.ui.DialogWrapper
import com.kip2.apkinstaller.model.Device
import javax.swing.*
import java.awt.*

class DeviceSelectionDialog(private val devices: List<Device>) : DialogWrapper(true) {
    private val selection = mutableMapOf<String, Boolean>()
    private val checkBoxes = mutableListOf<JCheckBox>()
    
    init {
        title = "Select Target Devices"
        devices.forEach { selection[it.id] = false }
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        
        val selectAll = JCheckBox("Select All")
        selectAll.addActionListener {
            val isSelected = selectAll.isSelected
            checkBoxes.forEach { it.isSelected = isSelected }
            devices.forEach { selection[it.id] = isSelected }
        }
        
        val listPanel = JPanel(GridLayout(0, 1, 5, 5))
        devices.forEach { device ->
            val checkBox = JCheckBox("${device.name} (${device.id})")
            checkBox.addActionListener {
                selection[device.id] = checkBox.isSelected
                if (!checkBox.isSelected) {
                    selectAll.isSelected = false
                } else {
                    selectAll.isSelected = checkBoxes.all { it.isSelected }
                }
            }
            checkBoxes.add(checkBox)
            listPanel.add(checkBox)
        }
        
        panel.add(selectAll, BorderLayout.NORTH)
        panel.add(JScrollPane(listPanel), BorderLayout.CENTER)
        
        return panel
    }
    
    fun getSelectedDevices(): List<Device> {
        return devices.filter { selection[it.id] == true }
    }
}
