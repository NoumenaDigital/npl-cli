package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter

/**
 * Status command that shows which tenants and applications the user can access in NOUMENA Cloud.
 * 
 * Example output when logged in:
 * 
 * Accessible tenants and applications:
 * 
 * 🏢 Tenant: My Company (my-company)
 *    ├── 📱 Production App (prod-app)
 *    └── 📱 Development App (dev-app)
 * 
 * 🏢 Tenant: Demo Environment (demo-env)
 *    └── 📱 Demo Application (demo-app)
 * 
 * Total: 2 tenant(s), 3 application(s)
 * 
 * Usage:
 *   npl status [--url <cloud-url>]
 * 
 * The command requires the user to be logged in via 'npl cloud login' first.
 */
class StatusCommand(
    private val authManager: CloudAuthManager = CloudAuthManager(),
    private val args: List<String> = emptyList(),
) : CommandExecutor {
    override val commandName: String = "status"
    override val description: String = "Show which tenants and applications you can access in NOUMENA Cloud"

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "url",
                description = "NOUMENA Cloud URL",
                isRequired = false,
                valuePlaceholder = "<url>",
            ),
        )

    override fun createInstance(params: List<String>): CommandExecutor = StatusCommand(authManager, params)

    override fun execute(output: ColorWriter): ExitCode {
        try {
            val parsedArgs = CommandArgumentParser.parse(args, parameters)
            val url = parsedArgs.getValue("url") ?: "https://cloud.noumena.global"

            // Get access token
            val tokenResponse = authManager.getToken()
            val token = tokenResponse.accessToken
                ?: throw CloudCommandException("You are not logged in. Please run 'npl cloud login' first.")

            // Create a client to fetch tenants (we don't need app/tenant specific config for this)
            val client = NoumenaCloudClient(NoumenaCloudConfig.get("", "", url))
            val tenants = client.fetchTenants(token)

            if (tenants.isEmpty()) {
                output.info("No tenants accessible.")
                return ExitCode.SUCCESS
            }

            output.info("Accessible tenants and applications:")
            output.info("")

            tenants.forEach { tenant ->
                output.info("🏢 Tenant: ${tenant.name} (${tenant.slug})")
                
                if (tenant.applications.isEmpty()) {
                    output.info("   └── No applications")
                } else {
                    tenant.applications.forEachIndexed { index, app ->
                        val prefix = if (index == tenant.applications.size - 1) "   └──" else "   ├──"
                        output.info("$prefix 📱 ${app.name} (${app.slug})")
                    }
                }
                output.info("")
            }

            output.success("Total: ${tenants.size} tenant(s), ${tenants.sumOf { it.applications.size }} application(s)")
            return ExitCode.SUCCESS

        } catch (e: CloudCommandException) {
            throw e
        } catch (e: Exception) {
            throw CloudCommandException("Failed to fetch status: ${e.message ?: "Unknown error"}", e)
        }
    }
}