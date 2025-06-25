package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.commands.registry.CheckCommand
import com.noumenadigital.npl.cli.commands.registry.CloudCommand
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.commands.registry.DeployCommand
import com.noumenadigital.npl.cli.commands.registry.HelpCommand
import com.noumenadigital.npl.cli.commands.registry.InitCommand
import com.noumenadigital.npl.cli.commands.registry.McpCommand
import com.noumenadigital.npl.cli.commands.registry.OpenapiCommand
import com.noumenadigital.npl.cli.commands.registry.PumlCommand
import com.noumenadigital.npl.cli.commands.registry.TestCommand
import com.noumenadigital.npl.cli.commands.registry.VersionCommand
import com.noumenadigital.npl.cli.exception.CommandNotFoundException

enum class Commands(
    override val commandExecutorFactory: () -> CommandExecutor,
) : CommandsRegistry {
    VERSION({ VersionCommand }),
    HELP({ HelpCommand() }),
    INIT({ InitCommand() }),
    CHECK({ CheckCommand() }),
    TEST({ TestCommand() }),
    OPENAPI({ OpenapiCommand() }),
    PUML({ PumlCommand() }),
    DEPLOY({ DeployCommand() }),
    CLOUD({ CloudCommand() }),
    MCP({ McpCommand }),
    ;

    companion object {
        fun commandFromString(
            command: String,
            params: List<String> = emptyList(),
        ): CommandExecutor {
            val normalizedCommand = command.lowercase()
            val matchedCommand =
                Commands.entries.find { it.commandName.equals(normalizedCommand, ignoreCase = true) }
                    ?: throw CommandNotFoundException(normalizedCommand)

            val baseExecutor = matchedCommand.getBaseExecutor()
            return baseExecutor.createInstance(params)
        }
    }
}
