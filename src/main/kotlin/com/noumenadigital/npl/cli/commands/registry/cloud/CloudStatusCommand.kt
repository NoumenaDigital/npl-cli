package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandDescriptor
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.model.Application
import com.noumenadigital.npl.cli.model.Tenant
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter

object CloudStatusCommandDescriptor : CommandDescriptor {
    override val commandName: String = "cloud status"
    override val description: String = "List tenants and applications with their status from NOUMENA Cloud"
    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud deployment URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<url>",
                configFilePath = YamlConfig.Cloud.url,
            ),
            NamedParameter(
                name = "client-id",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<client-id>",
                configFilePath = YamlConfig.Cloud.clientId,
            ),
            NamedParameter(
                name = "client-secret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<client-secret>",
                configFilePath = YamlConfig.Cloud.clientSecret,
            ),
            NamedParameter(
                name = "auth-url",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<auth-url>",
                configFilePath = YamlConfig.Cloud.authUrl,
            ),
        )

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val parsedUrl = parsedArguments["url"] as? String ?: "https://portal.noumena.cloud"
        val parsedClientId = parsedArguments["client-id"] as? String ?: "paas"
        val parsedClientSecret = parsedArguments["client-secret"] as? String ?: "paas"
        val parsedAuthUrl = parsedArguments["auth-url"] as? String ?: "https://keycloak.noumena.cloud/realms/paas"
        return CloudStatusCommand(
            url = parsedUrl,
            clientId = parsedClientId,
            clientSecret = parsedClientSecret,
            authUrl = parsedAuthUrl,
        )
    }
}

class CloudStatusCommand(
    private val url: String,
    private val clientId: String,
    private val clientSecret: String,
    authUrl: String,
) : CommandExecutor {
    private val noumenaCloudAuthConfig =
        NoumenaCloudAuthConfig.get(
            clientId = clientId,
            clientSecret = clientSecret,
            url = authUrl,
        )
    private val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
    private val cloudAuthManager = CloudAuthManager(noumenaCloudAuthClient)
    private val noumenaCloudClient = NoumenaCloudClient(NoumenaCloudConfig(url = url))

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val tokenResponse = cloudAuthManager.getToken()
            val token =
                tokenResponse.accessToken
                    ?: throw IllegalStateException(
                        "Access token is not available. Please authenticate with `npl cloud login` command first.",
                    )
            val tenants = noumenaCloudClient.fetchTenants(token)

            if (tenants.isEmpty()) {
                output.info("No tenants found.")
                return ExitCode.SUCCESS
            }

            tenants.forEach { tenant ->
                printTenant(output, tenant)
                tenant.applications.forEachIndexed { index, app ->
                    val isLast = index == tenant.applications.size - 1
                    printApplication(output, app, isLast)
                }
            }

            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud status")
        }
    }

    private fun printTenant(
        output: ColorWriter,
        tenant: Tenant,
    ) {
        val stateIndicator = getStateIndicator(tenant.state)
        val stateText = tenant.state?.let { " [$it]" } ?: ""
        output.info("📂 ${tenant.name} (${tenant.slug})$stateText $stateIndicator")
    }

    private fun printApplication(
        output: ColorWriter,
        app: Application,
        isLast: Boolean,
    ) {
        val prefix = if (isLast) "  └── " else "  ├── "
        val stateIndicator = getStateIndicator(app.state)
        val stateText = app.state?.let { " [$it]" } ?: ""
        output.info("$prefix📦 ${app.name} (${app.slug})$stateText $stateIndicator")
    }

    private fun getStateIndicator(state: String?): String =
        when (state?.lowercase()) {
            "active" -> "🟢"
            "pending" -> "🟡"
            "deactivated", "suspended", "unsubscribed", "deleted" -> "🔴"
            else -> "⚪"
        }
}
