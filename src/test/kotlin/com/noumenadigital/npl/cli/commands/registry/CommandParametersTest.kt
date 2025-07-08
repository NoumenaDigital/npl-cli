package com.noumenadigital.npl.cli.commands.registry

import com.noumenadigital.npl.cli.commands.CommandArgumentParser
import com.noumenadigital.npl.cli.commands.Commands
import com.noumenadigital.npl.cli.commands.CommandsParser
import com.noumenadigital.npl.cli.commands.NamedParameter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class CommandParametersTest :
    FunSpec({

        context("CommandParameter validation") {
            test("NamedParameter should not allow '--' prefix") {
                shouldThrow<IllegalArgumentException> {
                    NamedParameter(name = "--param", description = "desc")
                }
                NamedParameter(name = "param", description = "desc") // Should not throw
            }

            test("NamedParameter takesValue should be true only if valuePlaceholder is not null") {
                NamedParameter(name = "param", description = "desc").takesValue.shouldBeFalse()
                NamedParameter(name = "param", description = "desc", valuePlaceholder = "<val>").takesValue.shouldBeTrue()
            }
        }

        context("CommandArgumentParser") {
            val parser = CommandArgumentParser
            val namedParamWithValue =
                NamedParameter(name = "file", description = "File path", valuePlaceholder = "<path>")
            val namedFlag = NamedParameter(name = "verbose", description = "Enable verbose mode")

            val parameters = listOf(namedParamWithValue, namedFlag)

            test("should parse named parameter with value") {
                val args = listOf("--file", "my/path.txt", "other")
                val result = parser.parse(args, parameters)

                result.getValue("file") shouldBe "my/path.txt"
                result.hasFlag("verbose").shouldBeFalse()
                result.unexpectedArgs shouldBe listOf("other")
            }

            test("should parse named flag") {
                val args = listOf("--verbose", "other", "--file", "path")
                val result = parser.parse(args, parameters)

                result.hasFlag("verbose").shouldBeTrue()
                result.getValue("file") shouldBe "path"
                result.unexpectedArgs shouldBe listOf("other")
            }

            test("should parse multiple named parameters and flags") {
                val args = listOf("--verbose", "--file", "my/path.txt", "positionalArg")
                val result = parser.parse(args, parameters)

                result.hasFlag("verbose").shouldBeTrue()
                result.getValue("file") shouldBe "my/path.txt"
                result.unexpectedArgs shouldBe listOf("positionalArg")
            }

            test("should handle value appearing before its named parameter") {
                // The current simple parser expects "--param value" order
                val args = listOf("my/path.txt", "--file", "--verbose")
                val result = parser.parse(args, parameters)

                result.getValue("file").shouldBeNull() // Cannot parse value before param
                result.hasFlag("verbose").shouldBeTrue()
                result.unexpectedArgs shouldBe listOf("my/path.txt", "--file") // --file is unexpected because its value wasn't consumed
            }

            test("should treat value starting with '--' as a separate parameter") {
                val args = listOf("--file", "--another-param")
                val result = parser.parse(args, parameters)

                result.getValue("file").shouldBeNull() // Value starts with '--', not consumed
                result.hasFlag("verbose").shouldBeFalse()
                // Both become unexpected because --file expects a value but gets another param
                result.unexpectedArgs shouldBe listOf("--file", "--another-param")
            }

            test("should handle args list ending with a parameter expecting a value") {
                val args = listOf("start", "--file")
                val result = parser.parse(args, parameters)

                result.getValue("file").shouldBeNull()
                result.hasFlag("verbose").shouldBeFalse()
                result.unexpectedArgs shouldBe listOf("start", "--file")
            }

            test("should return empty map and all args as unexpected when no named parameters defined") {
                val args = listOf("--file", "path", "command", "--verbose")
                val result = parser.parse(args, emptyList()) // No parameters defined

                result.unexpectedArgs shouldBe args
                result.hasFlag("file").shouldBeFalse()
                result.getValue("file").shouldBeNull()
                result.hasFlag("verbose").shouldBeFalse()
            }

            test("should return empty parsed values and unexpected args for empty input") {
                val args = emptyList<String>()
                val result = parser.parse(args, parameters)

                result.unexpectedArgs.shouldBeEmpty()
                result.hasFlag("file").shouldBeFalse()
                result.getValue("file").shouldBeNull()
                result.hasFlag("verbose").shouldBeFalse()
            }
        }

        context("CommandsParser") {
            test("should parse a list of valid command strings into Command objects") {
                val parser = CommandsParser
                val input = listOf("help")

                val result = parser.parse(input)

                result.commandName shouldBe Commands.commandFromString("help", emptyList()).commandName
                result.description shouldBe Commands.commandFromString("help", emptyList()).description
            }

            test("should execute help command when input list is empty") {
                val parser = CommandsParser
                val input = emptyList<String>()

                val result = parser.parse(input)
                result.commandName shouldBe Commands.commandFromString("help", emptyList()).commandName
                result.description shouldBe Commands.commandFromString("help", emptyList()).description
            }
        }
    })
