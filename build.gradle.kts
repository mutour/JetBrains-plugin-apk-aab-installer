plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
}

group = "com.kip2"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
//        intellijIdea("2025.2.4")
//        local("/Users/king/Applications/Android Studio.app")
        local("/Users/king/Applications/WebStorm.app")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Compose support provided by the platform
        @Suppress("UnstableApiUsage")
        composeUI()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.kip2.apkinstaller"
        name = "Apk/Aab Installer"
        
        ideaVersion {
            sinceBuild = "221"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
