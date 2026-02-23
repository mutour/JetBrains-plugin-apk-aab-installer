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
