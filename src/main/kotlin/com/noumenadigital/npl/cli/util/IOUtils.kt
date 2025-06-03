package com.noumenadigital.npl.cli.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

object IOUtils {
    val objectMapper = jacksonObjectMapper()

    fun writeObjectToFile(
        file: File,
        obj: Any,
    ) {
        file.parentFile?.mkdirs()
        objectMapper.writeValue(file, obj)
    }
}
