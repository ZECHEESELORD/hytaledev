plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.intellij.platform") version "2.1.0" apply false
}

allprojects {
    group = "sh.harold.hytaledev"
    version = "0.1.0-SNAPSHOT"
}
