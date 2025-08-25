package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tenant(
    val name: String,
    val slug: String,
    val applications: List<Application>,
    val id: String? = null,
    val state: String? = null,
)
