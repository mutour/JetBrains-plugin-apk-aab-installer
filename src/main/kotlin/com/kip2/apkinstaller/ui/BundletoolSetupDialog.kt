package com.kip2.apkinstaller.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import javax.swing.*

class BundletoolSetupDialog : DialogWrapper(true) {
    private val pathField = TextFieldWithBrowseButton()
    
    init {
        title = "Bundletool Setup"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        panel.add(JLabel("Bundletool is required to install AAB files."))
        panel.add(Box.createVerticalStrut(10))
        panel.add(JLabel("Path to bundletool.jar:"))
        panel.add(pathField)
        
        return panel
    }
    
    fun getPath(): String = pathField.text
}
