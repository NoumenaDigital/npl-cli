package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.commands.DeprecationNotifier
import com.noumenadigital.npl.cli.commands.registry.CommandsParser
import com.noumenadigital.npl.cli.exception.ArgumentParsingException
import com.noumenadigital.npl.cli.exception.AuthorizationFailedException
import com.noumenadigital.npl.cli.exception.ClientSetupException
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.exception.CommandExecutionException
import com.noumenadigital.npl.cli.exception.CommandNotFoundException
import com.noumenadigital.npl.cli.exception.CommandParsingException
import com.noumenadigital.npl.cli.exception.CommandValidationException
import com.noumenadigital.npl.cli.exception.DeployConfigException
import com.noumenadigital.npl.cli.exception.InternalException
import com.noumenadigital.npl.cli.exception.RequiredParameterMissing
import com.noumenadigital.npl.cli.exception.buildOutputMessage
import com.noumenadigital.npl.cli.service.ColorWriter

class CommandProcessor(
    private val commandsParser: CommandsParser = CommandsParser,
) {
    fun process(
        inputArgs: List<String>,
        output: ColorWriter,
    ): ExitCode {
        output.use { out ->
            try {
                DeprecationNotifier.setSink(out::warning)
                return commandsParser.parse(inputArgs).execute(out)
            } catch (ex: InternalException) {
                when (ex) {
                    is CommandNotFoundException -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.CONFIG_ERROR
                    }

                    is CommandParsingException -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.USAGE_ERROR
                    }

                    is ArgumentParsingException -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.USAGE_ERROR
                    }

                    is CommandValidationException -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.GENERAL_ERROR
                    }

                    is CommandExecutionException -> {
                        output.error(ex.buildOutputMessage(inputArgs))
                        return ExitCode.INTERNAL_ERROR
                    }

                    is DeployConfigException -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.CONFIG_ERROR
                    }

                    is ClientSetupException -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.GENERAL_ERROR
                    }

                    is AuthorizationFailedException -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.CONFIG_ERROR
                    }

                    is CloudCommandException -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.GENERAL_ERROR
                    }

                    is RequiredParameterMissing -> {
                        output.error(ex.buildOutputMessage())
                        return ExitCode.USAGE_ERROR
                    }
                }
            } catch (ex: Exception) {
                output.error(ex.buildOutputMessage(inputArgs))
                return when {
                    ex is java.io.IOException -> ExitCode.IO_ERROR
                    ex.message?.contains("file", ignoreCase = true) == true -> ExitCode.NO_INPUT
                    else -> ExitCode.INTERNAL_ERROR
                }
            } finally {
                DeprecationNotifier.setSink(null)
            }
        }
    }
}
