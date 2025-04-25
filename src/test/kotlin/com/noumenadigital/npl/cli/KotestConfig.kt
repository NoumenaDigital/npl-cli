package com.noumenadigital.npl.cli

import io.kotest.core.config.AbstractProjectConfig

/**
 * Kotest project configuration to disable auto-scanning of the classpath.
 * This eliminates the warnings that appear during test execution.
 */
class KotestConfig : AbstractProjectConfig() {
    override val autoScanEnabled = false
}
