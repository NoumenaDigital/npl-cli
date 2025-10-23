package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.EnvironmentVariable
import com.noumenadigital.npl.cli.commands.NamedParameter
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

interface CommandDescriptor {
    val commandName: String
    val description: String
    val usageInstruction: String?
        get() = null
    val isParentCommand: Boolean
        get() = false

    val parameters: List<NamedParameter>
        get() = emptyList()

    val envVariables: List<EnvironmentVariable>
        get() = emptyList()

    val supportsMcp: Boolean
        get() = true

    fun toMcpToolInput(): Tool.Input =
        Tool.Input(
            properties =
                buildJsonObject {
                    parameters.filter { !it.isHidden }.forEach { parameter ->
                        putJsonObject(parameter.name) {
                            if (parameter.takesValue) {
                                put("type", "string")
                            } else {
                                put("type", "boolean")
                            }
                            val description =
                                if (parameter.takesPath) {
                                    "${parameter.description} (should be an absolute path)"
                                } else {
                                    parameter.description
                                }
                            put("description", description)
                        }
                    }
                },
            required = parameters.filter { it.isRequiredForMcp }.map { it.name },
        )

    fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor
}
