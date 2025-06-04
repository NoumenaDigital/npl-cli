package com.noumenadigital.npl.cli.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object IOUtils {
    val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun writeObjectToFile(
        file: File,
        obj: Any,
    ) {
        file.parentFile?.mkdirs()
        objectMapper.writeValue(file, obj)
    }
}
