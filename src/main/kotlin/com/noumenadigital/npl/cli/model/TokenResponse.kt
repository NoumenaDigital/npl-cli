package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String?,
    @JsonProperty("expires_in") val expiresIn: Number?,
    @JsonProperty("refresh_expires_in") val refreshExpiresIn: Number?,
    @JsonProperty("refresh_token") val refreshToken: String,
    @JsonProperty("token_type") val tokenType: String?,
)
