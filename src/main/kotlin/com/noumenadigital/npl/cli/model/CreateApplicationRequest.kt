package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateApplicationRequest(
    val name: String,
    val provider: String,
    @JsonProperty("engine_version")
    val engineVersion: String,
    @JsonProperty("trusted_issuers")
    val trustedIssuers: List<String>,
)
