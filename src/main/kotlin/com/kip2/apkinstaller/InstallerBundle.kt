package com.kip2.apkinstaller

import java.util.ResourceBundle

object InstallerBundle {
    private val bundle = ResourceBundle.getBundle("messages.InstallerBundle")
    
    fun message(key: String): String = bundle.getString(key)
}
