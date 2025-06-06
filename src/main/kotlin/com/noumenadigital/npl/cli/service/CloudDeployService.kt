package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.http.NoumenaCloudClient

class CloudDeployService(
    val cloudAuthManager: CloudAuthManager,
    val noumenaCloudClient: NoumenaCloudClient,
) {
    fun deployNplApplication(archive: ByteArray) {
        val token = cloudAuthManager.getAccessAccessToken()
        if (token.accessToken == null) {
            throw IllegalStateException("Access token is not available. Please authenticate with `npl cloud login` command first.")
        }
        noumenaCloudClient.uploadApplicationArchive(token.accessToken, archive)
    }
}
