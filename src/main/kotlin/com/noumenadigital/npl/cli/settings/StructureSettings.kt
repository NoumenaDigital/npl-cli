package com.noumenadigital.npl.cli.settings

import java.io.File

data class StructureSettings(
    val frontEnd: File? = null,
    val migrationDescriptorFile: File? = null,
    val nplSourceDir: File? = null,
    val outputDir: File? = null,
    val rulesFile: File? = null,
    val testCoverage: Boolean = false,
    val testSourceDir: File? = null,
)
