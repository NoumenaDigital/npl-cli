package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Application(
    val id: String,
    val name: String,
    val slug: String,
    @JsonProperty("engine_version") val engineVersion: EngineVersion? = null,
    val state: String? = null,
    val links: Links? = null,
    @JsonProperty("website_url") val websiteUrl: String? = null,
)
