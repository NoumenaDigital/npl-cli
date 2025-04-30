package com.noumenadigital.npl.cli

/**
 * Standard exit codes based on sysexits.h conventions.
 * See: https://man.openbsd.org/sysexits.3
 */
enum class ExitCode(
    val code: Int,
) {
    SUCCESS(0),
    GENERAL_ERROR(1),
    COMPILATION_ERROR(2),
    USAGE_ERROR(64),
    DATA_ERROR(65),
    NO_INPUT(66),
    INTERNAL_ERROR(70),
    IO_ERROR(74),
    CONFIG_ERROR(78),
}
