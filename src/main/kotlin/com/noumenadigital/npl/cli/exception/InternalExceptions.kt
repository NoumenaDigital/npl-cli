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
    val isConnectionError: Boolean = false,
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

internal object ConnectionErrorPatterns {
    const val CONNECTION_REFUSED = "connection refused"
    const val CONNECTION_RESET = "connection reset"
    const val CONNECTION_TIMED_OUT = "connection timed out"
    const val FAILED_TO_CONNECT = "failed to connect"
    const val NO_ROUTE_TO_HOST = "no route to host"

    val PATTERNS =
        listOf(
            CONNECTION_REFUSED,
            CONNECTION_RESET,
            CONNECTION_TIMED_OUT,
            FAILED_TO_CONNECT,
            NO_ROUTE_TO_HOST,
        )
}
