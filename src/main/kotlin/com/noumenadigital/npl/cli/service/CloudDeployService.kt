package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.http.NoumenaCloudClient

class CloudDeployService(
    val cloudAuthManager: CloudAuthManager,
    val noumenaCloudClient: NoumenaCloudClient,
) {
    fun deployNplApplication(
        archive: ByteArray,
        output: ColorWriter,
        actionName: String,
    ) {
        val token = cloudAuthManager.getAccessToken(output, actionName, noumenaCloudClient.config.tenantSlug)
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.uploadApplicationArchive(token, archive, tenants)
    }

    fun deployFrontend(
        archive: ByteArray,
        output: ColorWriter,
        actionName: String,
    ) {
        val token = cloudAuthManager.getAccessToken(output, actionName, noumenaCloudClient.config.tenantSlug)
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.uploadFrontendArchive(token, archive, tenants)
    }

    fun clearApp(
        output: ColorWriter,
        actionName: String,
    ) {
        val token = cloudAuthManager.getAccessToken(output, actionName, noumenaCloudClient.config.tenantSlug)
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.clearApplication(token, tenants)
    }
}
