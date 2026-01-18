plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.intellij.platform") apply false
}

allprojects {
    group = "sh.harold.hytaledev"
    version = "0.1.0-SNAPSHOT"
}
