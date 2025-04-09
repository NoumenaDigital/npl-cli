package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import com.noumenadigital.npl.cli.exception.CommandParsingException
import com.noumenadigital.npl.cli.exception.InternalException
import com.noumenadigital.npl.cli.exception.errorMessage
import com.noumenadigital.npl.cli.exception.genericErrorMessage
import com.noumenadigital.npl.cli.service.CommandsParser
import java.io.Writer

class CommandProcessor(
    private val commandsParser: CommandsParser = CommandsParser,
) {
    companion object {
        private const val START_MESSAGE = "Executing command '%s'...\n"
        private const val END_MESSAGE_SUCCESS = "\nCommand '%s' finished SUCCESSFULLY."
    }

    fun process(
        inputArgs: List<String>,
        output: Writer,
    ) {
        output.use { out ->
            try {
                val command = commandsParser.parse(inputArgs)
                out.write(START_MESSAGE.format(command.commandName))
                command.execute(out)
                out.write(END_MESSAGE_SUCCESS.format(command.commandName))
            } catch (ex: InternalException) {
                when (ex) {
                    is CommandNotFoundException -> output.write(ex.errorMessage())
                    is CommandParsingException -> output.write(ex.errorMessage())
                }
            } catch (ex: Exception) {
                ex.genericErrorMessage(inputArgs)
            }
        }
    }
}
