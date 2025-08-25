package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Application(
    val id: String,
    val name: String,
    val slug: String,
    @JsonProperty("owner_id")
    val ownerId: String? = null,
    val state: String? = null,
    @JsonProperty("engine_version")
    val engineVersion: EngineVersion? = null,
    @JsonProperty("deployed_at")
    val deployedAt: String? = null,
    val links: ApplicationLinks? = null,
    @JsonProperty("trusted_issuers")
    val trustedIssuers: List<String> = emptyList(),
    @JsonProperty("backup_records")
    val backupRecords: List<String> = emptyList(),
    val namespace: String? = null,
    @JsonProperty("configuration_id")
    val configurationId: String? = null,
    @JsonProperty("deleted_at")
    val deletedAt: String? = null,
    @JsonProperty("add_ons")
    val addOns: List<String> = emptyList(),
    @JsonProperty("website_deployed_at")
    val websiteDeployedAt: String? = null,
    @JsonProperty("website_url")
    val websiteUrl: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EngineVersion(
    val version: String,
    val deprecated: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApplicationLinks(
    val api: String? = null,
    val graphql: String? = null,
    val swagger: String? = null,
    val inspector: String? = null,
    val keycloak: String? = null,
)
