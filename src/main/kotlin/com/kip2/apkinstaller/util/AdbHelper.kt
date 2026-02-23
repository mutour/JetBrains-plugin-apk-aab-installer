package com.kip2.apkinstaller.util

import java.io.File

object AdbHelper {
    fun getAdbPath(): String? {
        return AdbLocator().findAdb().path
    }

    fun isValidAdb(path: String): Boolean {
        val file = File(path)
        return file.exists() && file.canExecute()
    }
}
