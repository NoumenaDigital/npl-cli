package com.noumenadigital.npl.cli

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
    ): ExitCode {
        output.use { out ->
            try {
                return commandsParser.parse(inputArgs, baseDir).execute(out)
            } catch (ex: InternalException) {
                when (ex) {
                    is CommandNotFoundException -> {
                        output.write(ex.buildOutputMessage())
                        return ExitCode.CONFIG_ERROR
                    }
                    is CommandParsingException -> {
                        output.write(ex.buildOutputMessage())
                        return ExitCode.USAGE_ERROR
                    }
                    is CommandExecutionException -> {
                        output.write(ex.buildOutputMessage(inputArgs))
                        return ExitCode.INTERNAL_ERROR
                    }
                }
            } catch (ex: Exception) {
                output.write(ex.buildOutputMessage(inputArgs))
                return when {
                    ex is java.io.IOException -> ExitCode.IO_ERROR
                    ex.message?.contains("file", ignoreCase = true) == true -> ExitCode.NO_INPUT
                    else -> ExitCode.INTERNAL_ERROR
                }
            }
        }
    }
}
