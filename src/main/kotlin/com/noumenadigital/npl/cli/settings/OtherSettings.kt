package com.noumenadigital.npl.cli.settings

import java.io.File

data class OtherSettings(
    val projectDir: File? = null,
    val templateUrl: String? = null,
    val minimal: Boolean = false,
)
