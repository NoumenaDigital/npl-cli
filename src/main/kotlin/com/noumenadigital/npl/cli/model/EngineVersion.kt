package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class EngineVersion(
    val version: String? = null,
    val deprecated: Boolean? = null,
)

