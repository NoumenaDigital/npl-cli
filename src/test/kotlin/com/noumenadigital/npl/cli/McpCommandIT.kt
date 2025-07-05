package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.ints.shouldBeNegative
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class McpCommandIT :
    FunSpec({
        context("MCP Server Integration Tests") {
            test("initialize successfully") {
                runMcpSession { session ->
                    val initResponse = session.initialize()

                    initResponse["jsonrpc"]?.jsonPrimitive?.content shouldBe "2.0"
                    initResponse["id"]?.jsonPrimitive?.int shouldBe 1
                    initResponse["result"] shouldNotBe null

                    val result = initResponse["result"]?.jsonObject
                    result?.get("protocolVersion")?.jsonPrimitive?.content shouldBe "2024-11-05"

                    val capabilities = result?.get("capabilities")?.jsonObject
                    capabilities
                        ?.get("tools")
                        ?.jsonObject
                        ?.get("listChanged")
                        ?.jsonPrimitive
                        ?.boolean shouldBe true
                }
            }

            test("listing available tools") {
                runMcpSession { session ->
                    session.initialize()
                    session.sendInitialized()

                    val listToolsResponse = session.listTools()

                    listToolsResponse["jsonrpc"]?.jsonPrimitive?.content shouldBe "2.0"
                    listToolsResponse["result"] shouldNotBe null

                    val tools = listToolsResponse["result"]?.jsonObject?.get("tools")?.jsonArray
                    tools shouldNotBe null

                    val toolNames = tools?.map { it.jsonObject["name"]?.jsonPrimitive?.content ?: "" } ?: emptyList()

                    toolNames shouldContainAll
                        listOf(
                            "npl_check",
                            "npl_test",
                            "npl_openapi",
                            "npl_puml",
                            "npl_deploy",
                            "npl_cloud login",
                            "npl_cloud logout",
                            "npl_cloud deploy",
                            "npl_cloud clear",
                        )

                    toolNames shouldNotContainAnyOf
                        listOf(
                            "npl_version",
                            "npl_help",
                            "npl_cloud",
                            "npl_cloud help",
                        )
                }
            }

            test("calling npl_check tool successfully") {
                runMcpSession { session ->
                    session.initialize()
                    session.sendInitialized()

                    val testDirPath = getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()

                    val checkResponse =
                        session.callTool(
                            toolName = "npl_check",
                            arguments =
                                buildJsonObject {
                                    put("directory", testDirPath)
                                },
                        )

                    checkResponse["jsonrpc"]?.jsonPrimitive?.content shouldBe "2.0"
                    checkResponse["result"] shouldNotBe null

                    val result = checkResponse["result"]?.jsonObject
                    val content =
                        result
                            ?.get("content")
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                    val text = content?.get("text")?.jsonPrimitive?.content

                    text shouldNotBe null
                    val responseJson = Json.parseToJsonElement(text!!)
                    responseJson.jsonObject["success"]?.jsonPrimitive?.boolean shouldBe true
                }
            }

            test("calling npl_test tool unsuccessfully") {
                runMcpSession { session ->
                    session.initialize()
                    session.sendInitialized()

                    val testDirPath = getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()

                    val testResponse =
                        session.callTool(
                            toolName = "npl_test",
                            arguments =
                                buildJsonObject {
                                    put("sourceDir", testDirPath)
                                    put("coverage", false)
                                },
                        )

                    testResponse["jsonrpc"]?.jsonPrimitive?.content shouldBe "2.0"
                    testResponse["result"] shouldNotBe null

                    val result = testResponse["result"]?.jsonObject
                    val content =
                        result
                            ?.get("content")
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                    val text = content?.get("text")?.jsonPrimitive?.content

                    text shouldNotBe null
                    val responseJson = Json.parseToJsonElement(text!!)
                    responseJson.jsonObject["success"]?.jsonPrimitive?.boolean shouldBe false
                }
            }

            test("should handle tool call with missing required parameter") {
                runMcpSession { session ->
                    session.initialize()
                    session.sendInitialized()

                    val checkResponse =
                        session.callTool(
                            toolName = "npl_cloud deploy",
                            arguments =
                                buildJsonObject {
                                    // Missing required 'directory' parameter
                                },
                        )

                    checkResponse["jsonrpc"]?.jsonPrimitive?.content shouldBe "2.0"
                    checkResponse["result"] shouldNotBe null

                    val result = checkResponse["result"]?.jsonObject
                    val content =
                        result
                            ?.get("content")
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                    val text = content?.get("text")?.jsonPrimitive?.content

                    text shouldNotBe null
                    val responseJson = Json.parseToJsonElement(text!!)
                    responseJson.jsonObject["success"]?.jsonPrimitive?.boolean shouldBe false
                    responseJson.jsonObject["error"]?.jsonPrimitive?.content shouldContain "Required parameter"
                }
            }

            test("should handle non-existent tool") {
                runMcpSession { session ->
                    session.initialize()
                    session.sendInitialized()

                    val response =
                        session.callTool(
                            toolName = "non_existent_tool",
                            arguments =
                                buildJsonObject {
                                    put("param", "value")
                                },
                        )

                    response["error"] shouldNotBe null
                    val error = response["error"]?.jsonObject
                    error
                        ?.get("code")
                        ?.jsonPrimitive
                        ?.int
                        ?.shouldBeNegative()
                }
            }
        }
    })

