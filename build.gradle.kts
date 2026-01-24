import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.intellij.platform") apply false
}

allprojects {
    group = "sh.harold.hytaledev"
    version = "0.1.0-SNAPSHOT"
}

val projectJdkFeatureVersion = 21

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(projectJdkFeatureVersion)
        }
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(projectJdkFeatureVersion))
            }
        }
    }
}
