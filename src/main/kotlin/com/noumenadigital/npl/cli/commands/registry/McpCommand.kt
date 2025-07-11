package com.noumenadigital.npl.cli.commands.registry

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.ConsoleAppender
import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.CloudCommands
import com.noumenadigital.npl.cli.commands.CloudDeployCommands
import com.noumenadigital.npl.cli.commands.Commands
import com.noumenadigital.npl.cli.service.ColorWriter
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter

object McpCommand : CommandExecutor {
    private val logger = LoggerFactory.getLogger(McpCommand::class.java)
    override val commandName: String = "mcp"
    override val description: String = "Start an MCP server exposing NPL CLI functionality over stdio"
    override val supportsMcp: Boolean = false

    override fun execute(output: ColorWriter): ExitCode {
        // Only for MCP: reconfigure Logback to log to System.err
        try {
            val loggerFactory = LoggerFactory.getILoggerFactory()
            if (loggerFactory is LoggerContext) {
                val context = loggerFactory
                val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
                val appenders = rootLogger.iteratorForAppenders()
                while (appenders.hasNext()) {
                    val appender = appenders.next()
                    if (appender is ConsoleAppender<*>) {
                        appender.setTarget("System.err")
                        appender.start()
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore if logback is not present
        }

        // Redirect stdout to stderr to avoid interfering with JSON-RPC communication
        val originalOut = System.out
        val originalErr = System.err

        // Create a PrintStream that writes to stderr instead of stdout
        val stderrStream = PrintStream(originalErr, true)
        System.setOut(stderrStream)

        // Create a separate stdout stream for MCP transport
        val mcpStdout = PrintStream(originalOut, true)

        try {
            // Create server before runBlocking to ensure logging goes to stderr
            val server = createMcpServer()

            runBlocking {
                val transport =
                    StdioServerTransport(
                        System.`in`.asInput(),
                        mcpStdout.asSink().buffered(),
                    )

                server.connect(transport)

                val done = Job()
                server.onClose {
                    done.complete()
                }
                done.join()
            }
        } catch (e: Exception) {
            output.error("Failed to start MCP server: ${e.message}")
            e.printStackTrace()
            return ExitCode.GENERAL_ERROR
        } finally {
            // Restore original stdout
            System.setOut(originalOut)
        }

        return ExitCode.SUCCESS
    }

    private fun createMcpServer(): Server {
        val server =
            Server(
                serverInfo =
                    Implementation(
                        name = "npl-cli-mcp",
                        version = VersionCommand.getVersionFromPom() ?: "development",
                    ),
                options =
                    ServerOptions(
                        capabilities =
                            ServerCapabilities(
                                tools = ServerCapabilities.Tools(listChanged = true),
                                resources =
                                    ServerCapabilities.Resources(
                                        subscribe = false,
                                        listChanged = false,
                                    ),
                            ),
                    ),
            )

        addAllTools(server)
        return server
    }

    private fun addAllTools(server: Server) {
        (Commands.entries + CloudCommands.entries + CloudDeployCommands.entries).forEach { command ->
            val executor = command.getBaseExecutor()
            if (executor.supportsMcp) {
                val toolName = command.commandName.replace(" ", "_")
                addTool(server, toolName, executor)
            }
        }
    }

    private fun addTool(
        server: Server,
        toolName: String,
        executor: CommandExecutor,
    ) {
        val schema = executor.toMcpToolInput()

        server.addTool(
            name = toolName,
            description = executor.description,
            inputSchema = schema,
        ) { req ->
            logger.info("MCP tool called: $toolName with args: ${req.arguments}")
            val args = req.arguments
            val outputBuffer = StringWriter()
            val colorWriter = ColorWriter(PrintWriter(outputBuffer))

            try {
                val cmdArgs = buildCommandArgs(args, executor)
                logger.info("Built command args: $cmdArgs")
                val commandInstance = executor.createInstance(cmdArgs)
                val exitCode = commandInstance.execute(colorWriter)

                logger.info("Tool execution completed with exit code: $exitCode")
                CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                text =
                                    buildJsonObject {
                                        put("output", outputBuffer.toString())
                                        put("success", exitCode == ExitCode.SUCCESS)
                                    }.toString(),
                            ),
                        ),
                )
            } catch (e: Exception) {
                logger.error("Tool execution failed with exception: ${e.message}", e)
                CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                text =
                                    buildJsonObject {
                                        put("error", "Error running ${executor.commandName}: ${e.message}")
                                        put("success", false)
                                    }.toString(),
                            ),
                        ),
                )
            }
        }
    }

    private fun buildCommandArgs(
        args: Map<String, JsonElement>,
        executor: CommandExecutor,
    ): List<String> {
        val cmdArgs = mutableListOf<String>()

        executor.parameters.filter { !it.isHidden }.forEach { parameter ->
            val jsonElement = args[parameter.name]

            if (jsonElement != null) {
                if (parameter.takesValue) {
                    val argValue = jsonElement.jsonPrimitive.contentOrNull
                    if (argValue != null) {
                        cmdArgs.add(parameter.cliName)
                        cmdArgs.add(argValue)
                    } else if (parameter.isRequired) {
                        throw IllegalArgumentException("Required parameter `${parameter.name}` is missing")
                    }
                } else {
                    val isFlagSet = jsonElement.jsonPrimitive.booleanOrNull ?: false
                    if (isFlagSet) {
                        cmdArgs.add(parameter.cliName)
                    }
                }
            } else if (parameter.isRequired) {
                throw IllegalArgumentException("Required parameter `${parameter.name}` is missing")
            }
        }

        return cmdArgs
    }
}
