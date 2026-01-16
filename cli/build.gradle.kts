plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
}

application {
    mainClass.set("sh.harold.hytaledev.cli.MainKt")
}
