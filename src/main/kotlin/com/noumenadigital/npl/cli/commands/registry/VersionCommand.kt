package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.service.ColorWriter
import java.io.InputStream
import java.util.Properties

data object VersionCommand : CommandExecutor {
    override val commandName: String = "version"
    override val description: String = "Display the current version of the NPL CLI"

    override fun execute(output: ColorWriter): ExitCode {
        val version = getVersionFromPom() ?: "development"
        output.success("NPL CLI $version")
        return ExitCode.SUCCESS
    }

    private fun getVersionFromPom(): String? =
        try {
            val pomProperties: InputStream? =
                this::class.java.getResourceAsStream(
                    "/META-INF/maven/com.noumenadigital.platform/npl-cli/pom.properties",
                )
            if (pomProperties != null) {
                val props = Properties()
                props.load(pomProperties)
                props.getProperty("version")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
}
