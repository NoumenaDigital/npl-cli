package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tenant(
    val id: String? = null,
    val name: String,
    val slug: String,
    val state: String? = null,
    val applications: List<Application>,
)
