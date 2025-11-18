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
        val token = getAccessToken(output, actionName)
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.uploadApplicationArchive(token, archive, tenants)
    }

    fun deployFrontend(
        archive: ByteArray,
        output: ColorWriter,
        actionName: String,
    ) {
        val token = getAccessToken(output, actionName)
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.uploadFrontendArchive(token, archive, tenants)
    }

    fun clearApp(
        output: ColorWriter,
        actionName: String,
    ) {
        val token = getAccessToken(output, actionName)
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.clearApplication(token, tenants)
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
                throw IllegalStateException("Access token is not available. Please authenticate with `npl cloud login` command first.")
            }
            return token.accessToken
        }
    }
}
