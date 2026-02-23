# Apk/Aab Installer 插件实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 开发一个 JetBrains IntelliJ 插件，能够从 IDE 直接将 APK/AAB 文件安装到连接的 Android 设备上。

**架构:** 基于 IntelliJ Platform SDK 开发，使用 Kotlin 语言。采用分层架构：UI 层（Action/Dialog）→ 业务层（AdbHelper/DeviceManager）→ 数据层（Settings）。

**技术栈:**
- 语言: Kotlin
- 构建系统: Gradle (使用 org.jetbrains.intellij 插件)
- 最小 IDE 版本: 2021.1 (IntelliJ IDEA)
- 目标 IDE: 兼容所有 JetBrains IDE (IntelliJ IDEA, Android Studio 等)

---

## 阶段 1: 项目脚手架搭建

### Task 1: 初始化 Gradle 项目结构

**文件:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradlew` (shell 脚本)
- Create: `gradlew.bat` (Windows 批处理)
- Create: `src/main/resources/META-INF/plugin.xml`

**Step 1: 创建 settings.gradle.kts**

```kotlin
rootProject.name = "apk-aab-installer"

plugins {
    id("org.jetbrains.intellij") version "1.17.4" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
}
```

**Step 2: 创建 build.gradle.kts**

```kotlin
plugins {
    id("java")
    id("org.jetbrains.intellij")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

group = "com.kip2"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

intellij {
    version.set("2023.2.5")
    type.set("IC") // IC = IntelliJ IDEA Community
}

tasks {
    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("243.*")
    }
}
```

**Step 3: 创建 gradle-wrapper.properties**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.4-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Step 4: 生成 Gradle Wrapper**

Run: `gradle wrapper --gradle-version 8.4`

**Step 5: 创建 plugin.xml**

```xml
<idea-plugin>
    <id>com.kip2.apkinstaller</id>
    <name>Apk/Aab Installer</name>
    <version>1.0.0</version>
    <vendor>KIP2 (Developer: King)</vendor>
    <description>Install APK/AAB files to Android devices directly from IDE</description>
    
    <depends>com.intellij.modules.platform</depends>
    
    <application-components>
    </application-components>
    
    <actions>
    </actions>
