package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.exception.NplCliException
import com.noumenadigital.npl.cli.service.CommandExecutorOutput
import com.noumenadigital.npl.cli.service.CommandsParser
import com.noumenadigital.npl.cli.service.NplCommandsParser

sealed interface CommandExecutor {
    fun process(
        commands: List<String>,
        output: CommandExecutorOutput,
    )
}

class NplCommandExecutor(
    private val commandsParser: CommandsParser = NplCommandsParser,
) : CommandExecutor {
    companion object {
        private const val BEFORE_RESOLUTION_PADDING = "\n\n"
        private const val START_MESSAGE = "Executing command '%s'...\n"
        private const val END_MESSAGE_SUCCESS = "${BEFORE_RESOLUTION_PADDING}Command '%s' finished SUCCESSFULLY.\n"
        private const val END_GENERIC_ERROR_WRAPPER = "Executing command FAILED with error %s"
    }

    override fun process(
        commands: List<String>,
        output: CommandExecutorOutput,
    ) {
        output.get().use { out ->
            try {
                val commandsList = commandsParser.parse(commands)
                commandsList.forEach { command ->
                    val commandName = command.nplCliCommandsEnum.commandName
                    out.write(START_MESSAGE.format(commandName))
                    command.nplCliCommandsEnum.nplCommand?.execute(out)
                    out.write(END_MESSAGE_SUCCESS.format(commandName))
                }
            } catch (ex: NplCliException) {
                ex.message?.let { out.write(it) }
            } catch (ex: Exception) {
                ex.message?.let { out.write(END_GENERIC_ERROR_WRAPPER.format(ex.stackTraceToString())) }
            } finally {
                out.write("\n")
            }
        }
    }
}
