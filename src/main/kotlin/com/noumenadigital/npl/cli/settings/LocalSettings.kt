package com.noumenadigital.npl.cli.settings

data class LocalSettings(
    val clientId: String? = null,
    val clientSecret: String? = null,
    val managementUrl: String? = null,
    val password: String? = null,
    val username: String? = null,
)
