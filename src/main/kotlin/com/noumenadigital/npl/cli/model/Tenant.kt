package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tenant(
    val name: String,
    val slug: String,
    val applications: List<Application>,
)
