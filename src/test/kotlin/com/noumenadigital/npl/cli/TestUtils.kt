package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.service.ColorWriter
import java.io.File
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.Paths

object TestUtils {
    data class TestContext(
        val commands: List<String>,
        val workingDirectory: File = File("."),
        val process: Process,
        val output: String,
    )

    private fun getNplPath(): String {
        // For running with the binary
        val rootDir = File(".").canonicalFile
        val targetDir = rootDir.resolve("target")

        val candidates =
            targetDir.listFiles { _, name ->
                name == "npl" || name.startsWith("npl-")
            }

        if (!candidates.isNullOrEmpty()) {
            return candidates.maxByOrNull(File::lastModified)?.absolutePath!!
        }

        throw IllegalStateException("Cannot locate NPL native binary in ${targetDir.absolutePath}")
    }

    private fun getJarPath(): String {
        // Path to the fat JAR with dependencies
        val rootDir = File(".").canonicalFile
        return rootDir.resolve("target/npl-cli-jar-with-dependencies.jar").absolutePath
    }

    // Determines how to run the tests based on the test.mode system property
    private fun getTestMode(): String = System.getenv().getOrDefault("TEST_MODE", "direct")

    fun getTestResourcesPath(subPath: List<String> = emptyList()): Path {
        // Use the standard resources directory location
        val rootDir = File("src/test/resources").canonicalFile
        return Paths
            .get(rootDir.toString(), "npl-sources", *subPath.toTypedArray())
            .toAbsolutePath()
            .normalize()
    }

    fun String.normalize(withPadding: Boolean = true): String =
        replace("\r\n", "\n")
            // Normalize path separators to forward slashes
            .replace('\\', '/')
            // Normalize durations
            .replace(Regex("in \\d+ ms"), "in XXX ms")
            .replace(Regex("\\d+%"), "XXX%")
            // Remove any ANSI color codes
            .replace(Regex("\\e\\[[0-9;]*m"), "")
            .replace(Regex("(\\.*FAIL)[ \\t]+"), "$1")
            .replace(Regex("(\\.{2,})"), if (withPadding) "..padding.." else "$1") // TODO: remove when ST-4601
            .trimIndent()

    fun runCommand(
        commands: List<String>,
        env: Map<String, String> = emptyMap(),
        test: TestContext.() -> Unit,
    ) {
        val testMode = getTestMode()

        val testContext =
            when (testMode) {
                "binary" -> runWithBinary(commands, env)
                "jar" -> runWithJar(commands, env)
                else -> runDirect(commands)
            }

        testContext.apply(test)
    }

    private fun runWithBinary(
        commands: List<String>,
        env: Map<String, String>,
    ): TestContext {
        val process =
            ProcessBuilder(getNplPath(), *commands.toTypedArray())
                .redirectErrorStream(true)
                .apply {
                    environment().putAll(env)
                }.start()

        val output =
            process.inputStream
                .bufferedReader()
                .readText()
                .trimIndent()

        return TestContext(
            commands = commands,
            process = process,
            output = output,
        )
    }

    private fun runWithJar(
        commands: List<String>,
        env: Map<String, String>,
    ): TestContext {
        // Get the JAR file path
        val jarPath = getJarPath()

        // Ensure the JAR exists
        if (!File(jarPath).exists()) {
            throw IllegalStateException(
                "JAR file not found at $jarPath. " +
                    "Run 'mvn package -Pconfig-gen' to build the JAR file.",
            )
        }

        // Build the command: java -jar <jar-path> <commands>
        val commandList =
            mutableListOf(
                "java",
                "-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image",
                "-Duser.home=${System.getProperty("user.home")}",
                "--enable-native-access=ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access=allow",
                "-Djava.awt.headless=true",
                "-jar",
                jarPath,
            )
        commandList.addAll(commands)

        // Start the process
        val process =
            ProcessBuilder(commandList)
                .redirectErrorStream(true)
                .apply {
                    environment().putAll(env)
                }.start()

        // Read the output
        val output =
            process.inputStream
                .bufferedReader()
                .readText()
                .trimIndent()

        return TestContext(
            commands = commands,
            process = process,
            output = output,
        )
    }

    private fun runDirect(commands: List<String>): TestContext {
        val stringWriter = ColorWriter(StringWriter(), false)

        val exitCode = CommandProcessor().process(commands, stringWriter)

        // Get the output as a string
        val output = stringWriter.toString().trimIndent()

        // Create a Process-like object to maintain API compatibility
        val process =
            object : Process() {
                override fun destroy() {}

                override fun exitValue(): Int = exitCode.code

                override fun getErrorStream() = null

                override fun getInputStream() = null

                override fun getOutputStream() = null

                override fun isAlive() = false

                override fun waitFor(): Int = 0

                override fun waitFor(
                    timeout: Long,
                    unit: java.util.concurrent.TimeUnit,
                ) = false
            }

        return TestContext(
            commands = commands,
            process = process,
            output = output,
        )
    }
}
