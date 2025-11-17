package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.AuthorizationFailedException
import com.noumenadigital.npl.cli.exception.ClientSetupException
import com.noumenadigital.platform.client.auth.AuthConfiguration
import com.noumenadigital.platform.client.auth.AuthorizationFailedAuthTokenException
import com.noumenadigital.platform.client.auth.TokenAuthorizationProvider
import com.noumenadigital.platform.client.auth.UserConfiguration
import com.noumenadigital.platform.client.engine.ManagementHttpClient
import java.net.ConnectException
import java.net.SocketTimeoutException

private object ConnectionErrorPatterns {
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

sealed class DeployResult {
    data object Success : DeployResult()

    data object ClearSuccess : DeployResult()

    data class DeploymentFailed(
        val exception: Exception,
    ) : DeployResult()

    data class ClearFailed(
        val exception: Exception,
    ) : DeployResult()
}

private data class ClientContext(
    val managementClient: ManagementHttpClient,
    val authProvider: TokenAuthorizationProvider,
)

class DeployService(
    val clientId: String,
    val clientSecret: String,
    val authUrl: String,
    val managementUrl: String,
    val username: String,
    val password: String,
) {
    fun clearApplication(): DeployResult =
        try {
            val context = setupClientContextInternal()
            context.managementClient.clearApplicationContents(context.authProvider)
            DeployResult.ClearSuccess
        } catch (e: Exception) {
            DeployResult.ClearFailed(wrapException(e))
        }

    fun deploySourcesAndMigrations(srcDir: String): DeployResult {
        return try {
            val context = setupClientContextInternal()
            context.managementClient.deploySourcesWithMigrations(
                sourceDirectory = srcDir,
                authorizationProvider = context.authProvider,
            )
            DeployResult.Success
        } catch (e: AuthorizationFailedAuthTokenException) {
            if (isConnectionError(e)) return DeployResult.DeploymentFailed(wrapException(e))
            throw AuthorizationFailedException(
                message =
                    e.message
                        ?: "Authorization failed for clientId: $clientId, authUrl: $authUrl, username: $username",
                cause = e,
            )
        } catch (e: Exception) {
            DeployResult.DeploymentFailed(wrapException(e))
        }
    }

    private fun setupClientContextInternal(): ClientContext {
        try {
            val userConfig = UserConfiguration(username, password)
            val authConfig =
                AuthConfiguration(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    authUrl = authUrl,
                )
            val authProvider = TokenAuthorizationProvider(userConfig, authConfig)
            val managementClient = ManagementHttpClient(managementUrl)

            return ClientContext(managementClient, authProvider)
        } catch (e: Exception) {
            if (isConnectionError(e)) {
                throw ClientSetupException(
                    message = buildConnectionErrorMessage(),
                    cause = e,
                    isConnectionError = true,
                )
            } else {
                throw ClientSetupException("Client setup failed: ${e.message}", e)
            }
        }
    }

    private fun wrapException(e: Exception): Exception =
        when (e) {
            is AuthorizationFailedAuthTokenException -> {
                if (isConnectionError(e)) {
                    ClientSetupException(
                        message = buildConnectionErrorMessage(),
                        cause = e,
                        isConnectionError = true,
                    )
                } else {
                    AuthorizationFailedException(
                        message = e.message ?: "Authorization failed for $managementUrl",
                        cause = e,
                    )
                }
            }

            is ClientSetupException -> e
            else -> {
                if (isConnectionError(e)) {
                    ClientSetupException(
                        message = buildConnectionErrorMessage(),
                        cause = e,
                        isConnectionError = true,
                    )
                } else {
                    e
                }
            }
        }

    private fun buildConnectionErrorMessage(): String {
        val urlList = formatServiceUrls()
        return "Engine or authorization service not found at $urlList. Please check that the service is running, healthy and accessible."
    }

    private fun formatServiceUrls(): String {
        val urls = getServiceUrls()
        return urls.joinToString(" or ") { "`$it`" }
    }

    private fun getServiceUrls(): List<String> = listOfNotNull(authUrl, managementUrl).distinct()

    private fun isConnectionError(e: Exception): Boolean {
        fun isConnectionException(ex: Throwable?): Boolean {
            if (ex == null) return false

            return when (ex) {
                is ConnectException -> true
                is SocketTimeoutException -> true
                else -> {
                    val message = ex.message ?: ""
                    ConnectionErrorPatterns.PATTERNS.any { pattern ->
                        message.contains(pattern, ignoreCase = true)
                    }
                }
            }
        }

        return isConnectionException(e) || isConnectionException(e.cause)
    }
}
