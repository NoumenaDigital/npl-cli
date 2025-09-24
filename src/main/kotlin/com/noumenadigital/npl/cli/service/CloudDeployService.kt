package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.http.NoumenaCloudClient

class CloudDeployService(
    val cloudAuthManager: CloudAuthManager,
    val noumenaCloudClient: NoumenaCloudClient,
) {
    fun deployNplApplication(archive: ByteArray) {
        val token = getAccessToken()
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.uploadApplicationArchive(token, archive, tenants)
    }

    fun deployFrontend(archive: ByteArray) {
        val token = getAccessToken()
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.uploadFrontendArchive(token, archive, tenants)
    }

    fun clearApp() {
        val token = getAccessToken()
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.clearApplication(token, tenants)
    }

    fun deployNplApplicationWithToken(
        archive: ByteArray,
        accessToken: String,
    ) {
        val tenants = noumenaCloudClient.fetchTenants(accessToken)
        noumenaCloudClient.uploadApplicationArchive(accessToken, archive, tenants)
    }

    fun deployFrontendWithToken(
        archive: ByteArray,
        accessToken: String,
    ) {
        val tenants = noumenaCloudClient.fetchTenants(accessToken)
        noumenaCloudClient.uploadFrontendArchive(accessToken, archive, tenants)
    }

    private fun getAccessToken(): String {
        val token = cloudAuthManager.getToken()
        if (token.accessToken == null) {
            throw IllegalStateException("Access token is not available. Please authenticate with `npl cloud login` command first.")
        }
        return token.accessToken
    }
}
