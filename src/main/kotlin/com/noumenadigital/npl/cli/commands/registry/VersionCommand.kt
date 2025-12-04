package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.service.ColorWriter
import mu.KotlinLogging.logger
import java.util.Properties

val logger = logger {}

object VersionCommandDescriptor : CommandDescriptor {
    override val commandName: String = "version"
    override val description: String = "Display the current version of the NPL CLI"
    override val supportsMcp: Boolean = false

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor = VersionCommand
}

data object VersionCommand : CommandExecutor {
    override fun execute(output: ColorWriter): ExitCode {
        val version = getVersionFromPom()
        if (version == null) {
            output.warning("Could not determine NPL CLI version from POM file, assuming `development`.")
        }
        output.success("NPL CLI ${version ?: "development"}")
        return ExitCode.SUCCESS
    }

    internal fun getVersionFromPom(): String? =
        try {
            this::class.java
                .getResourceAsStream(
                    "/META-INF/maven/com.noumenadigital.platform/npl-cli/pom.properties",
                )?.use { inputStream ->
                    Properties()
                        .apply {
                            load(inputStream)
                        }.getProperty("version")
                }
        } catch (_: Exception) {
            logger.debug { "Failed to read version from POM file." }
            null
        }
}
