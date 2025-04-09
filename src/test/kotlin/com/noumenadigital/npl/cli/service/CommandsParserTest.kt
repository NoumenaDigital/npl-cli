package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.CommandsRegistry
import com.noumenadigital.npl.cli.exception.CommandParsingException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CommandsParserTest :
    FunSpec({

        test("should parse a list of valid command strings into Command objects") {
            val parser = CommandsParser
            val input = listOf("help")

            val result = parser.parse(input)

            result shouldBe CommandsRegistry.commandFromString("help")
        }

        test("should throw CommandParsingException when input list is empty") {
            val parser = CommandsParser
            val input = emptyList<String>()

            shouldThrow<CommandParsingException> {
                parser.parse(input)
            }
        }
    })
