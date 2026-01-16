plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    implementation(project(":core"))

    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.plugins.gradle")
        instrumentationTools()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "sh.harold.hytaledev"
        name = "HytaleDev"
        version = project.version.toString()
    }
}
