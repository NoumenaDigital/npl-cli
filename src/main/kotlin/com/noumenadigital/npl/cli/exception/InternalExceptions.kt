package com.noumenadigital.npl.cli.exception

sealed class InternalException : RuntimeException()

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

data class DeployConfigException(
    override val message: String,
) : InternalException()

data class ClientSetupException(
    override val message: String,
    override val cause: Throwable? = null,
) : InternalException()

class RequiredParameterMissing(
    val parameterName: String,
) : InternalException()

data class AuthorizationFailedException(
    override val message: String,
    override val cause: Throwable? = null,
) : InternalException()
