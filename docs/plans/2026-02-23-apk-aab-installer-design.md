# JetBrains Apk/Aab Installer Plugin - Design Document

## 1. Overview
A JetBrains IntelliJ Platform plugin designed to simplify the installation of `.apk` and `.aab` (Android App Bundle) files directly from the IDE to connected Android devices.

## 2. Core Features & Interaction Flow

### 2.1 Entry Points (UI Integration)
- **Project View Context Menu**: Right-clicking on any `.apk` or `.aab` file will reveal an `Install to Device(s)` action.
- **Editor Tab Context Menu**: When an `.apk` or `.aab` file is double-clicked and opened/previewed in the editor tab, right-clicking the tab header will also provide the `Install to Device(s)` action.

### 2.2 Device Detection & Selection
- **ADB Query**: Executes `adb devices` to parse the list of currently connected and online devices (ignoring offline/unauthorized devices).
- **Single Device**: If exactly **1 device** is connected, the plugin skips the selection dialog and immediately proceeds to background installation.
- **Multiple Devices**: If **>1 device** is connected, a modal `DialogWrapper` with a Checkbox list is presented. It includes a "Select All" toggle. Users can select the target devices, and upon confirmation, the installation proceeds to the selected targets.

### 2.3 Installation Execution
- **Background Tasks**: The installation process uses JetBrains' `Task.Backgroundable` API to execute off the Event Dispatch Thread (EDT), ensuring the IDE UI does not freeze.
- **Progress UI**: A `ProgressIndicator` is shown in the IDE's bottom status bar, detailing which device is currently being installed to.
- **Notifications**: After execution (success or failure), an IDE Notification Balloon (using `NotificationGroupManager`) will be displayed detailing the outcome.

## 3. Environment & Tooling Strategy

### 3.1 ADB Path Resolution
The plugin implements a robust, prioritized detection mechanism for the `adb` executable:
1. **IDE Settings**: Custom user configuration found in `Settings/Preferences -> Tools -> Apk/Aab Installer`.
2. **Project Configuration**: The plugin parses the root `local.properties` file for `sdk.dir`, appending `/platform-tools/adb`.
3. **Environment Variables**: Checks `ANDROID_HOME` and `ANDROID_SDK_ROOT`.
4. **Global PATH**: Attempts a raw `adb` shell execution.

*Note: The Settings panel will aggregate all discovered paths into a dropdown menu, allowing users to rapidly switch between ADB environments without manual typing.*

### 3.2 Bundletool Handling (Phase 2: AAB)
- To install `.aab` files, Google's `bundletool.jar` is required to build APKS and push them.
- **Dynamic Retrieval**: If a user attempts to install an AAB and `bundletool` is not configured, the plugin will display a settings prompt.
- **Auto-Download**: The prompt will feature an "Auto-Download" button. When clicked, the plugin downloads the latest `bundletool.jar` from GitHub Releases into a local plugin cache directory, keeping the plugin binary lightweight.

## 4. Phased Implementation Plan
- **Phase 1**: Project scaffolding, ADB path resolution, device detection, Settings UI, and `.apk` installation logic.
- **Phase 2**: Editor tab integration, `bundletool` dynamic downloading, and `.aab` extraction/installation logic.
