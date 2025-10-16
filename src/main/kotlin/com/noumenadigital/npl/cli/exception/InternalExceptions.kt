package com.noumenadigital.npl.cli.exception

sealed class InternalException : RuntimeException()

class ArgumentParsingException(
    override val message: String,
) : InternalException()

class CommandParsingException(
    val commands: List<String> = emptyList(),
) : InternalException()

class CommandNotFoundException(
    val commandName: String,
) : InternalException()

class CommandExecutionException(
    override val message: String,
    override val cause: Throwable? = null,
) : InternalException()

class CommandValidationException(
    override val message: String,
) : InternalException()

data class DeployConfigException(
    override val message: String,
) : InternalException()

data class ClientSetupException(
    override val message: String,
    override val cause: Throwable? = null,
) : InternalException()

class RequiredParameterMissing(
    val usageInstruction: String? = null,
    val parameterNames: List<String>,
    val yamlExamples: List<String?> = emptyList(),
) : InternalException()

data class AuthorizationFailedException(
    override val message: String,
    override val cause: Throwable? = null,
) : InternalException()
