package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonProperty

data class DeviceCodeResponse(
    @JsonProperty("device_code") val deviceCode: String,
    @JsonProperty("user_code") val userCode: String,
    @JsonProperty("verification_uri") val verificationUri: String,
    @JsonProperty("verification_uri_complete") val verificationUriComplete: String,
    @JsonProperty("interval") val interval: Int,
    @JsonProperty("expires_in") val expiresIn: Int,
)
