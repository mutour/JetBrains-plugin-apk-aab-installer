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
    // compileOnly(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}

intellij {
    version.set("213.7172.25")
    type.set("IC")
}

tasks {
    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("243.*")
    }
    withType<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>().configureEach {
        enabled = false
    }
}
