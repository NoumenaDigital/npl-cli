package com.noumenadigital.npl.cli.settings

data class CloudSettings(
    val app: String? = null,
    val authUrl: String? = null,
    val clear: Boolean = false,
    val deploymentUrl: String? = null,
    val tenant: String? = null,
    val url: String? = null,
)
