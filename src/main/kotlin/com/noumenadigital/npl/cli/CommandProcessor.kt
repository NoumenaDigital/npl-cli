package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.commands.registry.BaseDirectoryAware
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import com.noumenadigital.npl.cli.exception.CommandParsingException
import com.noumenadigital.npl.cli.exception.InternalException
import com.noumenadigital.npl.cli.exception.buildOutputMessage
import com.noumenadigital.npl.cli.service.CommandsParser
import java.io.Writer
import java.nio.file.Path

class CommandProcessor(
    private val commandsParser: CommandsParser = CommandsParser,
    private val baseDir: Path? = null,
) {
    fun process(
        inputArgs: List<String>,
        output: Writer,
    ) {
        output.use { out ->
            try {
                val command = commandsParser.parse(inputArgs)

                if (baseDir != null && command is BaseDirectoryAware) {
                    command.setBaseDirectory(baseDir)
                }

                command.execute(out)
            } catch (ex: InternalException) {
                when (ex) {
                    is CommandNotFoundException -> output.write(ex.buildOutputMessage())
                    is CommandParsingException -> output.write(ex.buildOutputMessage())
                    is CommandExecutionException -> output.write(ex.buildOutputMessage(inputArgs))
                }
            } catch (ex: Exception) {
                ex.buildOutputMessage(inputArgs)
            }
        }
    }
}
