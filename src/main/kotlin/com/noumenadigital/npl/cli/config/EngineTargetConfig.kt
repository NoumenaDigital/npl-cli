package com.noumenadigital.npl.cli.config

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = EngineTargetConfig::class, name = "engine"),
)
sealed interface DeploymentTargetConfig

data class EngineTargetConfig(
    val engineManagementUrl: String = "http://localhost:12400/realms/noumena",
    val authUrl: String = "http://localhost:11000",
    val username: String,
    val password: String,
    val clientId: String? = null,
    val clientSecret: String? = null,
) : DeploymentTargetConfig
