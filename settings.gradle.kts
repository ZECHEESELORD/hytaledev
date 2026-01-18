import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "hytaledev"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.1.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

include(":core")
include(":idea-plugin")
include(":cli")

project(":core").projectDir = file("core")
project(":idea-plugin").projectDir = file("idea-plugin")
project(":cli").projectDir = file("cli")
