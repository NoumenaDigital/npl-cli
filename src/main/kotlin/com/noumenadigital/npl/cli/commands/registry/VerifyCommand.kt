package com.noumenadigital.npl.cli.commands.registry

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.noumenadigital.npl.cli.ExitCode
import com.noumenadigital.npl.cli.commands.NamedParameter
import com.noumenadigital.npl.cli.config.YamlConfig
import com.noumenadigital.npl.cli.service.AuditVerificationService
import com.noumenadigital.npl.cli.service.ColorWriter
import com.noumenadigital.npl.cli.service.DidResolver
import com.noumenadigital.npl.cli.service.VerificationResult
import java.io.File

object VerifyCommandDescriptor : CommandDescriptor {
    override val commandName: String = "verify"
    override val description: String = "Verify NOUMENA verifiable protocol audit trails"
    override val supportsMcp: Boolean = true
    override val usageInstruction: String =
        """
        verify --audit <file-or-url> --sources <path> [options]

        Verifies a NOUMENA verifiable protocol audit trail according to the NOUMENA Network whitepaper.
        
        The verification process includes:
          - Structure validation
          - Hash-chain completeness (previousHash verification)
          - State hash verification
          - DID resolution and signature verification
          - Replay verification (if --replay is enabled and sources are provided)

        Arguments:
          --audit <file-or-url>      Path to audit JSON file or HTTP(S) URL (required)
          --sources <path>           Path to local NPL sources directory or zip (required)

        Options:
          --did-scheme <http|https>  Scheme for DID resolution (default: https)
          --did-host-override <host> Override host for DID resolution (e.g., localhost:8080 for testing)
          --fail-fast                Stop verification on first error (default: false)
          --json                     Output results in JSON format (default: false)
          --no-replay                Disable replay verification (default: replay is enabled)

        Exit codes:
          0 - Verification successful
          1 - Verification failed
        """.trimIndent()

    override val parameters: List<NamedParameter> =
        listOf(
            NamedParameter(
                name = "audit",
                description = "Path to audit JSON file or HTTP(S) URL",
                isRequired = true,
                valuePlaceholder = "<file-or-url>",
                takesPath = true,
                isRequiredForMcp = true,
                configFilePath = YamlConfig.Verify.audit,
            ),
            NamedParameter(
                name = "sources",
                description = "Path to local NPL sources directory or zip",
                isRequired = true,
                valuePlaceholder = "<path>",
                takesPath = true,
                isRequiredForMcp = true,
                configFilePath = YamlConfig.Verify.sources,
            ),
            NamedParameter(
                name = "did-scheme",
                description = "Scheme for DID resolution (http or https)",
                isRequired = false,
                valuePlaceholder = "<http|https>",
                configFilePath = YamlConfig.Verify.didScheme,
            ),
            NamedParameter(
                name = "did-host-override",
                description = "Override host for DID resolution (e.g., localhost:8080)",
                isRequired = false,
                valuePlaceholder = "<host:port>",
                configFilePath = YamlConfig.Verify.didHostOverride,
            ),
            NamedParameter(
                name = "fail-fast",
                description = "Stop verification on first error",
                isRequired = false,
                configFilePath = YamlConfig.Verify.failFast,
            ),
            NamedParameter(
                name = "json",
                description = "Output results in JSON format",
                isRequired = false,
                configFilePath = YamlConfig.Verify.json,
            ),
            NamedParameter(
                name = "no-replay",
                description = "Disable replay verification",
                isRequired = false,
                configFilePath = YamlConfig.Verify.noReplay,
            ),
        )

    override fun createCommandExecutorInstance(parsedArguments: Map<String, Any>): CommandExecutor {
        val audit = parsedArguments["audit"] as String
        val sources = parsedArguments["sources"] as String
        val didScheme = parsedArguments["did-scheme"] as? String ?: "https"
        val didHostOverride = parsedArguments["did-host-override"] as? String
        val failFast = parsedArguments["fail-fast"] != null
        val jsonOutput = parsedArguments["json"] != null
        val noReplay = parsedArguments["no-replay"] != null

        return VerifyCommand(
            audit = audit,
            sources = sources,
            didScheme = didScheme,
            didHostOverride = didHostOverride,
            failFast = failFast,
            jsonOutput = jsonOutput,
            enableReplay = !noReplay,
        )
    }
}

class VerifyCommand(
    private val audit: String,
    private val sources: String,
    private val didScheme: String,
    private val didHostOverride: String?,
    private val failFast: Boolean,
    private val jsonOutput: Boolean,
    private val enableReplay: Boolean,
) : CommandExecutor {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    override fun execute(output: ColorWriter): ExitCode {
        // Validate inputs
        if (didScheme != "http" && didScheme != "https") {
            output.error("Invalid did-scheme: $didScheme. Must be 'http' or 'https'")
            return ExitCode.GENERAL_ERROR
        }

        val auditFile = File(audit)
        if (!audit.startsWith("http://") && !audit.startsWith("https://") && !auditFile.exists()) {
            output.error("Audit file does not exist: $audit")
            return ExitCode.GENERAL_ERROR
        }

        val sourcesFile = File(sources)
        if (!sourcesFile.exists()) {
            output.error("Sources path does not exist: $sources")
            return ExitCode.GENERAL_ERROR
        }

        if (!jsonOutput) {
            output.info("Verifying audit trail from: $audit")
            output.info("Using sources from: $sources")
            output.info("")
        }

        try {
            val didResolver = DidResolver(didScheme, didHostOverride)
            val verificationService = AuditVerificationService(didResolver, enableReplay)

            val result = verificationService.verify(
                auditSource = audit,
                sourcesPath = sources,
                failFast = failFast,
            )

            if (jsonOutput) {
                outputJson(result, output)
            } else {
                outputHuman(result, output)
            }

            return if (result.success) ExitCode.SUCCESS else ExitCode.DATA_ERROR
        } catch (e: Exception) {
            if (jsonOutput) {
                val errorResult = mapOf(
                    "success" to false,
                    "error" to e.message,
                    "errors" to emptyList<Any>()
                )
                output.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorResult))
            } else {
                output.error("Verification failed: ${e.message}")
                if (e.cause != null) {
                    output.error("Caused by: ${e.cause?.message}")
                }
            }
            return ExitCode.GENERAL_ERROR
        }
    }

    private fun outputJson(result: VerificationResult, output: ColorWriter) {
        val jsonResult = mapOf(
            "success" to result.success,
            "errors" to result.errors.map {
                mapOf(
                    "step" to it.step,
                    "message" to it.message
                )
            }
        )
        output.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResult))
    }

    private fun outputHuman(result: VerificationResult, output: ColorWriter) {
        if (result.success) {
            output.success("✓ Verification successful!")
            output.info("")
            output.info("All checks passed:")
            output.info("  ✓ Structure validation")
            output.info("  ✓ Hash-chain completeness")
            output.info("  ✓ State hash verification")
            output.info("  ✓ Signature verification")
            if (enableReplay) {
                output.info("  ✓ Replay verification")
            }
        } else {
            output.error("✗ Verification failed!")
            output.info("")
            output.error("Errors found:")
            result.errors.forEach { error ->
                output.error("  [${error.step}] ${error.message}")
            }
        }
    }
}

