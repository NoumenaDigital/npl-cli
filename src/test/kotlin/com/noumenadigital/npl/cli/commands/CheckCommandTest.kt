package com.noumenadigital.npl.cli.commands

import io.kotest.core.spec.style.FunSpec

class CheckCommandTest :
    FunSpec({

        context("success") {
            test("single file") {}
            test("multiple files") {}
            test("multiple packages") {}
            test("both main and test sources") {}
            test("failure in test sources should not lead to check failure") {}
        }

        context("failure") {
            test("single file") {}
            test("multiple files") {}
            test("multiple packages") {}
        }

        context("warnings") {
            test("warnings during compilation")
            test("no NPL sources")
            test("multiple warnings")
        }
    })
