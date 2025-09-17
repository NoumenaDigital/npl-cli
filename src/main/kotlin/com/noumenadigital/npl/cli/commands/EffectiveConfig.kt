package com.noumenadigital.npl.cli.commands

import com.noumenadigital.npl.cli.config.YamlConfig
import java.io.File

interface AppConfig

class EmptyAppConfig : AppConfig

data class EffectiveConfig(
    val nplSourceDir: File? = null,
    val testSourceDir: File? = null,
    val outputDir: File? = null,
    val rulesPath: File? = null,
    val deployTarget: String? = null,
    val deployConfig: File? = null,
    val clear: Boolean = false,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val cloudApp: String? = null,
    val cloudTenant: String? = null,
    val authUrl: String? = null,
    val deploymentUrl: String? = null,
    val portalUrl: String? = null,
    val frontend: String? = null,
    val managementUrl: String? = null,
    val username: String? = null,
    val password: String? = null,
    val migrationDescriptorPath: File? = null,
    val projectDir: File? = null,
    val bare: Boolean = false,
    val templateUrl: String? = null,
    val withCoverage: Boolean = false,
) {
    fun <T : AppConfig> toConfig(transform: (EffectiveConfig) -> T): T = transform(this)

    companion object {
        private fun String.toFile(): File = File(this)

        fun from(
            args: CommandArgumentParser.ParsedArguments,
            yamlConfig: YamlConfig?,
        ): EffectiveConfig {
            fun Boolean.orElse(default: Boolean?): Boolean = (takeIf { it } ?: default) == true

            return EffectiveConfig(
                // local
                nplSourceDir = args.getValueOrElse("source-dir", yamlConfig?.structure?.nplSources)?.toFile(),
                testSourceDir =
                    args.getValueOrElse("test-source-dir", yamlConfig?.structure?.nplTestSources)?.toFile(),
                outputDir = args.getValueOrElse("output", yamlConfig?.structure?.output)?.toFile(),
                rulesPath = args.getValueOrElse("rules", yamlConfig?.structure?.rules)?.toFile(),
                // cloud
                clientId = args.getValueOrElse("client-id", yamlConfig?.local?.clientId),
                clientSecret = args.getValueOrElse("client-secret", yamlConfig?.local?.clientSecret),
                cloudApp = args.getValueOrElse("application", yamlConfig?.cloud?.application),
                cloudTenant = args.getValueOrElse("tenant", yamlConfig?.cloud?.tenant),
                deployTarget = args.getValueOrElse("target", yamlConfig?.cloud?.deployTarget),
                authUrl = args.getValueOrElse("auth-url", yamlConfig?.cloud?.authUrl),
                portalUrl = args.getValueOrElse("portal-url", yamlConfig?.cloud?.portalUrl),
                frontend = args.getValueOrElse("front-end", yamlConfig?.structure?.frontEndUrl),
                managementUrl = args.getValueOrElse("management-url", yamlConfig?.local?.managementUrl),
                username = args.getValueOrElse("username", yamlConfig?.local?.username),
                password = args.getValueOrElse("password", yamlConfig?.local?.password),
                // not sure where to put these
                migrationDescriptorPath =
                    args.getValueOrElse("migration-descriptor", yamlConfig?.structure?.nplMigrationDescriptor)?.toFile(),
                projectDir = args.getValueOrElse("project-dir", yamlConfig?.structure?.projectDir)?.toFile(),
                withCoverage = args.hasFlag("coverage").orElse(yamlConfig?.local?.withCoverage),
                clear = args.hasFlag("clear").orElse(yamlConfig?.local?.withCoverage),
                templateUrl = args.getValueOrElse("template-url", yamlConfig?.local?.templateUrl),
            )
        }
    }
}
