package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.http.NoumenaCloudClient

class CloudDeployService(
    val cloudAuthManager: CloudAuthManager,
    val noumenaCloudClient: NoumenaCloudClient,
) {
    fun deployNplApplication(archive: ByteArray) {
        val token = getAccessToken()
        noumenaCloudClient.uploadApplicationArchive(token, archive)
    }

    fun clearApp() {
        val token = getAccessToken()
        noumenaCloudClient.clearApplication(token)
    }

    private fun getAccessToken(): String {
        val token = cloudAuthManager.getToken()
        if (token.accessToken == null) {
            throw IllegalStateException("Access token is not available. Please authenticate with `npl cloud login` command first.")
        }
        return token.accessToken
    }
}
