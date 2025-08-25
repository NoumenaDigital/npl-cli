package com.noumenadigital.npl.cli.commands.registry.cloud

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.registry.CommandExecutor
import com.noumenadigital.npl.cli.exception.CloudCommandException
import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.http.NoumenaCloudConfig
import com.noumenadigital.npl.cli.service.CloudAuthManager
import com.noumenadigital.npl.cli.service.ColorWriter

class CloudStatusCommand(
    private val authManager: CloudAuthManager = CloudAuthManager(),
    private val cloudClient: NoumenaCloudClient = NoumenaCloudClient(NoumenaCloudConfig())
) : CommandExecutor {
    override val commandName: String = "cloud status"
    override val description: String = "Show logged-in user's tenants and applications with metadata"
    override val supportsMcp: Boolean = false

    override fun execute(output: ColorWriter): ExitCode {
        try {
            // Get authentication token
            val token = authManager.getToken()
            
            // Fetch tenants (which include applications)
            val tenants = cloudClient.fetchTenants(token.accessToken!!)
            
            if (tenants.isEmpty()) {
                output.info("No tenants found.")
                return ExitCode.SUCCESS
            }
            
            // Display tenants and applications
            output.info("Cloud Status:")
            output.info("=".repeat(50))
            
            tenants.forEach { tenant ->
                output.info("")
                output.success("Tenant: ${tenant.name}")
                output.info("  ID: ${tenant.id ?: "N/A"}")
                output.info("  Slug: ${tenant.slug}")
                if (tenant.state != null) {
                    output.info("  State: ${tenant.state}")
                }
                
                if (tenant.applications.isEmpty()) {
                    output.info("  No applications found")
                } else {
                    output.info("  Applications (${tenant.applications.size}):")
                    tenant.applications.forEach { app ->
                        output.info("    - ${app.name}")
                        output.info("      ID: ${app.id}")
                        output.info("      Slug: ${app.slug}")
                        if (app.state != null) {
                            output.info("      State: ${app.state}")
                        }
                        if (app.engineVersion != null) {
                            output.info("      Engine Version: ${app.engineVersion.version}${if (app.engineVersion.deprecated) " (deprecated)" else ""}")
                        }
                        if (app.deployedAt != null) {
                            output.info("      Deployed At: ${app.deployedAt}")
                        }
                        if (app.links?.api != null) {
                            output.info("      API URL: ${app.links.api}")
                        }
                        if (app.namespace != null) {
                            output.info("      Namespace: ${app.namespace}")
                        }
                    }
                }
            }
            
            return ExitCode.SUCCESS
        } catch (ex: Exception) {
            throw CloudCommandException(ex.message, ex, "cloud status")
        }
    }
}