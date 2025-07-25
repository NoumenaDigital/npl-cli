package com.noumenadigital.npl.cli

import com.noumenadigital.npl.cli.TestUtils.normalize
import com.noumenadigital.npl.cli.TestUtils.runCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BinaryCommandsIT :
    FunSpec({
        test("version command should return correct version based on test mode") {
            val pomFile = File("pom.xml")
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(pomFile)
            doc.documentElement.normalize()

            val pomVersion =
                doc.documentElement
                    .getElementsByTagName("version")
                    .item(0)
                    .textContent

            runCommand(listOf("version")) {
                process.waitFor()

                val testMode = System.getenv().getOrDefault("TEST_MODE", "direct")

                val expectedVersion = if (testMode == "direct") "development" else pomVersion
                val expectedOutput =
                    "${if (testMode == "direct") "Could not determine NPL CLI version from POM file, assuming `development`.\n" else ""}NPL CLI $expectedVersion"
                        .normalize()

                output.normalize() shouldBe expectedOutput
                process.exitValue() shouldBe ExitCode.SUCCESS.code
            }
        }

        test("help command should return list of commands") {
            runCommand(listOf("help")) {
                process.waitFor()

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
                    """.normalize()

                output.normalize() shouldBe expectedOutput
                process.exitValue() shouldBe ExitCode.SUCCESS.code
            }
        }

        test("unknown command error") {
            runCommand(listOf("notexisitingcommand")) {
                process.waitFor()

                val expectedOutput =
                    """
                    Command not supported: 'notexisitingcommand'.
                    """.normalize()

                output.normalize() shouldBeEqual expectedOutput
                process.exitValue() shouldBe ExitCode.CONFIG_ERROR.code
            }
        }

        test("correct command should be suggested") {
            runCommand(listOf("versTion")) {
                process.waitFor()

                val expectedOutput =
                    """
                    Command not supported: 'verstion'. Did you mean 'version'?
                    """.normalize()

                output.normalize() shouldBeEqual expectedOutput
                process.exitValue() shouldBe ExitCode.CONFIG_ERROR.code
            }
        }
    })
