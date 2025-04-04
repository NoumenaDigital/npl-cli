package com.noumenadigital.npl.cli.service

import com.noumenadigital.npl.cli.commands.NplCliCommandsEnum
import com.noumenadigital.npl.cli.exception.CommandParsingException
import com.noumenadigital.npl.cli.service.impl.NplCommandsParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class NplCommandsParserTest : FunSpec({

    test("should parse a list of valid command strings into Command objects") {
        val parser = NplCommandsParser()
        val input = listOf("help")

        val result = parser.parse(input)

        result shouldHaveSize 1
        result[0].nplCliCommandsEnum shouldBe NplCliCommandsEnum.commandFromString("help")
    }

    test("should throw CommandParsingException when input list is empty") {
        val parser = NplCommandsParser()
        val input = emptyList<String>()

        shouldThrow<CommandParsingException> {
            parser.parse(input)
        }
    }
})
