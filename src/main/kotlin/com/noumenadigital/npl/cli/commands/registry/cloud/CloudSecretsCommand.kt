package com.noumenadigital.npl.cli.commands.registry.cloud

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
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
import com.noumenadigital.npl.cli.service.CloudService
import com.noumenadigital.npl.cli.service.ColorWriter

class CloudSecretsCommand(
    private val cloudService: CloudService =
        CloudService(
            CloudAuthManager(),
            NoumenaCloudClient(NoumenaCloudConfig()),
        ),
) : CommandExecutor {
    override val commandName: String = "cloud secrets"
    override val description: String = "Retrieve application secrets from NOUMENA Cloud"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "app",
                description = "NOUMENA Cloud Application slug",
                isRequired = true,
                valuePlaceholder = "<app>",
            ),
            NamedParameter(
                name = "tenant",
                description = "NOUMENA Cloud Tenant slug",
                isRequired = true,
                valuePlaceholder = "<tenant>",
            ),
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud deployment URL",
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
        val app = parsedArgs.getRequiredValue("app")
        val tenant = parsedArgs.getRequiredValue("tenant")
        val clientId = parsedArgs.getValue("clientId")
        val clientSecret = parsedArgs.getValue("clientSecret")
        val authUrl = parsedArgs.getValue("authUrl")
        val url = parsedArgs.getValue("url")
        val noumenaCloudAuthConfig = NoumenaCloudAuthConfig.get(clientId, clientSecret, authUrl)
        val noumenaCloudAuthClient = NoumenaCloudAuthClient(noumenaCloudAuthConfig)
        val cloudService =
            CloudService(
                CloudAuthManager(noumenaCloudAuthClient),
                NoumenaCloudClient(NoumenaCloudConfig.get(app, tenant, url)),
            )
        return CloudSecretsCommand(cloudService = cloudService)
    }

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val secrets = cloudService.getAppSecrets()
            val objectMapper = ObjectMapper().registerKotlinModule()
            val jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(secrets)
            output.info(jsonOutput)
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud secrets")
        }
    }
}
