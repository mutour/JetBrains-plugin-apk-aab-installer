# Design Doc: Multi-Platform Support for Apk/Aab Installer

## Status
- **Author**: Sisyphus (AI)
- **Date**: 2026-03-02
- **Topic**: Resolving platform compatibility issues by moving Java/Gradle dependencies to optional.

## Context
The plugin currently specifies hard dependencies on `com.intellij.modules.java` and `com.intellij.modules.externalSystem` in `plugin.xml`. This prevents the plugin from being installed in non-Java IDEs like WebStorm, PyCharm, and CLion.

The only part of the plugin that truly requires `externalSystem` is the automatic detection of signing configurations from Gradle projects. The rest of the plugin (ADB detection, APK/AAB installation, UI) only requires the base IntelliJ Platform.

## Goals
- Support all IntelliJ-based IDEs ("Full Platform").
- Preserve Gradle-based signing configuration detection in IDEs where it is supported (IntelliJ IDEA, Android Studio).
- Ensure a smooth fallback (manual entry) in IDEs where Gradle is not available.

## Design

### 1. `plugin.xml` Restructuring
- Remove the hard dependency on `com.intellij.modules.java`.
- Move the `com.intellij.modules.externalSystem` dependency to an optional block.

```xml
<depends>com.intellij.modules.platform</depends>

<!-- Optional dependencies -->
<depends optional="true" config-file="apkinstaller-gradle.xml">com.intellij.modules.externalSystem</depends>
```

### 2. Decoupling Signing Detection
To avoid `NoClassDefFoundError` in IDEs without the Gradle module, we will introduce an extension point for signing configuration detection.

**New Interface**: `com.kip2.apkinstaller.service.SigningConfigProvider`
```kotlin
interface SigningConfigProvider {
    fun getConfigs(project: Project, module: Module?): List<SigningConfig>
}
```

**Main Implementation**:
A default manual/empty provider will be used if no others are found.

**Optional Implementation**:
The `GradleSigningService` will be wrapped in a `GradleSigningConfigProvider` and registered in `apkinstaller-gradle.xml`.

### 3. Optional Configuration File
`src/main/resources/META-INF/apkinstaller-gradle.xml`:
```xml
<idea-plugin>
    <extensions defaultExtensionNs="com.intellij">
        <!-- Register Gradle-specific extensions here if needed, 
             or just use the extension point from the main plugin -->
    </extensions>
</idea-plugin>
```
Actually, we can define a custom extension point in the main `plugin.xml` and implement it in the optional file.

### 4. UI Adjustments
In `AabInstallDialog.kt`, the "Detect" button will only be visible or functional if at least one `SigningConfigProvider` (other than the manual one) is available, or we will explicitly check for the `com.intellij.modules.externalSystem` plugin availability.

## Validation Plan
- **WebStorm**: Verify the plugin installs and runs. The "Detect" button should be hidden or show a friendly message. Manual signing should work.
- **IntelliJ IDEA**: Verify that "Detect" button still pulls signing configs from Gradle.
- **Android Studio**: Same as IntelliJ IDEA.
