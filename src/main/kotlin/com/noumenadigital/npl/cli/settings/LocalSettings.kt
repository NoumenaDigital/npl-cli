package com.noumenadigital.npl.cli.settings

data class LocalSettings(
    val managementUrl: String,
    val authUrl: String,
    val password: String? = null,
    val username: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val clear: Boolean = false,
)
