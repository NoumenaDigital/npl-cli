package com.noumenadigital.npl.cli.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.noumenadigital.npl.cli.model.AuditEntry
import com.noumenadigital.npl.cli.model.AuditResponse
import com.noumenadigital.npl.cli.model.VerificationMethod
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.Signature
import java.util.*
import org.erdtman.jcs.JsonCanonicalizer

class AuditVerificationService(
    private val didResolver: DidResolver,
    private val enableReplay: Boolean = true,
) {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun verify(
        auditSource: String,
        sourcesPath: String? = null,
        failFast: Boolean = false,
    ): VerificationResult {
        val auditResponse = loadAuditResponse(auditSource)
        return verify(auditResponse, sourcesPath, failFast)
    }

    fun verify(
        auditResponse: AuditResponse,
        sourcesPath: String? = null,
        failFast: Boolean = false,
    ): VerificationResult {
        val errors = mutableListOf<VerificationError>()

        // Step Ax: Parse & structure validation
        val structureErrors = validateStructure(auditResponse)
        errors.addAll(structureErrors)
        if (failFast && errors.isNotEmpty()) {
            return VerificationResult(false, errors)
        }

        // Hash-chain completeness
        val hashChainErrors = verifyHashChain(auditResponse.auditLog)
        errors.addAll(hashChainErrors)
        if (failFast && errors.isNotEmpty()) {
            return VerificationResult(false, errors)
        }

        // State hash completeness check
        val stateHashErrors = verifyStateHash(auditResponse)
        errors.addAll(stateHashErrors)
        if (failFast && errors.isNotEmpty()) {
            return VerificationResult(false, errors)
        }

        // DID resolution + signature verification
        val signatureErrors = verifySignatures(auditResponse.auditLog, failFast)
        errors.addAll(signatureErrors)
        if (failFast && errors.isNotEmpty()) {
            return VerificationResult(false, errors)
        }

        // Replay verification (if enabled and sources provided)
        if (enableReplay && sourcesPath != null) {
            val replayErrors = verifyReplay(auditResponse, sourcesPath)
            errors.addAll(replayErrors)
        } else if (enableReplay) {
            errors.add(
                VerificationError(
                    step = "Replay",
                    message = "Replay verification requested but no sources provided"
                )
            )
        }

        return VerificationResult(errors.isEmpty(), errors)
    }

    private fun loadAuditResponse(source: String): AuditResponse {
        val content = if (source.startsWith("http://") || source.startsWith("https://")) {
            URL(source).readText()
        } else {
            File(source).readText()
        }
        return objectMapper.readValue(content)
    }

    private fun validateStructure(auditResponse: AuditResponse): List<VerificationError> {
        val errors = mutableListOf<VerificationError>()

        if (auditResponse.auditLog.isEmpty()) {
            errors.add(VerificationError("Structure", "audit_log is empty"))
        }

        auditResponse.auditLog.forEachIndexed { index, entry ->
            if (entry.id.isBlank()) {
                errors.add(VerificationError("Structure", "Entry $index: id is blank"))
            }
            if (entry.timestamp.isBlank()) {
                errors.add(VerificationError("Structure", "Entry $index: timestamp is blank"))
            }
            if (entry.action.name.isBlank()) {
                errors.add(VerificationError("Structure", "Entry $index: action.name is blank"))
            }
            if (entry.stateHash.isBlank()) {
                errors.add(VerificationError("Structure", "Entry $index: stateHash is blank"))
            }
            if (entry.proof.verificationMethod.isBlank()) {
                errors.add(VerificationError("Structure", "Entry $index: proof.verificationMethod is blank"))
            }
            if (entry.proof.jws.isBlank()) {
                errors.add(VerificationError("Structure", "Entry $index: proof.jws is blank"))
            }
        }

        return errors
    }

    private fun verifyHashChain(auditLog: List<AuditEntry>): List<VerificationError> {
        val errors = mutableListOf<VerificationError>()

        auditLog.forEachIndexed { index, entry ->
            if (index == 0) {
                if (entry.previousHash != null) {
                    errors.add(
                        VerificationError(
                            "HashChain",
                            "Entry 0: previousHash should be null, got ${entry.previousHash}"
                        )
                    )
                }
            } else {
                val previousEntry = auditLog[index - 1]
                val previousUnsignedEntry = createUnsignedEntry(previousEntry)
                val previousHashBytes = computeHash(previousUnsignedEntry)
                val expectedPreviousHash = "sha256:" + bytesToHex(previousHashBytes)

                if (entry.previousHash != expectedPreviousHash) {
                    errors.add(
                        VerificationError(
                            "HashChain",
                            "Entry $index: previousHash mismatch. Expected $expectedPreviousHash, got ${entry.previousHash}"
                        )
                    )
                }
            }
        }

        return errors
    }

    private fun verifyStateHash(auditResponse: AuditResponse): List<VerificationError> {
        val errors = mutableListOf<VerificationError>()

        if (auditResponse.auditLog.isEmpty()) {
            return errors
        }

        val lastEntry = auditResponse.auditLog.last()
        val stateHashBytes = computeHash(auditResponse.state)
        val computedStateHash = "sha256:" + bytesToHex(stateHashBytes)

        if (lastEntry.stateHash != computedStateHash) {
            errors.add(
                VerificationError(
                    "StateHash",
                    "Last entry stateHash mismatch. Expected $computedStateHash, got ${lastEntry.stateHash}"
                )
            )
        }

        return errors
    }

    private fun verifySignatures(auditLog: List<AuditEntry>, failFast: Boolean): List<VerificationError> {
        val errors = mutableListOf<VerificationError>()

        auditLog.forEachIndexed { index, entry ->
            try {
                val verificationMethod = resolveVerificationMethod(entry.proof.verificationMethod)
                val publicKey = extractPublicKey(verificationMethod)

                val unsignedEntry = createUnsignedEntry(entry)
                val entryHashBytes = computeHash(unsignedEntry)

                val jwsParts = entry.proof.jws.split(".")
                if (jwsParts.size != 3) {
                    errors.add(
                        VerificationError(
                            "Signature",
                            "Entry $index: Invalid JWS format, expected 3 parts separated by '.'"
                        )
                    )
                    if (failFast) return errors
                    return@forEachIndexed
                }

                val (header, payload, signatureB64) = jwsParts

                val payloadBytes = base64UrlDecode(payload)

                if (!payloadBytes.contentEquals(entryHashBytes)) {
                    errors.add(
                        VerificationError(
                            "Signature",
                            "Entry $index: JWS payload does not match entry hash"
                        )
                    )
                    if (failFast) return errors
                    return@forEachIndexed
                }

                val signatureBytes = base64UrlDecode(signatureB64)
                val signingInput = "$header.$payload".toByteArray(StandardCharsets.US_ASCII)

                val signature = Signature.getInstance("Ed25519")
                signature.initVerify(publicKey)
                signature.update(signingInput)
                println("  Match: ${payloadBytes.contentEquals(entryHashBytes)}")

                val isValid = signature.verify(signatureBytes)
                println("  Signature valid: $isValid")

                if (!isValid) {
                    errors.add(
                        VerificationError(
                            "Signature",
                            "Entry $index: Signature verification failed"
                        )
                    )
                    if (failFast) return errors
                }
            } catch (e: Exception) {
                errors.add(
                    VerificationError(
                        "Signature",
                        "Entry $index: ${e.message}"
                    )
                )
                if (failFast) return errors
            }
        }

        return errors
    }

    private fun verifyReplay(auditResponse: AuditResponse, sourcesPath: String): List<VerificationError> {
        val errors = mutableListOf<VerificationError>()

        try {
            // Configure replay
            val config = ReplayConfig(
                sourcesPath = sourcesPath,
                baseUrl = System.getenv("NPL_BASE_URL") ?: "http://localhost:12000",
                dockerComposeCmd = System.getenv("NPL_DOCKER_COMPOSE_CMD") ?: "docker compose up -d --wait",
                deployCmd = System.getenv("NPL_DEPLOY_CMD") ?: "npl deploy",
                skipDocker = System.getenv("NPL_SKIP_DOCKER")?.toBoolean() ?: false,
                cleanup = System.getenv("NPL_CLEANUP")?.toBoolean() ?: false,
                verbose = true,
            )

            val replayRunner = ReplayRunner(config)
            val result = replayRunner.runReplay(auditResponse)

            // Convert replay errors to verification errors
            result.errors.forEach { replayError ->
                errors.add(
                    VerificationError(
                        "Replay",
                        "Entry ${replayError.entryIndex}: ${replayError.message}"
                    )
                )
            }

        } catch (e: Exception) {
            errors.add(
                VerificationError(
                    "Replay",
                    "Replay verification failed: ${e.message}"
                )
            )
        }

        return errors
    }

    private fun createUnsignedEntry(entry: AuditEntry): Map<String, Any?> {
        val actionMap = mutableMapOf(
            "name" to entry.action.name,
            "parameters" to entry.action.parameters
        )

        if (entry.action.type != null) {
            actionMap["type"] = entry.action.type
        }

        return mapOf(
            "id" to entry.id,
            "timestamp" to entry.timestamp,
            "action" to actionMap,
            "notificationHashes" to entry.notificationHashes,
            "stateHash" to entry.stateHash,
            "previousHash" to entry.previousHash
        )
    }

    private fun computeHash(data: Any): ByteArray {
        val json = objectMapper.writeValueAsString(data)
        val canonicalJson = JsonCanonicalizer(json).encodedString
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(canonicalJson.toByteArray(StandardCharsets.UTF_8))
    }

    private fun resolveVerificationMethod(verificationMethodUri: String): VerificationMethod {
        val didUri = if (verificationMethodUri.contains("#")) {
            verificationMethodUri.substringBefore("#")
        } else {
            throw DidResolutionException("Invalid verification method URI: $verificationMethodUri")
        }

        val didDoc = didResolver.resolve(didUri)

        return didDoc.verificationMethod.find { it.id == verificationMethodUri }
            ?: throw DidResolutionException("Verification method $verificationMethodUri not found in DID document")
    }

    private fun extractPublicKey(verificationMethod: VerificationMethod): java.security.PublicKey {
        if (verificationMethod.type != "Ed25519VerificationKey2020" &&
            verificationMethod.type != "JsonWebKey2020") {
            throw IllegalArgumentException("Unsupported verification method type: ${verificationMethod.type}")
        }

        val jwk = verificationMethod.publicKeyJwk
            ?: throw IllegalArgumentException("publicKeyJwk not found in verification method")

        if (jwk.kty != "OKP" || jwk.crv != "Ed25519") {
            throw IllegalArgumentException("Only Ed25519 keys are supported, got kty=${jwk.kty}, crv=${jwk.crv}")
        }

        val publicKeyBytes = base64UrlDecode(jwk.x)

        val keySpec = java.security.spec.X509EncodedKeySpec(
            // Ed25519 public key in X.509 format
            byteArrayOf(
                0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
            ) + publicKeyBytes
        )

        val keyFactory = java.security.KeyFactory.getInstance("Ed25519")
        return keyFactory.generatePublic(keySpec)
    }

    private fun base64UrlDecode(encoded: String): ByteArray {
        val base64 = encoded.replace('-', '+').replace('_', '/')
        val padding = when (base64.length % 4) {
            2 -> "=="
            3 -> "="
            else -> ""
        }
        return Base64.getDecoder().decode(base64 + padding)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

data class VerificationResult(
    val success: Boolean,
    val errors: List<VerificationError>,
)

data class VerificationError(
    val step: String,
    val message: String,
)

