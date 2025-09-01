package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Links(
    val api: String? = null,
    val graphql: String? = null,
    val swagger: String? = null,
    val inspector: String? = null,
)

