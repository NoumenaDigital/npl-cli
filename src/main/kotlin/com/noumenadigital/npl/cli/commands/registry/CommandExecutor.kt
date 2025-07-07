package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CommandParameter
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.service.ColorWriter
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

interface CommandExecutor {
    val commandName: String
    val description: String

    val parameters: List<CommandParameter>
        get() = emptyList()

    val supportsMcp: Boolean
        get() = true

    fun createInstance(params: List<String>): CommandExecutor = this

    fun execute(output: ColorWriter): ExitCode

    fun toMcpToolInput(): Tool.Input =
        Tool.Input(
            properties =
                buildJsonObject {
                    parameters.filter { !it.isHidden }.forEach { parameter ->
                        putJsonObject(parameter.name) {
                            when (parameter) {
                                is NamedParameter -> {
                                    if (parameter.takesValue) {
                                        put("type", "string")
                                    } else {
                                        put("type", "boolean")
                                    }
                                }
                            }
                            put("description", parameter.description)
                        }
                    }
                },
            required = parameters.filter { it.isRequired && !it.isHidden }.map { it.name },
        )
}
