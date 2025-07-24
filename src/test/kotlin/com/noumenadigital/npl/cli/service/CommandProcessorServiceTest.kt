package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.CommandProcessor
import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.registry.CommandsParser
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringWriter

class CommandProcessorServiceTest :
    FunSpec({
        data class TestContext(
            val commandsParser: CommandsParser = CommandsParser,
            val writer: ColorWriter = ColorWriter(StringWriter(), false),
            val executor: CommandProcessor = CommandProcessor(commandsParser),
        )

        fun withTestContext(test: TestContext.() -> Unit) {
            TestContext().apply(test)
        }
        test("should execute parsed command and write output") {
            withTestContext {
                val exitCode = executor.process(listOf("version"), writer)
                val expectedOutput =
                    "Could not determine NPL CLI version from POM file, assuming `development`.\n" +
                        "NPL CLI development\n"
                writer.toString() shouldBe expectedOutput
                exitCode shouldBe ExitCode.SUCCESS
            }
        }
        test("should execute 'help' command if no input provided") {
            withTestContext {
                val exitCode = executor.process(emptyList(), writer)
                val expectedOutput =
                    """
                    version    Display the current version of the NPL CLI
                    help       Display the description for npl-cli commands
                    init       Initializes a new project
                                 --name <name>  Name of the project. A directory of this name will be created in the current directory
                                 --bare  Installs an empty project structure (defaults to false)
                                 --templateUrl <templateUrl>  URL of a repository containing a ZIP archive of the project template. Overrides the default template
                    check      Validate the correctness of NPL sources
                                 --sourceDir <directory>  Directory containing NPL source files (defaults to current directory)
                    test       Run the NPL tests
                                 --sourceDir <directory>  Source directory containing NPL tests to run. Must be a parent directory of all required sources (both production and test). (defaults to current directory)
                                 --coverage  Report test coverage details (printed to console as well as coverage.xml)
                                 --outputDir <output directory>  Directory to place generated output files (optional) (defaults to current directory)
                    openapi    Generate the openapi specifications of NPL api
                                 --sourceDir <directory>  Directory containing NPL source files (defaults to current directory)
                                 --rules <rules descriptor path>  Path to the party automation rules descriptor. If omitted, generated document will not reflect the current system
                                 --outputDir <output directory>  Directory to place generated output files (optional) (defaults to current directory)
                    puml       Generate a puml diagram from source in the given directory
                                 --sourceDir <directory>  Directory containing NPL source files (defaults to current directory)
                                 --outputDir <output directory>  Directory to place generated output files (optional) (defaults to current directory)
                    deploy     Deploy NPL sources to a Noumena Engine instance
                                 --target <name>  Named target from deploy.yml to deploy to. Required unless defaultTarget is set in config.
                                 --sourceDir <directory> (required)  Directory containing NPL sources
                                 --configFile <path>  Path to deploy.yml configuration file
                                 --clear  Clear application contents before deployment
                    cloud      Subset of commands for NOUMENA Cloud; for the full list of sub-commands, run `npl cloud help`
                    mcp        Start an MCP server exposing NPL CLI functionality over stdio
                    """.trimIndent()

                writer.toString().trimIndent() shouldBe expectedOutput
                exitCode shouldBe ExitCode.SUCCESS
            }
        }

        test("should print error message if command cannot be found") {
            withTestContext {
                val exitCode = executor.process(listOf("nonExistingCommand"), writer)
                val expectedOutput = "Command not supported: 'nonexistingcommand'.\n"
                writer.toString() shouldBe expectedOutput
                exitCode shouldBe ExitCode.CONFIG_ERROR
            }
        }

        test("should suggest another command if there is match with existing command") {
            withTestContext {
                val exitCode = executor.process(listOf("vers"), writer)
                val expectedOutput = "Command not supported: 'vers'. Did you mean 'version'?\n"
                writer.toString() shouldBe expectedOutput
                exitCode shouldBe ExitCode.CONFIG_ERROR
            }
        }
    })
