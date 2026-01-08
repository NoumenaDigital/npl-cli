package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.http.NoumenaCloudClient
import com.noumenadigital.npl.cli.model.Application
import com.noumenadigital.npl.cli.model.CreateApplicationRequest

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
        val token = cloudAuthManager.getAccessToken(output, actionName, tenant)

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

    fun deleteApplication(
        output: ColorWriter,
        actionName: String,
        tenant: String,
        appSlug: String,
    ) {
        val token = cloudAuthManager.getAccessToken(output, actionName, tenant)

        val tenants = noumenaCloudClient.fetchTenants(token)
        val targetTenant =
            tenants.find { it.slug.equals(tenant, ignoreCase = true) }
                ?: throw IllegalArgumentException("Tenant '$tenant' not found")

        val application =
            targetTenant.applications.find { it.slug.equals(appSlug, ignoreCase = true) }
                ?: throw IllegalArgumentException("Application '$appSlug' not found in tenant '$tenant'")

        output.info("Deleting application '${application.name}' from tenant '$tenant'...")
        noumenaCloudClient.deleteApplication(token, application.id)
    }
}
