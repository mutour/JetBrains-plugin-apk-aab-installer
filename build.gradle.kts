plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.kip2"
version = "1.0.3"

repositories {
    google()
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
//        intellijIdea("2025.2.4")
        local("/Users/king/Applications/IntelliJ IDEA.app")
//        local("/Users/king/Applications/Android Studio.app")
//        local("/Users/king/Applications/WebStorm.app")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.kip2.apkinstaller"
        name = "Apk/Aab Installer"
        
        ideaVersion {
            sinceBuild = "241"
        }

        description = """
            <h3>APK/AAB Multi-Device Installer</h3>
            <p>Streamline your Android deployment process by installing packages to one or multiple devices simultaneously.</p>
            
            <h4>Key Features:</h4>
            <ul>
                <li><b>Multi-Device Deployment:</b> Select and install APK or AAB files to one or multiple connected Android devices/emulators at once.</li>
                <li><b>Smart ADB Detection:</b> Automatically lists all connected devices via ADB with real-time status updates.</li>
                <li><b>AAB Support:</b> Seamlessly handles Android App Bundles (.aab) alongside standard APKs.</li>
                <li><b>Modern UI:</b> A clean, native-looking interface built with JetBrains Compose and Jewel.</li>
                <li><b>Background Execution:</b> All installation processes run in the background, keeping your IDE responsive.</li>
            </ul>

            <h4>🛠 Technical Information</h4>
            <ul>
                <li><b>Architecture:</b> Built with 100% Kotlin for high-performance integration with the IntelliJ Platform.</li>
                <li><b>UI Framework:</b> Developed using native <b>IntelliJ Kotlin UI DSL v2</b> for maximum compatibility and minimal size.</li>
                <li><b>Concurrency Model:</b> Utilizes background threads (Task.Backgroundable) to ensure fluid UI.</li>
                <li><b>Security & Privacy:</b> Operates entirely locally; no data collection or external transmission.</li>
            </ul>
        """.trimIndent()

        changeNotes = """
            <h4>🚀 Initial Release (v1.0.0)</h4>
            <p>We are excited to introduce the first version of the <b>APK/AAB Multi-Device Installer</b>.</p>
            <ul>
                <li><b>Multi-Device Deployment:</b> Install APK/AAB to multiple devices simultaneously.</li>
                <li><b>Smart ADB Detection:</b> Real-time detection of connected devices.</li>
                <li><b>AAB Support:</b> Integrated bundletool handling for App Bundles.</li>
                <li><b>Modern UI:</b> Refactored to native Kotlin UI DSL v2 for 2024.x compatibility and package size optimization.</li>
            </ul>
        """.trimIndent()
    }
//    pluginVerification {
//        ides {
//            ide("IC-2024.1.6")
//            ide("IC-2024.2.4")
//            ide("IC-2024.3.3")
//        }
//    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

//    runIde {
//        // 修复 Compose 在 IntelliJ 平台中的兼容性问题
//        jvmArgs(
//            "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
//            "--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED",
//            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
//            "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
//            "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
//            "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
//            "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED"
//        )
//    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
