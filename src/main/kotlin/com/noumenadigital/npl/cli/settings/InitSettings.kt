package com.noumenadigital.npl.cli.settings

import java.io.File

data class InitSettings(
    val projectDir: File?,
    val bare: Boolean,
    val templateUrl: String?,
)