private fun runMcpSession(test: (McpSession) -> Unit) {
    val jarPath = getJarPath()

    val process =
        ProcessBuilder(
            "java",
            "--enable-native-access=ALL-UNNAMED",
            "-jar",
            jarPath,
            "mcp",
        ).start()

    try {
        // Give the process a moment to start up
        Thread.sleep(500)

        val session = McpSession(process)
        test(session)
    } finally {
        process.destroy()
    }
}

private fun getJarPath(): String {
    val rootDir = File(".").canonicalFile
    val jarPath = rootDir.resolve("target/npl-cli-jar-with-dependencies.jar").absolutePath

    if (!File(jarPath).exists()) {
        throw IllegalStateException(
            "JAR file not found at $jarPath. " +
                "Run 'mvn package -Pconfig-gen' to build the JAR file.",
        )
    }

    return jarPath
}

private class McpSession(
    process: Process,
) {
    private val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
    private val reader = BufferedReader(InputStreamReader(process.inputStream))
    private val errorReader = BufferedReader(InputStreamReader(process.errorStream))
    private var requestId = 0

    init {
        // Start a background thread to consume stderr to prevent blocking
        Thread {
            try {
                while (true) {
                    val line = errorReader.readLine() ?: break
                    // Log messages go to stderr, we can ignore them or log them for debugging
                    // System.err.println("MCP stderr: $line")
                }
            } catch (_: Exception) {
                // Ignore exceptions when process is terminated
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun initialize(): JsonObject {
        val request =
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", ++requestId)
                put("method", "initialize")
                putJsonObject("params") {
                    put("protocolVersion", "2025-06-18")
                    putJsonObject("capabilities") {
                        putJsonObject("tools") {
                            put("listChanged", true)
                        }
                    }
                    putJsonObject("clientInfo") {
                        put("name", "TestClient")
                        put("version", "1.0.0")
                    }
                }
            }

        return sendRequest(request)
    }

    fun sendInitialized() {
        val notification =
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "notifications/initialized")
            }

        sendNotification(notification)
    }

    fun listTools(): JsonObject {
        val request =
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", ++requestId)
                put("method", "tools/list")
            }

        return sendRequest(request)
    }

    fun callTool(
        toolName: String,
        arguments: JsonObject,
    ): JsonObject {
        val request =
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", ++requestId)
                put("method", "tools/call")
                putJsonObject("params") {
                    put("name", toolName)
                    put("arguments", arguments)
                }
            }

        return sendRequest(request)
    }

    private fun sendRequest(request: JsonObject): JsonObject {
        writer.write(request.toString())
        writer.newLine()
        writer.flush()

        val responseLine = reader.readLine()
        if (responseLine == null) {
            throw IOException("No response received from MCP server")
        }

        // Debug: print the response for troubleshooting
        System.err.println("MCP Response: $responseLine")

        if (responseLine.isBlank()) {
            throw IOException("Empty response received from MCP server")
        }

        return Json.parseToJsonElement(responseLine).jsonObject
    }

    private fun sendNotification(notification: JsonObject) {
        writer.write(notification.toString())
        writer.newLine()
        writer.flush()
    }
}
