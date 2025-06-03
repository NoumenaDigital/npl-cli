package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenResponse(
    @JsonProperty("refresh_token") val refreshToken: String,
)
