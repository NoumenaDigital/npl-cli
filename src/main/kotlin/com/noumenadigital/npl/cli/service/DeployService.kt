package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.config.DeployConfig
import com.noumenadigital.npl.cli.config.EngineTargetConfig
import com.noumenadigital.npl.cli.exception.AuthorizationFailedException
import com.noumenadigital.npl.cli.exception.ClientSetupException
import com.noumenadigital.npl.cli.exception.DeployConfigException
import com.noumenadigital.platform.client.auth.AuthConfiguration
import com.noumenadigital.platform.client.auth.AuthorizationFailedAuthTokenException
import com.noumenadigital.platform.client.auth.TokenAuthorizationProvider
import com.noumenadigital.platform.client.auth.UserConfiguration
import com.noumenadigital.platform.client.engine.ManagementHttpClient

sealed class DeployResult {
    data class Success(
        val targetLabel: String,
    ) : DeployResult()

    data class ClearSuccess(
        val targetLabel: String,
    ) : DeployResult()

    data class DeploymentFailed(
        val targetLabel: String,
        val exception: Exception,
    ) : DeployResult()

    data class ClearFailed(
        val targetLabel: String,
        val exception: Exception,
    ) : DeployResult()
}

private data class ClientContext(
    val managementClient: ManagementHttpClient,
    val authProvider: TokenAuthorizationProvider,
)

class DeployService {
    private fun setupClientContext(targetLabel: String): ClientContext {
        val config = DeployConfig.load()
        val targetConfig =
            config.targets[targetLabel] as? EngineTargetConfig
                ?: throw DeployConfigException(
                    "Target '$targetLabel' configuration inconsistency. Validation passed but config not found during setup.",
                )

        try {
            val userConfig = UserConfiguration(targetConfig.username, targetConfig.password)
            val authConfig =
                AuthConfiguration(
                    clientId = targetConfig.clientId,
                    clientSecret = targetConfig.clientSecret,
                    authUrl = targetConfig.authUrl,
                )
            val authProvider = TokenAuthorizationProvider(userConfig, authConfig)
            val managementClient = ManagementHttpClient(targetConfig.engineManagementUrl)

            return ClientContext(managementClient, authProvider)
        } catch (e: Exception) {
            throw ClientSetupException("Client setup failed for target '$targetLabel': ${e.message}", e)
        }
    }

    fun clearApplication(targetLabel: String): DeployResult {
        val context = setupClientContext(targetLabel)
        return try {
            context.managementClient.clearApplicationContents(context.authProvider)
            DeployResult.ClearSuccess(targetLabel)
        } catch (e: Exception) {
            DeployResult.ClearFailed(targetLabel, e)
        }
    }

    fun deploySourcesAndMigrations(
        targetLabel: String,
        srcDir: String,
    ): DeployResult {
        val context = setupClientContext(targetLabel)

        return try {
            context.managementClient.deploySourcesWithMigrations(
                sourceDirectory = srcDir,
                authorizationProvider = context.authProvider,
            )
            DeployResult.Success(targetLabel)
        } catch (e: AuthorizationFailedAuthTokenException) {
            throw AuthorizationFailedException(
                message = e.message ?: "Authorization failed",
                cause = e,
            )
        } catch (e: Exception) {
            DeployResult.DeploymentFailed(targetLabel, e)
        }
    }
}
