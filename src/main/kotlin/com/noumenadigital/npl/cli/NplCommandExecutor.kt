package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.exception.NplCliException
import com.noumenadigital.npl.cli.service.CommandExecutorOutput
import com.noumenadigital.npl.cli.service.NplCommandsParser

class NplCommandExecutor(
    private val commandsParser: NplCommandsParser = NplCommandsParser,
) {
    companion object {
        private const val BEFORE_RESOLUTION_PADDING = "\n\n"
        private const val START_MESSAGE = "Executing command '%s'...\n"
        private const val END_MESSAGE_SUCCESS = "${BEFORE_RESOLUTION_PADDING}Command '%s' finished SUCCESSFULLY.\n"
        private const val END_GENERIC_ERROR_WRAPPER = "Executing command FAILED with error %s"
    }

    fun process(
        inputArgs: List<String>,
        output: CommandExecutorOutput,
    ) {
        output.get().use { out ->
            try {
                val command = commandsParser.parse(inputArgs)
                out.write(START_MESSAGE.format(command.commandName))
                command.execute(out)
                out.write(END_MESSAGE_SUCCESS.format(command.commandName))
            } catch (ex: NplCliException) {
                ex.message?.let { out.write(it) }
            } catch (ex: Exception) {
                ex.message?.let { out.write(END_GENERIC_ERROR_WRAPPER.format(ex.stackTraceToString())) }
            }
        }
    }
}
