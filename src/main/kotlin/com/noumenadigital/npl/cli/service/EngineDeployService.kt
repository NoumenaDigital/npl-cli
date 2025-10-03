package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.exception.AuthorizationFailedException
import com.noumenadigital.npl.cli.exception.ClientSetupException
import com.noumenadigital.npl.cli.settings.LocalSettings
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

class DeployService {
    fun clearApplication(localSettings: LocalSettings): DeployResult =
        try {
            val context = setupClientContextInternal(localSettings)
            context.managementClient.clearApplicationContents(context.authProvider)
            DeployResult.ClearSuccess
        } catch (e: Exception) {
            DeployResult.ClearFailed(wrapException(localSettings, e))
        }

    fun deploySourcesAndMigrations(
        localSettings: LocalSettings,
        srcDir: String,
    ): DeployResult =
        try {
            val context = setupClientContextInternal(localSettings)
            context.managementClient.deploySourcesWithMigrations(
                sourceDirectory = srcDir,
                authorizationProvider = context.authProvider,
            )
            DeployResult.Success
        } catch (e: AuthorizationFailedAuthTokenException) {
            throw AuthorizationFailedException(
                message = e.message ?: "Authorization failed for $localSettings",
                cause = e,
            )
        } catch (e: Exception) {
            DeployResult.DeploymentFailed(wrapException(localSettings, e))
        }

    private fun setupClientContextInternal(localSettings: LocalSettings): ClientContext {
        try {
            val userConfig = UserConfiguration(localSettings.username!!, localSettings.password!!)
            val authConfig =
                AuthConfiguration(
                    clientId = localSettings.clientId ?: "",
                    clientSecret = localSettings.clientSecret ?: "",
                    authUrl = localSettings.authUrl,
                )
            val authProvider = TokenAuthorizationProvider(userConfig, authConfig)
            val managementClient = ManagementHttpClient(localSettings.managementUrl)

            return ClientContext(managementClient, authProvider)
        } catch (e: Exception) {
            throw ClientSetupException("Client setup failed: ${e.message}", e)
        }
    }

    private fun wrapException(
        targetConfig: LocalSettings,
        e: Exception,
    ): Exception =
        when (e) {
            is AuthorizationFailedAuthTokenException ->
                AuthorizationFailedException(
                    message = e.message ?: "Authorization failed for ${targetConfig.managementUrl}",
                    cause = e,
                )
            is ClientSetupException -> e
            else -> e
        }
}
