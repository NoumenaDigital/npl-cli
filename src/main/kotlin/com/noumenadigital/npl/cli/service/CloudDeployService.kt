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

    fun clearApp() {
        val token = getAccessToken()
        val tenants = noumenaCloudClient.fetchTenants(token)
        noumenaCloudClient.clearApplication(token, tenants)
    }

    private fun getAccessToken(): String {
        val token = cloudAuthManager.getToken()
        if (token.accessToken == null) {
            throw IllegalStateException("Access token is not available. Please authenticate with `npl cloud login` command first.")
        }
        return token.accessToken
    }
}
