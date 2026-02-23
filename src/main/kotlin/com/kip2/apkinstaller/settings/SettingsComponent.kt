package com.kip2.apkinstaller.settings

import com.intellij.ui.components.JBLabel
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class SettingsComponent {
    private val adbComboBox = ComboBox<String>()
    private val bundletoolComboBox = ComboBox<String>()
    
    val panel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("ADB Path:"), adbComboBox, 1, false)
        .addLabeledComponent(JBLabel("Bundletool Path:"), bundletoolComboBox, 1, false)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    init {
        adbComboBox.isEditable = true
        bundletoolComboBox.isEditable = true
    }

    fun getAdbPath(): String = adbComboBox.selectedItem?.toString() ?: ""
    
    fun setAdbPath(path: String) {
        adbComboBox.selectedItem = path
    }

    fun getBundletoolPath(): String = bundletoolComboBox.selectedItem?.toString() ?: ""

    fun setBundletoolPath(path: String) {
        bundletoolComboBox.selectedItem = path
    }
}
