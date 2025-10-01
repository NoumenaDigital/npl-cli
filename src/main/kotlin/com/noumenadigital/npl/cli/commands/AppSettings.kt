package com.noumenadigital.npl.cli.commands

import java.io.File

interface CommandConfig

data class AppSettings(
    val cloud: Cloud,
    val local: Local,
    val structure: Structure,
    val other: Other,
) {
    data class Cloud(
        val app: String? = null,
        val authUrl: String? = null,
        val clear: Boolean = false,
        val deploymentUrl: String? = null,
        val target: String? = null,
        val tenant: String? = null,
        val url: String? = null,
    )

    data class Local(
        val clientId: String? = null,
        val clientSecret: String? = null,
        val managementUrl: String? = null,
        val password: String? = null,
        val username: String? = null,
    )

    data class Structure(
        val frontEnd: File? = null,
        val migrationDescriptorFile: File? = null,
        val nplSourceDir: File? = null,
        val outputDir: File? = null,
        val rulesFile: File? = null,
        val testCoverage: Boolean = false,
        val testSourceDir: File? = null,
    )

    data class Other(
        val projectDir: File? = null,
        val templateUrl: String? = null,
        val minimal: Boolean = false,
    )
}
