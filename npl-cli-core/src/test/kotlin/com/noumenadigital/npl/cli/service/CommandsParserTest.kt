package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.Commands
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CommandsParserTest :
    FunSpec({

        test("should parse a list of valid command strings into Command objects") {
            val parser = CommandsParser
            val input = listOf("help")

            val result = parser.parse(input)

            result shouldBe Commands.commandFromString("help")
        }

        test("should execute help command input list is empty") {
            val parser = CommandsParser
            val input = emptyList<String>()

            val result = parser.parse(input)
            result shouldBe Commands.commandFromString("help")
        }
    })
