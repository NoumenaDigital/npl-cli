package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.config.EngineTargetConfig
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

class DeployService {
    fun clearApplication(targetConfig: EngineTargetConfig): DeployResult =
        try {
            val context = setupClientContextInternal(targetConfig)
            context.managementClient.clearApplicationContents(context.authProvider)
            DeployResult.ClearSuccess
        } catch (e: Exception) {
            DeployResult.ClearFailed(wrapException(targetConfig, e))
        }

    fun deploySourcesAndMigrations(
        targetConfig: EngineTargetConfig,
        srcDir: String,
    ): DeployResult =
        try {
            val context = setupClientContextInternal(targetConfig)
            context.managementClient.deploySourcesWithMigrations(
                sourceDirectory = srcDir,
                authorizationProvider = context.authProvider,
            )
            DeployResult.Success
        } catch (e: AuthorizationFailedAuthTokenException) {
            throw AuthorizationFailedException(
                message = e.message ?: "Authorization failed for ${targetConfig.engineManagementUrl}",
                cause = e,
            )
        } catch (e: Exception) {
            DeployResult.DeploymentFailed(wrapException(targetConfig, e))
        }

    private fun setupClientContextInternal(targetConfig: EngineTargetConfig): ClientContext {
        try {
            val userConfig = UserConfiguration(targetConfig.username, targetConfig.password)
            val authConfig =
                AuthConfiguration(
                    clientId = targetConfig.clientId ?: "",
                    clientSecret = targetConfig.clientSecret ?: "",
                    authUrl = targetConfig.authUrl,
                )
            val authProvider = TokenAuthorizationProvider(userConfig, authConfig)
            val managementClient = ManagementHttpClient(targetConfig.engineManagementUrl)

            return ClientContext(managementClient, authProvider)
        } catch (e: Exception) {
            throw ClientSetupException("Client setup failed: ${e.message}", e)
        }
    }

    private fun wrapException(
        targetConfig: EngineTargetConfig,
        e: Exception,
    ): Exception =
        when (e) {
            is AuthorizationFailedAuthTokenException ->
                AuthorizationFailedException(
                    message = e.message ?: "Authorization failed for ${targetConfig.engineManagementUrl}",
                    cause = e,
                )
            is ClientSetupException -> e
            else -> e
        }
}
