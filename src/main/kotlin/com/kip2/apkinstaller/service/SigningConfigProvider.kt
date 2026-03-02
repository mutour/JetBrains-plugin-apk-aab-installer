package com.kip2.apkinstaller.service

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

data class SigningConfig(
    val name: String,
    val storeFile: String?,
    val storePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?,
    val moduleName: String = "unknown"
)

interface SigningConfigProvider {
    fun getSigningConfigs(project: Project, module: Module?): List<SigningConfig>
    fun getAllSigningConfigs(project: Project): List<SigningConfig>
}
