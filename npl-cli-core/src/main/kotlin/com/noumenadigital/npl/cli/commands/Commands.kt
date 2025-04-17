package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CheckCommand
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import java.nio.file.Path

enum class Commands(
    val commandName: String,
    val description: String,
    val createExecutor: (List<String>, Path?) -> CommandExecutor,
) {
    VERSION(
        "version",
        VersionCommand.COMMAND_DESCRIPTION,
        { _, _ -> VersionCommand },
    ),
    HELP(
        "help",
        HelpCommand.COMMAND_DESCRIPTION,
        { _, _ -> HelpCommand },
    ),
    CHECK(
        "check",
        CheckCommand.COMMAND_DESCRIPTION,
        { params, _ ->
            val targetDir = params.firstOrNull() ?: "."
            CheckCommand(targetDir = targetDir)
        },
    ),
    ;

    companion object {
        fun commandFromString(
            command: String,
            params: List<String> = emptyList(),
            baseDir: Path? = null,
        ): CommandExecutor {
            val normalizedCommand = command.lowercase()
            val matchedCommand =
                Commands.fromString(normalizedCommand)
                    ?: throw CommandNotFoundException(normalizedCommand)

            return matchedCommand.createExecutor.invoke(params, baseDir)
        }

        private fun fromString(name: String): Commands? = entries.find { it.commandName.equals(name, ignoreCase = true) }
    }
}
