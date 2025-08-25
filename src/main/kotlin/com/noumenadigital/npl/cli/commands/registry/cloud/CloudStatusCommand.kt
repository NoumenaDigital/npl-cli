package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthClient
import com.noumenadigital.npl.cli.http.NoumenaCloudAuthConfig
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter

class CloudStatusCommand(
    private val cloudAuthManager: CloudAuthManager = CloudAuthManager(),
    private val cloudClient: NoumenaCloudClient = NoumenaCloudClient(NoumenaCloudConfig()),
) : CommandExecutor {
    override val commandName: String = "cloud status"
    override val description: String = "Show tenants and applications for the logged-in user"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud base URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<url>",
            ),
            NamedParameter(
                name = "clientId",
                description = "OAuth2 Client ID",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientId>",
            ),
            NamedParameter(
                name = "clientSecret",
                description = "OAuth2 Client Secret",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<clientSecret>",
            ),
            NamedParameter(
                name = "authUrl",
                description = "NOUMENA Cloud Auth URL",
                isRequired = false,
                isHidden = true,
                valuePlaceholder = "<authUrl>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor {
        val parsedArgs = CommandArgumentParser.parse(params, parameters)
        val url = parsedArgs.getValue("url")
        val clientId = parsedArgs.getValue("clientId")
        val clientSecret = parsedArgs.getValue("clientSecret")
        val authUrl = parsedArgs.getValue("authUrl")

        val noumenaCloudAuthClient = NoumenaCloudAuthClient(NoumenaCloudAuthConfig.get(clientId, clientSecret, authUrl))
        val authManager = CloudAuthManager(noumenaCloudAuthClient)
        val client = NoumenaCloudClient(NoumenaCloudConfig.get(appSlug = "", tenantSlug = "", url = url))

        return CloudStatusCommand(authManager, client)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val token = cloudAuthManager.getToken()
            val accessToken = token.accessToken ?: throw CloudCommandException("No access token available. Please login again.")
            val tenants = cloudClient.fetchTenants(accessToken)

            if (tenants.isEmpty()) {
                output.info("No tenants found for the current user.")
                return ExitCode.SUCCESS
            }

            tenants.forEachIndexed { tenantIndex, tenant ->
                val tenantHeader = "Tenant: ${tenant.name} (${tenant.slug})" + (tenant.state?.let { " [${it}]" } ?: "")
                if (tenantIndex > 0) output.info()
                output.success(tenantHeader)

                if (tenant.applications.isEmpty()) {
                    output.info("  Applications: none")
                } else {
                    output.info("  Applications:")
                    tenant.applications.forEach { app ->
                        val appLine = buildString {
                            append("  - ${app.name} (${app.slug})")
                            app.state?.let { append(" [${it}]") }
                        }
                        output.info(appLine)

                        app.engineVersion?.version?.let { output.info("      engine: ${it}${if (app.engineVersion.deprecated == true) " (deprecated)" else ""}") }
                        app.links?.api?.let { output.info("      api: ${it}") }
                        app.links?.graphql?.let { output.info("      graphql: ${it}") }
                        app.links?.swagger?.let { output.info("      swagger: ${it}") }
                        app.links?.inspector?.let { output.info("      inspector: ${it}") }
                        app.websiteUrl?.let { output.info("      website: ${it}") }
                    }
                }
            }

            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud status")
        }
    }
}

