package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.AuthorizationFailedException
import com.noumenadigital.npl.cli.exception.ClientSetupException
import com.noumenadigital.platform.client.auth.AuthConfiguration
import com.noumenadigital.platform.client.auth.AuthorizationFailedAuthTokenException
import com.noumenadigital.platform.client.auth.TokenAuthorizationProvider
import com.noumenadigital.platform.client.auth.UserConfiguration
import com.noumenadigital.platform.client.engine.ManagementHttpClient

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

    fun deploySourcesAndMigrations(srcDir: String): DeployResult =
        try {
            val context = setupClientContextInternal()
            context.managementClient.deploySourcesWithMigrations(
                sourceDirectory = srcDir,
                authorizationProvider = context.authProvider,
            )
            DeployResult.Success
        } catch (e: AuthorizationFailedAuthTokenException) {
            throw AuthorizationFailedException(
                message =
                    e.message
                        ?: "Authorization failed for clientId: $clientId, authUrl: $authUrl, username: $username",
                cause = e,
            )
        } catch (e: Exception) {
            DeployResult.DeploymentFailed(wrapException(e))
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
            throw ClientSetupException("Client setup failed: ${e.message}", e)
        }
    }

    private fun wrapException(e: Exception): Exception =
        when (e) {
            is AuthorizationFailedAuthTokenException ->
                AuthorizationFailedException(
                    message = e.message ?: "Authorization failed for $managementUrl",
                    cause = e,
                )

            is ClientSetupException -> e
            else -> e
        }
}
