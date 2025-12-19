package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.model.Application
import com.noumenadigital.npl.cli.model.CreateApplicationRequest
import com.noumenadigital.npl.cli.model.Tenant

data class CreateApplicationResult(
    val application: Application,
    val engineVersion: String,
    val provider: String,
    val trustedIssuers: List<String>,
)

class CloudApplicationService(
    private val cloudAuthManager: CloudAuthManager,
    private val noumenaCloudClient: NoumenaCloudClient,
) {
    fun createApplication(
        output: ColorWriter,
        actionName: String,
        tenant: String,
        appName: String,
        provider: String,
        engineVersion: String?,
        trustedIssuers: String?,
    ): CreateApplicationResult {
        val token = getAccessToken(output, actionName)

        output.info("Fetching tenants...")
        val tenants = noumenaCloudClient.fetchTenants(token)
        val targetTenant =
            tenants.find { it.slug.equals(tenant, ignoreCase = true) }
                ?: throw IllegalArgumentException("Tenant '$tenant' not found")

        val tenantId =
            targetTenant.id
                ?: throw IllegalStateException("Tenant ID is not available for tenant '$tenant'")

        output.info("Generating app slug from name...")
        val appSlug = noumenaCloudClient.generateSlug(token, appName)

        val finalEngineVersion =
            engineVersion ?: run {
                output.info("Fetching latest engine version...")
                val versions = noumenaCloudClient.fetchEngineVersions(token)
                if (versions.isEmpty()) {
                    throw IllegalStateException("No engine versions available")
                }
                versions.first()
            }

        val finalTrustedIssuers =
            if (trustedIssuers != null) {
                trustedIssuers.split(",").map { it.trim() }
            } else {
                listOf("https://keycloak-$tenant-$appSlug.noumena.cloud/realms/$appSlug")
            }

        output.info("Creating application '$appName' in tenant '$tenant'...")
        val request =
            CreateApplicationRequest(
                name = appName,
                provider = provider,
                engineVersion = finalEngineVersion,
                trustedIssuers = finalTrustedIssuers,
            )

        val application = noumenaCloudClient.createApplication(token, tenantId, request)

        return CreateApplicationResult(
            application = application,
            engineVersion = finalEngineVersion,
            provider = provider,
            trustedIssuers = finalTrustedIssuers,
        )
    }

    fun listTenantsAndApplications(
        output: ColorWriter,
        actionName: String,
    ) {
        val token = getAccessToken(output, actionName)
        val tenants = noumenaCloudClient.fetchTenants(token)

        if (tenants.isEmpty()) {
            output.info("No tenants found.")
            return
        }

        tenants.forEach { tenant ->
            printTenant(output, tenant)
            tenant.applications.forEach { app ->
                printApplication(output, app)
            }
        }
    }

    private fun printTenant(
        output: ColorWriter,
        tenant: Tenant,
    ) {
        output.info("Tenant: ${tenant.name} (${tenant.slug})")
    }

    private fun printApplication(
        output: ColorWriter,
        app: Application,
    ) {
        output.info("  - ${app.name} (${app.slug})")
    }

    private fun getAccessToken(
        output: ColorWriter,
        actionName: String,
    ): String {
        val saClientId = noumenaCloudClient.config.tenantSlug
        val saClientSecret =
            System.getenv("NPL_SERVICE_ACCOUNT_CLIENT_SECRET")
                ?: System.getProperty("NPL_SERVICE_ACCOUNT_CLIENT_SECRET")

        if (!saClientSecret.isNullOrBlank()) {
            output.info("Preparing to $actionName using service account...")
            return cloudAuthManager.getServiceAccountAccessToken(output, saClientId, saClientSecret)
        } else {
            val token = cloudAuthManager.getToken()
            if (token.accessToken == null) {
                throw IllegalStateException(
                    "Access token is not available. Please authenticate with `npl cloud login` command first.",
                )
            }
            return token.accessToken
        }
    }
}
