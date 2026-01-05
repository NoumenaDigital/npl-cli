package com.noumenadigital.npl.cli.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class AuditResponse(
    @JsonProperty("audit_log")
    val auditLog: List<AuditEntry>,
    val state: Map<String, Any>,
)

data class AuditEntry(
    val id: String,
    val timestamp: String,
    val action: ActionData,
    val notificationHashes: List<String>,
    val stateHash: String,
    val previousHash: String?,
    val proof: ProofData,
)

data class ActionData(
    val type: String? = null,
    val name: String,
    val parameters: Map<String, Any>? = null,
)

data class ProofData(
    val type: String,
    val created: String,
    val verificationMethod: String,
    val proofPurpose: String,
    val jws: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DidDocument(
    val id: String,
    val verificationMethod: List<VerificationMethod>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VerificationMethod(
    val id: String,
    val type: String,
    val controller: String,
    val publicKeyJwk: PublicKeyJwk?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PublicKeyJwk(
    val kty: String,
    val crv: String,
    val x: String,
)

