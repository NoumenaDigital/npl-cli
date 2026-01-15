package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.EnvironmentVariable
import com.noumenadigital.npl.cli.commands.NamedParameter
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
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
                            setType(parameter)
                            setDescription(parameter)
                        }
                    }
                },
            required = parameters.filter { it.isRequiredForMcp }.map { it.name },
        )

    fun JsonObjectBuilder.setType(parameter: NamedParameter) {
        if (parameter.takesValue) {
            if (parameter.isRequiredForMcp) {
                put("type", "string")
            } else {
                putJsonArray("type") {
                    add(JsonPrimitive("string"))
                    // Allow null for string parameters (indicates optional) to prevent LLM inference and
                    // ensure consistent CLI/MCP behavior via npl.yml properties.
                    add(JsonPrimitive("null"))
                }
            }
        } else {
            put("type", "boolean")
        }
    }

    fun JsonObjectBuilder.setDescription(parameter: NamedParameter) {
        val doNotInferMsg = " [STRICT: User provided only. Do not infer.]"
        val baseDesc =
            if (parameter.takesPath) {
                "${parameter.description} (should be an absolute path)"
            } else {
                parameter.description
            }

        // A do not defer message is put on all tool parameter descriptions, in order to prefer npl.yml settings
        val finalDesc = "$baseDesc$doNotInferMsg"

        put("description", finalDesc)
    }

    fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor
}
