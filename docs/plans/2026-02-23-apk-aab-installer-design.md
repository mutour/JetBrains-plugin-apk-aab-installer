# JetBrains Apk/Aab Installer 插件 - 设计文档

## 1. 概述
一个专为 JetBrains IntelliJ 平台设计的插件，旨在简化从 IDE 将 `.apk` 和 `.aab`（Android App Bundle）文件直接安装到已连接的 Android 设备的流程。

## 2. 核心功能与交互流程

### 2.1 入口点（UI 集成）
- **项目视图（Project View）右键菜单**：在任何 `.apk` 或 `.aab` 文件上点击右键，会显示 `Install to Device(s)`（安装到设备）操作。
- **编辑器标签（Editor Tab）右键菜单**：当双击并在编辑器中预览 `.apk` 或 `.aab` 文件时，右键点击标签栏也会提供 `Install to Device(s)` 操作。

### 2.2 设备检测与选择
- **ADB 查询**：执行 `adb devices` 命令，解析当前已连接且在线的设备列表（忽略 offline/unauthorized 状态的设备）。
- **单设备**：如果当前仅连接了 **1 台** 设备，插件将跳过选择对话框，直接在后台执行安装。
- **多设备**：如果连接了 **多台** 设备，将弹出一个包含复选框列表的模态对话框（DialogWrapper）。它包含一个“全选”开关。用户可以选择目标设备，确认后，插件将在后台为所有选中的目标设备进行安装。

### 2.3 安装执行
- **后台任务**：安装过程使用 JetBrains 的 `Task.Backgroundable` API 在非 EDT（Event Dispatch Thread）线程中执行，确保 IDE 界面不会卡顿。
- **进度 UI**：在 IDE 底部的状态栏中显示 `ProgressIndicator` 进度条，实时展示当前正在安装的设备。
- **通知（Notifications）**：执行结束后（无论是成功还是失败），将使用 `NotificationGroupManager` 弹出一个通知气泡，详细说明安装结果。

## 3. 环境与工具链策略

### 3.1 ADB 路径探测机制
插件为 `adb` 可执行文件实现了健壮、具有优先级的探测机制：
1. **IDE 设置**：检查用户在 `Settings/Preferences -> Tools -> Apk/Aab Installer` 中的自定义配置。
2. **项目配置**：解析当前项目根目录的 `local.properties` 文件，读取 `sdk.dir` 属性并拼接 `/platform-tools/adb`。
3. **环境变量**：检查 `ANDROID_HOME` 和 `ANDROID_SDK_ROOT`。
4. **全局 PATH**：尝试直接执行系统全局的 `adb` 命令。

*注意：设置面板会将上述发现的所有路径聚合到一个下拉菜单中，允许用户无需手动输入即可快速切换不同的 ADB 环境。*

### 3.2 Bundletool 处理（阶段 2：AAB 安装）
- 为了安装 `.aab` 文件，需要依赖 Google 的 `bundletool.jar` 将其构建为 APKS 并推送到设备。
- **动态获取**：如果用户尝试安装 AAB 但尚未配置 `bundletool`，插件将弹出一个设置提示框。
- **自动下载**：该提示框将提供一个“一键下载”按钮。点击后，插件会从 GitHub Releases 下载最新版的 `bundletool.jar` 到插件的本地缓存目录中，从而保持插件安装包体积小巧。

## 4. 分阶段实施计划
- **阶段 1**：项目脚手架搭建、ADB 路径探测机制、设备检测解析、设置页面 UI 以及 `.apk` 安装核心逻辑。
- **阶段 2**：编辑器标签栏集成、`bundletool` 动态下载机制，以及 `.aab` 的解压与安装逻辑。
