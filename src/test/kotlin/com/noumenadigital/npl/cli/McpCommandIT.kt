package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.getTestResourcesPath
import com.noumenadigital.npl.cli.TestUtils.runMcpSessionDirect
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
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class McpCommandIT :
    FunSpec({
        context("MCP server") {
            test("should list available tools") {
                runMcpSession { session ->
                    session.initialize()
                    session.sendInitialized()

                    val toolsResponse = session.listTools()

                    toolsResponse["jsonrpc"]?.jsonPrimitive?.content shouldBe "2.0"
                    toolsResponse["result"] shouldNotBe null

                    val result = toolsResponse["result"]?.jsonObject
                    val tools = result?.get("tools")?.jsonArray

                    tools shouldNotBe null
                    val toolNames = tools!!.map { it.jsonObject["name"]?.jsonPrimitive?.content ?: "" }

                    toolNames shouldContainAll
                        listOf(
                            "check",
                            "test",
                            "openapi",
                            "puml",
                            "deploy",
                        )

                    toolNames shouldContainAll
                        listOf(
                            "cloud_login",
                            "cloud_logout",
                            "cloud_deploy",
                            "cloud_clear",
                        )

                    toolNames shouldNotContainAnyOf
                        listOf(
                            "version",
                            "help",
                            "cloud",
                            "cloud_help",
                        )
                }
            }

            test("calling check tool successfully") {
                runMcpSession { session ->
                    session.initialize()
                    session.sendInitialized()

                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()

                    val checkResponse =
                        session.callTool(
                            toolName = "check",
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

            test("calling test tool unsuccessfully") {
                runMcpSession { session ->
                    session.initialize()
                    session.sendInitialized()

                    val testDirPath =
                        getTestResourcesPath(listOf("success", "both_sources")).toAbsolutePath().toString()

                    val testResponse =
                        session.callTool(
                            toolName = "test",
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
                            toolName = "cloud_deploy",
                            arguments =
                                buildJsonObject {
                                    // Missing required '--app' and '--tenant' parameters
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

private fun runMcpSession(test: (McpSessionInterface) -> Unit) {
    val testMode = System.getenv().getOrDefault("TEST_MODE", "direct")

    when (testMode) {
        "direct" -> {
            runMcpSessionDirect { session ->
                test(
                    object : McpSessionInterface {
                        override fun initialize(): JsonObject = session.initialize()

                        override fun sendInitialized() = session.sendInitialized()

                        override fun listTools(): JsonObject = session.listTools()

                        override fun callTool(
                            toolName: String,
                            arguments: JsonObject,
                        ): JsonObject = session.callTool(toolName, arguments)
                    },
                )
            }
        }

        "binary", "jar" -> {
            runMcpSessionWithProcess { session ->
                test(
                    object : McpSessionInterface {
                        override fun initialize(): JsonObject = session.initialize()

                        override fun sendInitialized() = session.sendInitialized()

                        override fun listTools(): JsonObject = session.listTools()

                        override fun callTool(
                            toolName: String,
                            arguments: JsonObject,
                        ): JsonObject = session.callTool(toolName, arguments)
                    },
                )
            }
        }

        else -> {
            throw IllegalArgumentException("Unknown test mode: $testMode")
        }
    }
}

interface McpSessionInterface {
    fun initialize(): JsonObject

    fun sendInitialized()

    fun listTools(): JsonObject

    fun callTool(
        toolName: String,
        arguments: JsonObject,
    ): JsonObject
}

private fun runMcpSessionWithProcess(test: (McpSession) -> Unit) {
    val testMode = System.getenv().getOrDefault("TEST_MODE", "direct")

    val process =
        when (testMode) {
            "binary" -> {
                val nplPath = getNplPath()
                ProcessBuilder(nplPath, "mcp")
                    .redirectErrorStream(false)
                    .start()
            }

            "jar" -> {
                val jarPath = getJarPath()
                if (!java.io.File(jarPath).exists()) {
                    throw IllegalStateException(
                        "JAR file not found at $jarPath. " +
                            "Run 'mvn package -Pconfig-gen' to build the JAR file.",
                    )
                }
                ProcessBuilder(
                    "java",
                    "--enable-native-access=ALL-UNNAMED",
                    "-jar",
                    jarPath,
                    "mcp",
                ).redirectErrorStream(false).start()
            }

            else -> throw IllegalArgumentException("Unsupported test mode for MCP: $testMode")
        }

    try {
        // Give the process a moment to start up
        Thread.sleep(500)

        val session = McpSession(process)
        test(session)
    } finally {
        process.destroy()
    }
}

private fun getNplPath(): String {
    val rootDir = java.io.File(".").canonicalFile
    val targetDir = rootDir.resolve("target")

    val candidates =
        targetDir.listFiles { _, name ->
            name == "npl" || name.startsWith("npl-")
        }

    if (!candidates.isNullOrEmpty()) {
        return candidates.maxByOrNull(java.io.File::lastModified)?.absolutePath!!
    }

    throw IllegalStateException("Cannot locate NPL native binary in ${targetDir.absolutePath}")
}

private fun getJarPath(): String {
    val rootDir = java.io.File(".").canonicalFile
    return rootDir.resolve("target/npl-cli-jar-with-dependencies.jar").absolutePath
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
                // Process closed
            }
        }.start()
    }

    fun initialize(): JsonObject {
        val request =
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", ++requestId)
                put("method", "initialize")
                put(
                    "params",
                    buildJsonObject {
                        put("protocolVersion", "2025-06-18")
                        put(
                            "capabilities",
                            buildJsonObject {
                                put(
                                    "tools",
                                    buildJsonObject {
                                        put("listChanged", true)
                                    },
                                )
                            },
                        )
                        put(
                            "clientInfo",
                            buildJsonObject {
                                put("name", "TestClient")
                                put("version", "1.0.0")
                            },
                        )
                    },
                )
            }

        val response = sendRequest(request)
        return Json.parseToJsonElement(response).jsonObject
    }

    fun sendInitialized() {
        val notification =
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("method", "notifications/initialized")
                put("params", buildJsonObject {})
            }

        sendNotification(notification)
    }

    fun listTools(): JsonObject {
        val request =
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", ++requestId)
                put("method", "tools/list")
                put("params", buildJsonObject {})
            }

        val response = sendRequest(request)
        return Json.parseToJsonElement(response).jsonObject
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
                put(
                    "params",
                    buildJsonObject {
                        put("name", toolName)
                        put("arguments", arguments)
                    },
                )
            }

        val response = sendRequest(request)
        return Json.parseToJsonElement(response).jsonObject
    }

    private fun sendRequest(request: JsonObject): String {
        val requestStr = request.toString()
        writer.write(requestStr)
        writer.newLine()
        writer.flush()

        return reader.readLine() ?: throw RuntimeException("No response from MCP server")
    }

    private fun sendNotification(notification: JsonObject) {
        val notificationStr = notification.toString()
        writer.write(notificationStr)
        writer.newLine()
        writer.flush()
    }
}
