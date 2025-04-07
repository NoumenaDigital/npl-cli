package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.exception.NplCliException
import com.noumenadigital.npl.cli.service.CommandExecutorOutput
import com.noumenadigital.npl.cli.service.CommandsParser
import com.noumenadigital.npl.cli.service.NplCommandsParser

interface CommandExecutor {
    fun process(
        commands: List<String>,
        output: CommandExecutorOutput,
    )
}

class NplCommandExecutor(
    private val commandsParser: CommandsParser = NplCommandsParser(),
) : CommandExecutor {
    override fun process(
        commands: List<String>,
        output: CommandExecutorOutput,
    ) {
        output.get().use { out ->
            try {
                val commandsList = commandsParser.parse(commands)
                commandsList.forEach { command ->
                    out.write("Executing command: ${command.nplCliCommandsEnum.commandName}...\n")
                    command.nplCliCommandsEnum.nplCommand?.execute(out)
                }
            } catch (ex: NplCliException) {
                ex.message?.let { out.write(it) }
            } catch (ex: Exception) {
                ex.message?.let { out.write(ex.stackTraceToString()) }
            } finally {
                out.write("\n")
            }
        }
    }
}