</idea-plugin>
```

**Step 6: 验证构建**

Run: `./gradlew buildPlugin`
Expected: 构建成功，生成 `build/distributions/*.Step 7:zip`

** Commit**

```bash
git add .
git commit -m "feat: scaffold JetBrains plugin project with Gradle"
```

---

### Task 2: 创建基础包结构和占位 Action

**文件:**
- Create: `src/main/kotlin/com.kip2.apkinstaller/InstallerBundle.kt`
- Create: `src/main/kotlin/com.kip2.apkinstaller/actions/InstallAction.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: 创建国际化资源 Bundle**

```kotlin
package com.kip2.apkinstaller

import java.util.ResourceBundle

object InstallerBundle {
    private val bundle = ResourceBundle.getBundle("messages.InstallerBundle")
    
    fun message(key: String) = bundle.getString(key)
}
```

**Step 2: 创建 messages.properties**

```properties
# src/main/resources/messages/InstallerBundle.properties
install.action.text=Install to Device(s)
install.action.description=Install APK/AAB to connected Android devices
```

**Step 3: 创建占位 Action**

```kotlin
package com.kip2.apkinstaller.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.kip2.apkinstaller.InstallerBundle

class InstallAction : AnAction(InstallerBundle.message("install.action.text")) {
    override fun actionPerformed(e: AnActionEvent) {
        // TODO: 实现安装逻辑
    }
}
```

**Step 4: 注册 Action 到 plugin.xml**

```xml
<actions>
    <action id="InstallApkAction" 
            class="com.kip2.apkinstaller.actions.InstallAction"
            text="Install to Device(s)"
            description="Install APK/AAB to connected Android devices">
        <add-to-group group-id="ProjectViewPopupMenu" anchor="first"/>
    </action>
</actions>
```

**Step 5: 构建验证**

Run: `./gradlew buildPlugin`
Expected: 成功，插件包生成

**Step 6: Commit**

```bash
git add .
git commit -m "feat: add basic action placeholder and bundle"
```

---

## 阶段 2: ADB 环境探测与设备管理

### Task 3: 实现 ADB 路径探测服务

**文件:**
- Create: `src/main/kotlin/com.kip2.apkinstaller/settings/PluginSettings.kt`
- Create: `src/main/kotlin/com.kip2.apkinstaller/util/AdbLocator.kt`
- Create: `src/main/kotlin/com.kip2.apkinstaller/util/AdbHelper.kt`

**Step 1: 创建 PluginSettings (持久化配置)**

```kotlin
package com.kip2.apkinstaller.settings

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "ApkInstallerSettings",
    storages = [Storage("apk-installer-settings.xml")]
)
class PluginSettings {
    var adbPath: String = ""
    var bundletoolPath: String = ""
    
    companion object {
        fun getInstance() = ServiceManager.getService(PluginSettings::class.java)
    }
}
```

**Step 2: 创建 AdbLocator (路径探测)**

```kotlin
package com.kip2.apkinstaller.util

import java.io.File
import java.nio.file.Path

class AdbLocator {
    
    data class AdbResult(
        val path: String?,
        val source: String
    )
    
    fun findAdb(): AdbResult {
        // 1. 检查 IDE 设置
        val settings = com.kip2.apkinstaller.settings.PluginSettings.getInstance()
        if (settings.adbPath.isNotBlank() && File(settings.adbPath).exists()) {
            return AdbResult(settings.adbPath, "IDE Settings")
        }
        
        // 2. 检查 local.properties
        val localProperties = findInLocalProperties()
        if (localProperties != null) return localProperties
        
        // 3. 检查环境变量 ANDROID_HOME
        val envAdb = findInAndroidHome()
        if (envAdb != null) return envAdb
        
        // 4. 检查 PATH
        val pathAdb = findInSystemPath()
        if (pathAdb != null) return pathAdb
        
        return AdbResult(null, "Not found")
    }
    
    private fun findInLocalProperties(): AdbResult? {
        // 解析当前项目的 local.properties
        // 略...
        return null
    }
    
    private fun findInAndroidHome(): AdbResult? {
        val androidHome = System.getenv("ANDROID_HOME") 
            ?: System.getenv("ANDROID_SDK_ROOT") 
            ?: return null
        val adb = File(androidHome, "platform-tools/adb")
        return if (adb.exists()) AdbResult(adb.absolutePath, "ANDROID_HOME") else null
    }
    
    private fun findInSystemPath(): AdbResult? {
        // 检查系统 PATH 中是否存在 adb
        // 略...
        return null
    }
}
```

**Step 3: 验证 AdbLocator**

```kotlin
fun test_adb_locator_finds_system_adb() {
    val locator = AdbLocator()
    val result = locator.findAdb()
    assert(result.path != null) { "Should find adb in system" }
}
```

**Step 4: Commit**

```bash
git add .
git commit -feat: implement ADB path detection service"
```

---

### Task 4: 实现设备检测与管理

**文件:**
- Create: `src/main/kotlin/com.kip2.apkinstaller/model/Device.kt`
- Create: `src/main/kotlin/com.kip2.apkinstaller/service/DeviceManager.kt`

**Step 1: 定义 Device 数据模型**

```kotlin
package com.kip2.apkinstaller.model

data class Device(
    val id: String,
    val name: String,
    val state: DeviceState
)

enum class DeviceState {
    ONLINE, OFFLINE, UNAUTHORIZED
}
```

**Step 2: 创建 DeviceManager (执行 adb devices)**

```kotlin
package com.kip2.apkinstaller.service

import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.model.DeviceState
import com.kip2.apkinstaller.util.AdbLocator
import java.io.BufferedReader
import java.io.InputStreamReader

class DeviceManager(private val adbLocator: AdbLocator = AdbLocator()) {
    
    fun getDevices(): List<Device> {
        val adbPath = adbLocator.findAdb().path 
            ?: throw IllegalStateException("ADB not found")
        
        val process = ProcessBuilder(adbPath, "devices")
            .redirectErrorStream(true)
            .start()
        
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val devices = mutableListOf<Device>()
        
        reader.useLines { lines ->
            lines.drop(1) // 跳过 "List of devices attached"
                .filter { it.isNotBlank() }
                .forEach { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val id = parts[0]
                        val stateStr = parts[1]
                        val state = when (stateStr) {
                            "device" -> DeviceState.ONLINE
                            "offline" -> DeviceState.OFFLINE
                            else -> DeviceState.UNAUTHORIZED
                        }
                        devices.add(Device(id = id, name = id, state = state))
                    }
                }
        }
        
        return devices.filter { it.state == DeviceState.ONLINE }
    }
}
```

**Step 3: 测试 DeviceManager**

```kotlin
fun test_get_devices_returns_online_only() {
    val manager = DeviceManager()
    val devices = manager.getDevices()
    assert(devices.all { it.state == DeviceState.ONLINE })
}
```

**Step 4: Commit**

```bash
git add .
git commit -m "feat: implement device detection via adb devices"
```

---

### Task 5: 构建设置页面 UI (Settings Page)

**文件:**
- Create: `src/main/kotlin/com.kip2.apkinstaller/settings/SettingsComponent.kt`
- Create: `src/main/kotlin/com.kip2.apkinstaller/settings/SettingsConfigurable.kt`
- Modify: `src/main/kotlin/com.kip2.apkinstaller/settings/PluginSettings.kt`

**Step 1: 创建 SettingsComponent (Swing UI)**

```kotlin
package com.kip2.apkinstaller.settings

import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBLabel
import com.intellij.openapi.ui.ComboBox
import javax.swing.JPanel
import javax.swing.BoxLayout

class SettingsComponent {
    private val panel = JPanel()
    private val adbPathField = JBTextField()
    private val adbComboBox = ComboBox<String>()
    
    init {
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        // ADB Path
        val adbLabel = JBLabel("ADB Path:")
        adbComboBox.setEditable(true)
        
        panel.add(adbLabel)
        panel.add(adbComboBox)
    }
    
    fun getPanel() = panel
    
    fun setAdbPath(path: String) {
        adbComboBox.addItem(path)
        adbComboBox.selectedItem = path
    }
    
    fun getAdbPath(): String = adbComboBox.selectedItem?.toString() ?: ""
}
```

**Step 2: 创建 SettingsConfigurable (注册到 IDE)**

```kotlin
package com.kip2.apkinstaller.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException

class SettingsConfigurable : Configurable {
    private var component: SettingsComponent? = null
    
    override fun createComponent(): JComponent {
        component = SettingsComponent()
        return component!!.getPanel()
    }
    
    override fun isModified(): Boolean {
        val settings = PluginSettings.getInstance()
        return component!!.getAdbPath() != settings.adbPath
    }
    
    override fun apply() {
        val settings = PluginSettings.getInstance()
        settings.adbPath = component!!.getAdbPath()
    }
    
    override fun getDisplayName() = "Apk/Aab Installer"
}
```

**Step 3: 注册 Configurable 到 plugin.xml**

```xml
<application-components>
    <component>
        <implementation-class>com.kip2.apkinstaller.settings.SettingsConfigurable</implementation-class>
    </component>
</application-components>
```

**Step 4: 构建验证**

Run: `./gradlew buildPlugin`

**Step 5: Commit**

```bash
git add .
git commit -m "feat: add settings page for ADB configuration"
```

---

## 阶段 3: UI 交互实现

### Task 6: 实现设备选择对话框

**文件:**
- Create: `src/main/kotlin/com.kip2.apkinstaller/ui/DeviceSelectionDialog.kt`

**Step 1: 创建 DeviceSelectionDialog (DialogWrapper)**

```kotlin
package com.kip2.apkinstaller.ui

import com.intellij.openapi.ui.DialogWrapper
import com.kip2.apkinstaller.model.Device
import javax.swing.*
import java.awt.*
import javax.swing.tree.RowMapper

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
        
        // Select All
        val selectAll = JCheckBox("Select All")
        selectAll.addActionListener {
            checkBoxes.forEach { it.isSelected = selectAll.isSelected }
            selection.keys.forEach { selection[it] = selectAll.isSelected }
        }
        
        // Device list
        val listPanel = JPanel(GridLayout(0, 1, 5, 5))
        devices.forEach { device ->
            val checkBox = JCheckBox("${device.name} (${device.id})")
            checkBox.addActionListener {
                selection[device.id] = checkBox.isSelected
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
```

**Step 2: Commit**

```bash
git add .
git commit -m "feat: implement device selection dialog with checkboxes"
```

---

### Task 7: 实现安装 Action 核心逻辑

**文件:**
- Modify: `src/main/kotlin/com.kip2.apkinstaller/actions/InstallAction.kt`

**Step 1: 更新 InstallAction 实现**

```kotlin
package com.kip2.apkinstaller.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.kip2.apkinstaller.InstallerBundle
import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.service.DeviceManager
import com.kip2.apkinstaller.ui.DeviceSelectionDialog
import com.kip2.apkinstaller.util.AdbLocator

class InstallAction : AnAction(InstallerBundle.message("install.action.text")) {
    
    override fun update(e: AnActionEvent) {
        // 只在选中 apk/aab 文件时显示
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val visible = files?.any { f -> 
            f.extension?.lowercase() in listOf("apk", "aab")
        } ?: false
        e.presentation.isVisible = visible
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        val extension = file.extension?.lowercase() ?: return
        if (extension !in listOf("apk", "aab")) return
        
        // 1. 获取设备列表
        val deviceManager = DeviceManager()
        val devices = try {
            deviceManager.getDevices()
        } catch (ex: Exception) {
            showError(project, "Failed to get devices: ${ex.message}")
            return
        }
        
        if (devices.isEmpty()) {
            showError(project, "No device connected")
            return
        }
        
        // 2. 如果只有 1 台设备，直接安装；否则弹出选择对话框
        val targetDevices = if (devices.size == 1) {
            devices
        } else {
            val dialog = DeviceSelectionDialog(devices)
            if (!dialog.showAndGet()) return
            dialog.selectedDevices
        }
        
        // 3. TODO: 执行安装 (Task 8)
    }
    
    private fun showError(project: com.intellij.openapi.project.Project, message: String) {
        // 使用 NotificationGroup 显示错误
    }
}
```

**Step 2: 构建验证**

Run: `./gradlew buildPlugin`

**Step 3: Commit**

```bash
git add .
git commit -m "feat: implement install action with device selection logic"
```

---

### Task 8: 实现 APK 安装逻辑与后台任务

**文件:**
- Create: `src/main/kotlin/com.kip2.apkinstaller/service/ApkInstaller.kt`
- Modify: `src/main/kotlin/com.kip2.apkinstaller/actions/InstallAction.kt`

**Step 1: 创建 ApkInstaller 服务**

```kotlin
package com.kip2.apkinstaller.service

import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.util.AdbLocator
import com.intellij.openapi.progress.ProgressIndicator
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

class ApkInstaller(
    private val adbLocator: AdbLocator = AdbLocator()
) {
    data class InstallResult(
        val device: Device,
        val success: Boolean,
        val output: String
    )
    
    fun install(apkFile: File, devices: List<Device>, indicator: ProgressIndicator): List<InstallResult> {
        val adbPath = adbLocator.findAdb().path 
            ?: throw IllegalStateException("ADB not found")
        
        val results = mutableListOf<InstallResult>()
        
        devices.forEachIndexed { index, device ->
            if (indicator.isCanceled) return results
            
            indicator.text = "Installing to ${device.name}..."
            indicator.fraction = (index + 1).toDouble() / devices.size
            
            val result = installToDevice(adbPath, apkFile, device)
            results.add(result)
        }
        
        return results
    }
    
    private fun installToDevice(adbPath: String, apkFile: File, device: Device): InstallResult {
        val process = ProcessBuilder(
            adbPath, "-s", device.id, "install", "-r", apkFile.absolutePath
        ).redirectErrorStream(true).start()
        
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        return InstallResult(
            device = device,
            success = exitCode == 0,
            output = output
        )
    }
}
```

**Step 2: 修改 InstallAction 使用 Backgroundable Task**

```kotlin
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.PerformInBackgroundOption

// 在 actionPerformed 中替换 TODO 部分:
ProgressManager.getInstance().run(object : Task.Backgroundable(
    project,
    "Installing APK...",
    true,
    PerformInBackgroundOption.DEAF
) {
    override fun run(indicator: ProgressIndicator) {
        val installer = ApkInstaller()
        val results = installer.install(file.toNioPath().toFile(), targetDevices, indicator)
        
        // 显示结果通知
        val successCount = results.count { it.success }
        val message = "$successCount / ${results.size} devices installed successfully"
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ApkInstaller")
            .createNotification(message, NotificationType.INFO)
            .notify(project)
    }
})
```

**Step 3: 构建验证**

Run: `./gradlew buildPlugin`

**Step 4: Commit**

```bash
git add .
git commit -m "feat: implement APK installation with background task"
```

---

### Task 9: 支持 Editor Tab 右键菜单

**文件:**
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: 添加 Editor Tab 上下文菜单**

```xml
<actions>
    <!-- 保留原来的 ProjectViewPopupMenu -->
    
    <!-- 新增: Editor Tab 上下文菜单 -->
    <action id="InstallApkFromEditor"
            class="com.kip2.apkinstaller.actions.InstallAction"
            text="Install to Device(s)">
        <add-to-group group-id="EditorTabPopupMenu" anchor="first"/>
    </action>
</actions>
```

**Step 2: 修改 InstallAction 支持 Editor Tab**

```kotlin
// 在 update 方法中添加:
override fun update(e: AnActionEvent) {
    // 检查 Project View 或 Editor Tab
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) 
        ?: e.getData(EditorSmartKey.VIRTUAL_FILE)  // 需要获取编辑器中的文件
    
    val visible = file?.extension?.lowercase() in listOf("apk", "aab")
    e.presentation.isVisible = visible
}
```

**Step 3: 构建验证 & Commit**

Run: `./gradlew buildPlugin`
Commit: `git add . && git commit -m "feat: support editor tab context menu"`

---

## 阶段 4: AAB 安装支持 (Phase 2)

### Task 10: 实现 Bundletool 配置与下载

**文件:**
- Create: `src/main/kotlin/com.kip2.apkinstaller/util/BundletoolHelper.kt`
- Create: `src/main/kotlin/com.kip2.apkinstaller/ui/BundletoolSetupDialog.kt`

**Step 1: 创建 BundletoolHelper**

```kotlin
package com.kip2.apkinstaller.util

import com.kip2.apkinstaller.settings.PluginSettings
import java.io.File
import java.net.URL
import java.nio.file.Files

class BundletoolHelper {
    
    fun getBundletoolPath(): String? {
        val settings = PluginSettings.getInstance()
        if (settings.bundletoolPath.isNotBlank() && File(settings.bundletoolPath).exists()) {
            return settings.bundletoolPath
        }
        
        // 检查默认缓存路径
        val defaultPath = getDefaultCachePath()
        if (File(defaultPath).exists()) {
            return defaultPath
        }
        
        return null
    }
    
    fun downloadBundletool(progressIndicator: com.intellij.openapi.progress.ProgressIndicator): File {
        val cacheDir = getCacheDir()
        cacheDir.mkdirs()
        
        val targetFile = File(cacheDir, "bundletool.jar")
        
        if (targetFile.exists()) {
            return targetFile
        }
        
        // 从 GitHub Releases 下载
        val url = URL("https://github.com/google/bundletool/releases/download/1.16.0/bundletool-all-1.16.0.jar")
        
        progressIndicator.text = "Downloading bundletool..."
        
        url.openStream().use { input ->
            Files.copy(input, targetFile.toPath())
        }
        
        return targetFile
    }
    
    private fun getCacheDir(): File {
        val pluginDir = File(System.getProperty("user.home"), ".apk-installer")
        return File(pluginDir, "bundletool")
    }
    
    private fun getDefaultCachePath(): String {
        return File(getCacheDir(), "bundletool.jar").absolutePath
    }
}
```

**Step 2: 创建 BundletoolSetupDialog**

```kotlin
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
```

**Step 3: Commit**

```bash
git add .
git commit -m "feat: implement bundletool download and setup"
```

---

### Task 11: 实现 AAB 安装逻辑

**文件:**
- Create: `src/main/kotlin/com.kip2.apkinstaller/service/AabInstaller.kt`

**Step 1: 创建 AabInstaller**

```kotlin
package com.kip2.apkinstaller.service

import com.kip2.apkinstaller.model.Device
import com.kip2.apkinstaller.util.AdbLocator
import com.kip2.apkinstaller.util.BundletoolHelper
import com.intellij.openapi.progress.ProgressIndicator
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

class AabInstaller(
    private val adbLocator: AdbLocator = AdbLocator(),
    private val bundletoolHelper: BundletoolHelper = BundletoolHelper()
) {
    
    fun install(aabFile: File, devices: List<Device>, indicator: ProgressIndicator): List<ApkInstaller.InstallResult> {
        val adbPath = adbLocator.findAdb().path 
            ?: throw IllegalStateException("ADB not found")
        
        val bundletoolPath = bundletoolHelper.getBundletoolPath()
            ?: throw IllegalStateException("Bundletool not found. Please configure in settings.")
        
        val results = mutableListOf<ApkInstaller.InstallResult>()
        
        devices.forEachIndexed { index, device ->
            if (indicator.isCanceled) return results
            
            indicator.text = "Building APKS for ${device.name}..."
            indicator.fraction = (index + 1).toDouble() / devices.size * 0.5  // 前 50% 用于构建
            
            // 1. 使用 bundletool build-apks
            val apksFile = buildApks(bundletoolPath, aabFile, device)
            
            indicator.text = "Installing to ${device.name}..."
            indicator.fraction = 0.5 + (index + 1).toDouble() / devices.size * 0.5  // 后 50% 用于安装
            
            // 2. 使用 adb install 安装
            val result = installApks(adbPath, apksFile, device)
            results.add(result)
            
            // 清理临时文件
            apksFile.delete()
        }
        
        return results
    }
    
    private fun buildApks(bundletoolPath: String, aabFile: File, device: Device): File {
        val outputFile = File.createTempFile("output", ".apks")
        
        val process = ProcessBuilder(
            "java", "-jar", bundletoolPath,
            "build-apks",
            "--bundle", aabFile.absolutePath,
            "--output", outputFile.absolutePath,
            "--connected-device",
            "--device-id", device.id
        ).redirectErrorStream(true).start()
        
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            throw RuntimeException("Failed to build APKS: $output")
        }
        
        return outputFile
    }
    
    private fun installApks(adbPath: String, apksFile: File, device: Device): ApkInstaller.InstallResult {
        val process = ProcessBuilder(
            adbPath, "-s", device.id, "install-multiple",
            "-r", apksFile.absolutePath
        ).redirectErrorStream(true).start()
        
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        return ApkInstaller.InstallResult(
            device = device,
            success = exitCode == 0,
            output = output
        )
    }
}
```

**Step 2: 修改 InstallAction 支持 AAB**

```kotlin
// 在 InstallAction.actionPerformed 中:

if (extension == "aab") {
    // 检查 bundletool
    val bundletoolHelper = BundletoolHelper()
    if (bundletoolHelper.getBundletoolPath() == null) {
        // 弹窗提示配置
        val dialog = BundletoolSetupDialog()
        if (!dialog.showAndGet()) return
        
        // 可选: 自动下载
        val downloadDialog = BundletoolDownloadDialog()
        if (downloadDialog.showAndGet()) {
            // 下载并配置
        }
        return
    }
    
    // 执行 AAB 安装
    val aabInstaller = AabInstaller()
    // ... 执行安装
} else {
    // APK 安装 (现有逻辑)
}
```

**Step 3: 构建验证 & Commit**

Run: `./gradlew buildPlugin`
Commit: `git add . && git commit -m "feat: implement AAB installation with bundletool"`

---

## 阶段 5: 开源与发布 (Phase 3)

### Task 12: 添加开源协议 (MIT License)

**文件:**
- Create: `LICENSE`
- Create: `README.md`
- Modify: `build.gradle.kts` (更新开源信息)

**Step 1: 创建 LICENSE 文件 (MIT)**

**Step 2: 创建 README.md**
补充插件功能说明，安装和配置使用教程。

**Step 3: 构建验证 & Commit**

```bash
git add .
git commit -m "chore: add MIT license and README"
```

---

### Task 13: 准备发布与插件市场配置

**文件:**
- Modify: `src/main/resources/META-INF/plugin.xml` (补充详细的 Vendor、Description 和 Tags)

**Step 1: 更新 plugin.xml**
增加 Tags 例如 `<tags>android, apk, adb, install, device</tags>`，完善 Vendor 等信息，以利于搜索。

**Step 2: 构建最终发布包**

Run: `./gradlew buildPlugin`
生成的 zip 文件位于: `build/distributions/apk-aab-installer-1.0.0.zip`

**Step 3: Commit**

```bash
git add .
git commit -m "chore: prepare for marketplace release"
```

---

## 验收标准

每个 Task 完成后需满足:
- [ ] `./gradlew buildPlugin` 成功
- [ ] 生成的插件 zip 包位于 `build/distributions/`
- [ ] 代码无编译警告
- [ ] 已提交到 Git

---

**实施计划已保存。两种执行方式：**

**1. Subagent-Driven (当前 Session)** - 每个任务分配一个新的子代理，任务间进行代码审查，快速迭代

**2. Parallel Session (新 Session)** - 在新 Session 中使用 executing-plans 批量执行并设置检查点

**您想选择哪种方式？**
