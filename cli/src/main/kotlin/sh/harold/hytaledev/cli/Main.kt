package sh.harold.hytaledev.cli

import sh.harold.hytaledev.server.ServerJarInspector
import java.nio.file.Path

fun main(args: Array<String>) {
    when (val command = args.firstOrNull()) {
        "inspect-jar" -> inspectJar(args.drop(1))
        "init", "doctor" -> println("$command is not implemented yet.")
        null -> printUsage()
        else -> {
            println("Unknown command: $command")
            printUsage()
        }
    }
}

private fun inspectJar(args: List<String>) {
    val jarPath = args.firstOrNull()?.let { Path.of(it) }
        ?: error("Usage: hytaledev inspect-jar <path-to-HytaleServer.jar>")

    val report = ServerJarInspector().inspect(jarPath)
    println("Jar: ${report.jarPath}")
}

private fun printUsage() {
    println("Usage: hytaledev <command> [args]")
    println("Commands:")
    println("  init")
    println("  doctor")
    println("  inspect-jar <path-to-HytaleServer.jar>")
}
