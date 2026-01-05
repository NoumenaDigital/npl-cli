# NOUMENA NPL CLI: Verify Command Architecture

## Complete Technical Documentation

This document provides a comprehensive overview of how the `verify` command works in the NPL CLI, including all components, data flow, and implementation details.

---

## Table of Contents

1. [Overview](#overview)
2. [Command Entry Point](#command-entry-point)
3. [Core Components](#core-components)
4. [Verification Pipeline](#verification-pipeline)
5. [Replay Verification Deep Dive](#replay-verification-deep-dive)
6. [Data Models](#data-models)
7. [Security & Cryptography](#security--cryptography)
8. [Error Handling](#error-handling)

---

## Overview

The `verify` command validates NOUMENA verifiable protocol audit trails according to the NOUMENA Network whitepaper. It performs cryptographic verification, structural validation, and optional replay verification to ensure audit trail integrity.

### Key Features
- **Structure Validation**: Ensures audit trail has required fields
- **Hash-chain Verification**: Validates previousHash links between entries
- **State Hash Verification**: Confirms final state matches last entry's stateHash
- **Signature Verification**: Verifies Ed25519 signatures using DID resolution
- **Replay Verification**: Re-executes protocol actions to verify state transitions

---

## Command Entry Point

### VerifyCommand.kt

**Location**: `src/main/kotlin/com/noumenadigital/npl/cli/commands/registry/VerifyCommand.kt`

#### Command Parameters

```kotlin
--audit <file-or-url>          // Path to audit JSON or HTTP(S) URL (required)
--sources <path>               // Path to NPL sources directory (required)
--did-scheme <http|https>      // DID resolution scheme (default: https)
--did-host-override <host>     // Override DID resolution host (for testing)
--fail-fast                    // Stop on first error
--json                         // Output results in JSON format
--no-replay                    // Disable replay verification
```

#### Execution Flow

1. **Input Validation**
   - Validates `did-scheme` is `http` or `https`
   - Checks audit file/URL exists
   - Checks sources path exists

2. **Service Initialization**
   ```kotlin
   val didResolver = DidResolver(didScheme, didHostOverride)
   val verificationService = AuditVerificationService(didResolver, enableReplay)
   ```

3. **Verification Execution**
   ```kotlin
   val result = verificationService.verify(
       auditSource = audit,
       sourcesPath = sources,
       failFast = failFast
   )
   ```

4. **Output Formatting**
   - Human-readable format (default): Shows ✓/✗ for each verification step
   - JSON format: Machine-readable structured output

5. **Exit Code**
   - `0`: Verification successful
   - `1`: Verification failed (DATA_ERROR)
   - `2`: General error (e.g., invalid inputs)

---

## Core Components

### 1. AuditVerificationService

**Location**: `src/main/kotlin/com/noumenadigital/npl/cli/service/AuditVerificationService.kt`

**Responsibilities**: Orchestrates all verification steps

#### Main Verification Method

```kotlin
fun verify(
    auditResponse: AuditResponse,
    sourcesPath: String?,
    failFast: Boolean
): VerificationResult
```

#### Verification Steps (in order)

1. **Structure Validation** (`validateStructure`)
2. **Hash-chain Verification** (`verifyHashChain`)
3. **State Hash Verification** (`verifyStateHash`)
4. **Signature Verification** (`verifySignatures`)
5. **Replay Verification** (`verifyReplay`) - if enabled

---

### 2. DidResolver

**Location**: `src/main/kotlin/com/noumenadigital/npl/cli/service/DidResolver.kt`

**Responsibilities**: Resolves DID URIs to DID Documents containing public keys

#### Features

- **Caching**: Uses `ConcurrentHashMap` to cache resolved DIDs
- **did:web Support**: Only supports `did:web` method
- **Configurable Scheme**: Supports `http`/`https` for testing/production
- **Host Override**: Can override DID host for local testing

#### Resolution Algorithm

```
did:web:example.com
  → https://example.com/.well-known/did.json

did:web:example.com:users:alice
  → https://example.com/users/alice/did.json
```

#### Error Handling
- Throws `DidResolutionException` for:
  - Unsupported DID methods (only `did:web` supported)
  - HTTP errors during resolution
  - Network failures
  - Malformed DID documents

---

### 3. ReplayStateProjection

**Location**: `src/main/kotlin/com/noumenadigital/npl/cli/util/ReplayStateProjection.kt`

**Responsibilities**: Transforms REST API state into backend internal representation for hash comparison

#### Transformation Logic

```kotlin
fun fromRestState(rest: Map<String, Any?>): Map<String, Any?>
```

**Input**: REST state from protocol GET endpoint
```json
{
  "@id": "uuid",
  "@state": "unpaid",
  "@parties": {"issuer": {...}, "payee": {...}},
  "forAmount": 10
}
```

**Output**: Backend internal representation
```json
{
  "state": "unpaid",
  "slots": {
    "forAmount": 10,
    "issuer": {...},
    "payee": {...},
    "currentState": "unpaid"
  }
}
```

#### Key Features
- Extracts protocol fields (non-@ keys)
- Flattens `@parties` into slots
- Flattens `@observers` into slots (if present)
- Adds `currentState` to slots (matches backend's `frame.slots`)
- Preserves `state` at top level

---

## Verification Pipeline

### Step 1: Structure Validation

**Purpose**: Ensure audit trail has all required fields

**Checks**:
- `audit_log` is not empty
- Each entry has:
  - Non-blank `id`
  - Non-blank `timestamp`
  - Non-blank `action.name`
  - Non-blank `stateHash`
  - Non-blank `proof.verificationMethod`
  - Non-blank `proof.jws`

**Error Format**: `[Structure] Entry {index}: {field} is blank`

---

### Step 2: Hash-chain Verification

**Purpose**: Verify integrity of audit trail sequence

**Algorithm**:
```kotlin
for each entry[i] in audit_log:
    if i == 0:
        assert entry.previousHash == null
    else:
        previousEntry = audit_log[i-1]
        unsignedPrevious = removeProof(previousEntry)
        expectedHash = sha256(JCS(unsignedPrevious))
        assert entry.previousHash == expectedHash
```

**Hash Computation**:
1. Create unsigned entry (remove `proof` field)
2. Serialize to JSON
3. Apply JCS (JSON Canonicalization Scheme - RFC 8785)
4. Compute SHA-256 hash
5. Format as `sha256:{hex}`

**Error Format**: `[HashChain] Entry {index}: previousHash mismatch`

---

### Step 3: State Hash Verification

**Purpose**: Verify final state matches audit trail's last entry

**Algorithm**:
```kotlin
lastEntry = audit_log.last()
stateProjection = ReplayStateProjection.fromRestState(auditResponse.state)
computedHash = sha256(JCS(stateProjection))
assert lastEntry.stateHash == computedHash
```

**Key Points**:
- Only verifies the **final state** (last entry)
- Uses `ReplayStateProjection` to match backend's internal representation
- Does **not** verify intermediate state hashes (that's replay's job)

**Error Format**: `[StateHash] Last entry stateHash mismatch`

---

### Step 4: Signature Verification

**Purpose**: Verify cryptographic signatures using Ed25519 and DID resolution

#### Algorithm

For each entry:

1. **Resolve Verification Method**
   ```kotlin
   didUri = entry.proof.verificationMethod.substringBefore("#")
   didDoc = didResolver.resolve(didUri)
   verificationMethod = didDoc.verificationMethod.find { it.id == entry.proof.verificationMethod }
   ```

2. **Extract Public Key**
   - Verify `type` is `Ed25519VerificationKey2020` or `JsonWebKey2020`
   - Extract `publicKeyJwk` with `kty=OKP`, `crv=Ed25519`
   - Decode base64url-encoded `x` coordinate
   - Construct X.509 public key for Ed25519

3. **Verify JWS Signature**
   ```kotlin
   jws = entry.proof.jws  // format: {header}.{payload}.{signature}
   [header, payload, signature] = jws.split(".")
   
   // Verify payload matches entry hash
   payloadBytes = base64UrlDecode(payload)
   unsignedEntry = removeProof(entry)
   entryHash = sha256(JCS(unsignedEntry))
   assert payloadBytes == entryHash
   
   // Verify signature
   signingInput = "{header}.{payload}".toBytes()
   signatureBytes = base64UrlDecode(signature)
   assert Ed25519.verify(publicKey, signingInput, signatureBytes)
   ```

#### JWS Format

```
eyJhbGciOiJFZERTQSJ9.eyJoYXNoIjoiLi4uIn0.c2lnbmF0dXJl
^^^^^^^^^^^^^^^^^    ^^^^^^^^^^^^^^^^^^  ^^^^^^^^^^^^
     header                payload         signature
```

**Error Formats**:
- `[Signature] Entry {index}: Invalid JWS format`
- `[Signature] Entry {index}: JWS payload does not match entry hash`
- `[Signature] Entry {index}: Signature verification failed`
- `[Signature] Entry {index}: {exception message}`

---

### Step 5: Replay Verification (Optional)

**Purpose**: Verify state transitions by re-executing protocol actions

**See**: [Replay Verification Deep Dive](#replay-verification-deep-dive)

---

## Replay Verification Deep Dive

### ReplayRunner Architecture

**Location**: `src/main/kotlin/com/noumenadigital/npl/cli/service/ReplayRunner.kt`

### Configuration

```kotlin
data class ReplayConfig(
    val sourcesPath: String,           // Path to NPL sources
    val baseUrl: String,               // NPL runtime base URL
    val dockerComposeCmd: String,      // Docker command (not used)
    val deployCmd: String,             // Deploy command (not used)
    val skipDocker: Boolean,           // Skip Docker (always true)
    val cleanup: Boolean,              // Cleanup after replay
    val verbose: Boolean               // Verbose logging
)
```

### Initialization

```kotlin
private val partyTokens: Map<String, String> by lazy {
    // Reads npl.yml from sourcesPath
    // Extracts local.username, local.password, local.authUrl
    // Obtains JWT token from auth server
    // Returns map of party name -> JWT token
}
```

#### Token Acquisition

1. **Locate npl.yml**: Checks `config.sourcesPath` for `npl.yml`
2. **Parse Configuration**: Extracts `local` section
3. **Request Token**: POST to `{authUrl}/token` with form data:
   ```
   grant_type=password
   client_id=npl-cli
   username={username}
   password={password}
   ```
4. **Extract Access Token**: Parses JSON response for `access_token`

---

### Replay Execution Flow

#### Main Method: `runReplay(auditResponse: AuditResponse)`

```
1. Set actualBaseUrl (NPL_BASE_URL env var or config.baseUrl)
2. Validate audit log is not empty
3. Parse protocol identity from first entry
4. Extract protocol parties from state
5. For each audit entry:
   a. Execute action (constructor or permission/obligation)
   b. Verify state hash matches
6. Return ReplayResult with errors
```

---

### Step-by-Step Replay Process

#### 1. Parse Protocol Identity

**Input**: Entry ID URN
```
urn:npl:{host}/npl/{packagePath}/{protocolName}/{uuid}#{index}
```

**Example**:
```
urn:npl:localhost:12000/npl/demo/Iou/3340f93e-7639-4b26-8258-ce1cc162f908#0
```

**Parsed**:
```kotlin
ProtocolIdentity(
    host = "localhost:12000",
    packagePath = "demo",
    protocolName = "Iou",
    protocolId = "3340f93e-7639-4b26-8258-ce1cc162f908"
)
```

#### 2. Extract Protocol Parties

**Purpose**: Determine which parties are involved in the protocol

```kotlin
fun extractProtocolParties(state: Map<String, Any>): Set<String> {
    val parties = state["@parties"] as? Map<*, *>
    return parties?.keys?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
}
```

**Example**:
```json
{
  "@parties": {
    "issuer": {"claims": {"preferred_username": ["alice"]}},
    "payee": {"claims": {"preferred_username": ["bob"]}}
  }
}
```
**Result**: `Set("issuer", "payee")`

---

#### 3. Replay Constructor

**Purpose**: Create protocol instance (for new instance replay)

**URL**: `POST {baseUrl}/npl/{package}/{protocol}/`

**Body**:
```json
{
  "@parties": {...},
  "forAmount": 10,
  // ... other constructor parameters
}
```

**Body Construction**:
1. Extract `@parties` from audit state
2. Add constructor parameters from `entry.action.parameters`
3. Skip generic parameters (`arg0`, `arg1`, etc.)
4. Add state fields that look like constructor params
5. Normalize types (convert string numbers to numeric types)

**Token Selection**:
1. Try party that owns constructor (from NPL parsing)
2. Try protocol parties
3. Try remaining tokens

**Response Handling**:
- **200-299**: Extract `@id` from response, return as `createdProtocolId`
- **403**: Try next token
- **Other**: Log error, add to errors list, abort

**Example Output**:
```
POST http://localhost:12000/npl/demo/Iou/
  Constructor body: {"@parties":{...},"forAmount":10}
  Found: action 'pay' -> party 'issuer'
  Found: action 'forgive' -> party 'payee'
    Trying token for party: alice
    ✓ Success with party: alice
  Created protocol with @id: 62d438c2-2124-4ad9-91ab-4a77604b45b6
```

---

#### 4. Replay Actions

**Purpose**: Execute permission/obligation actions

**URL**: `POST {baseUrl}/npl/{package}/{protocol}/{id}/{actionName}`

**Body**:
```json
{
  "amount": 1
}
```

**Body Construction**:
1. Extract parameters from `entry.action.parameters`
2. Normalize types (string → int/double/boolean if applicable)
3. Serialize to JSON

**Token Selection**:
1. Parse NPL source files to find action owner
2. Try owner's token first
3. Try protocol parties' tokens
4. Try remaining tokens

**Response Handling**:
- **200-299**: Success, return
- **403**: Try next token
- **Other**: Log error, add to errors list, abort

**Example Output**:
```
POST http://localhost:12000/npl/demo/Iou/62d438c2.../pay
  Action body: {"amount":1}
  Action 'pay' is owned by party 'issuer' (from NPL)
    Trying token for party: alice
    ✓ Success with party: alice
```

---

#### 5. Verify State Hash (After Each Action)

**Purpose**: Verify replayed state matches audit trail's expected state hash

**URL**: `GET {baseUrl}/npl/{package}/{protocol}/{id}/`

**Algorithm**:

```kotlin
1. Fetch current state from runtime
   GET /npl/{package}/{protocol}/{id}/
   
2. Parse response as Map<String, Any?>
   
3. Normalize protocol IDs (for UUID differences)
   - Replace runtime ID with audited ID in @id field
   - Replace runtime ID with audited ID in @actions URLs
   
4. Apply ReplayStateProjection.fromRestState()
   
5. Serialize projection to JSON
   
6. Compute hash: sha256(JCS(json))
   
7. Compare with entry.stateHash
```

**Why ID Normalization?**

When replay creates a **new** protocol instance (not using the audited ID), the `@id` and `@actions` URLs will have different UUIDs. Normalization allows hash comparison by replacing the runtime UUID with the audited UUID.

**Example**:

Before normalization:
```json
{
  "@id": "62d438c2-2124-4ad9-91ab-4a77604b45b6",  // runtime ID
  "@actions": {
    "pay": "http://localhost:12000/npl/demo/Iou/62d438c2-2124-4ad9-91ab-4a77604b45b6/pay"
  }
}
```

After normalization:
```json
{
  "@id": "3340f93e-7639-4b26-8258-ce1cc162f908",  // audited ID
  "@actions": {
    "pay": "http://localhost:12000/npl/demo/Iou/3340f93e-7639-4b26-8258-ce1cc162f908/pay"
  }
}
```

**IMPORTANT**: Only `@id` and `@actions` are normalized. All other fields (like `@state`, protocol fields) are **not** modified. This means:
- ✅ UUID differences are handled
- ✅ NPL code changes (state renames, field changes) are **detected**

**Debug Output**:
```
GET http://localhost:12000/npl/demo/Iou/62d438c2.../
  Expected: sha256:89c86309092078e79ea354ba97d1ea896ed09619e7ae0aea531f1c0a74ca961c
  Computed: sha256:89c86309092078e79ea354ba97d1ea896ed09619e7ae0aea531f1c0a74ca961c
  Replay state: @state=unpaid, fields=[forAmount]
```

---

### NPL Source Parsing

**Purpose**: Determine which party owns which action

**Location**: `parseNplPermissions(protocolName: String)`

**Algorithm**:
1. Walk `config.sourcesPath` directory
2. Find `.npl` files containing `protocol` and `protocolName`
3. Parse with regex: `(permission|obligation)\[([^]]+)]\s+(\w+)\s*\(`
4. Extract party name and action name
5. Return map: `actionName -> partyName`

**Example NPL**:
```npl
protocol Iou(issuer: Party, payee: Party, forAmount: Number) {
    permission[issuer] pay(amount: Number) { ... }
    permission[payee] forgive() { ... }
}
```

**Parsed Map**:
```kotlin
{
  "pay" -> "issuer",
  "forgive" -> "payee"
}
```

---

### Type Normalization

**Purpose**: Convert string representations to actual types

```kotlin
fun normalizeTypes(value: Any): Any {
    return when (value) {
        is String -> value.toIntOrNull() 
                  ?: value.toDoubleOrNull()
                  ?: value.toBooleanStrictOrNull()
                  ?: value
        is Map<*, *> -> value.mapValues { normalizeTypes(it.value) }
        is List<*> -> value.map { normalizeTypes(it) }
        else -> value
    }
}
```

**Example**:
```kotlin
normalizeTypes("10")       // → 10 (Int)
normalizeTypes("3.14")     // → 3.14 (Double)
normalizeTypes("true")     // → true (Boolean)
normalizeTypes("hello")    // → "hello" (String)
```

---

### Hash Computation

**Method**: `computeStateHash(json: String)`

```kotlin
1. Canonicalize JSON using JCS (RFC 8785)
   val canonicalJson = JsonCanonicalizer(json).encodedString
   
2. Compute SHA-256 hash
   val digest = MessageDigest.getInstance("SHA-256")
   val hashBytes = digest.digest(canonicalJson.toByteArray(UTF_8))
   
3. Format as "sha256:{hex}"
   return "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
```

**Why JCS?**

JSON Canonicalization Scheme (RFC 8785) ensures:
- Deterministic property ordering
- Consistent number representation
- Uniform whitespace handling
- Same JSON → same canonical form → same hash

---

## Data Models

### AuditResponse

```kotlin
data class AuditResponse(
    val audit_log: List<AuditEntry>,
    val state: Map<String, Any>
)
```

### AuditEntry

```kotlin
data class AuditEntry(
    val id: String,                        // URN: urn:npl:{host}/npl/{pkg}/{proto}/{uuid}#{index}
    val timestamp: String,                 // ISO 8601 timestamp
    val action: ActionData,
    val notificationHashes: List<String>,
    val stateHash: String,                 // sha256:{hex}
    val previousHash: String?,             // sha256:{hex} or null for entry 0
    val proof: ProofData
)
```

### ActionData

```kotlin
data class ActionData(
    val type: String?,                     // "constructor", "permission", "obligation"
    val name: String,                      // Action name
    val parameters: Map<String, Any>?      // Action parameters
)
```

### ProofData

```kotlin
data class ProofData(
    val type: String,                      // "Ed25519Signature2020"
    val created: String,                   // ISO 8601 timestamp
    val verificationMethod: String,        // DID URI with fragment
    val proofPurpose: String,              // "assertionMethod"
    val jws: String                        // JWS: {header}.{payload}.{signature}
)
```

### DidDocument

```kotlin
data class DidDocument(
    val id: String,                        // DID URI
    val verificationMethod: List<VerificationMethod>
)
```

### VerificationMethod

```kotlin
data class VerificationMethod(
    val id: String,                        // Full verification method URI
    val type: String,                      // "Ed25519VerificationKey2020"
    val controller: String,                // DID URI
    val publicKeyJwk: PublicKeyJwk?
)
```

### PublicKeyJwk

```kotlin
data class PublicKeyJwk(
    val kty: String,                       // "OKP" (Octet Key Pair)
    val crv: String,                       // "Ed25519"
    val x: String                          // Base64url-encoded public key
)
```

---

## Security & Cryptography

### Ed25519 Signature Verification

**Algorithm**: Ed25519 (Edwards-curve Digital Signature Algorithm)

**Key Properties**:
- **Public key size**: 32 bytes
- **Signature size**: 64 bytes
- **Security level**: ~128-bit
- **Speed**: Very fast verification
- **Deterministic**: Same message + key → same signature

**Verification Steps**:
1. Extract 32-byte public key from JWK
2. Decode base64url signature (64 bytes)
3. Construct signing input: `"{header}.{payload}"` as US-ASCII bytes
4. Verify: `Ed25519.verify(publicKey, signingInput, signature)`

### JSON Canonicalization Scheme (JCS)

**RFC**: 8785

**Purpose**: Ensure deterministic JSON serialization for hashing

**Features**:
- Lexicographic property ordering
- No whitespace
- Unicode normalization
- Number serialization rules
- No array/object trailing commas

**Example**:
```json
// Input (any order, whitespace)
{
  "b": 2,
  "a": 1
}

// Canonical output
{"a":1,"b":2}
```

### SHA-256 Hashing

**Algorithm**: SHA-256 (Secure Hash Algorithm 256-bit)

**Properties**:
- **Output size**: 32 bytes (256 bits)
- **Collision resistance**: 2^128 operations
- **Pre-image resistance**: 2^256 operations
- **Format**: `sha256:{64 hex characters}`

---

## Error Handling

### Error Types

#### VerificationError

```kotlin
data class VerificationError(
    val step: String,     // "Structure", "HashChain", "StateHash", "Signature", "Replay"
    val message: String   // Detailed error message
)
```

### Fail-Fast Mode

When `--fail-fast` is enabled:
- Verification stops at first error
- Remaining steps are skipped
- Partial results are returned

When `--fail-fast` is disabled:
- All verification steps execute
- All errors are collected
- Complete results are returned

### Replay Errors

```kotlin
data class ReplayError(
    val entryIndex: Int,   // Audit entry index (-1 for general errors)
    val message: String    // Detailed error message
)
```

**Converted to VerificationError**:
```kotlin
VerificationError(
    step = "Replay",
    message = "Entry {entryIndex}: {message}"
)
```

---

## Environment Variables

### Runtime Configuration

```bash
NPL_BASE_URL              # Override runtime base URL (default: http://localhost:12000)
NPL_DOCKER_COMPOSE_CMD    # Docker compose command (not used in current implementation)
NPL_DEPLOY_CMD            # Deploy command (not used)
NPL_SKIP_DOCKER           # Skip Docker startup (always true)
NPL_CLEANUP               # Cleanup after replay (default: false)
```

---

## File Structure

```
src/main/kotlin/com/noumenadigital/npl/cli/
├── commands/registry/
│   └── VerifyCommand.kt              # Command entry point
├── service/
│   ├── AuditVerificationService.kt   # Main verification orchestration
│   ├── DidResolver.kt                # DID resolution
│   └── ReplayRunner.kt               # Replay verification
├── util/
│   └── ReplayStateProjection.kt      # State transformation
└── model/
    ├── AuditEntry.kt                 # Data models
    ├── AuditResponse.kt
    └── DidDocument.kt
```

---

## Typical Execution Timeline

```
1. Parse command arguments                [<1ms]
2. Load audit JSON from file/URL          [10-100ms]
3. Structure validation                   [1ms]
4. Hash-chain verification                [5-50ms, depends on log size]
5. State hash verification                [1ms]
6. Signature verification                 [100-500ms, includes DID resolution + crypto]
   - Resolve DID documents (cached)       [50-200ms per unique DID]
   - Verify Ed25519 signatures            [~0.1ms per signature]
7. Replay verification (if enabled)       [1-30s, depends on protocol complexity]
   - Obtain JWT token                     [100-500ms]
   - Execute constructor                  [100-500ms]
   - Execute actions                      [100-500ms per action]
   - Verify state hashes                  [50-200ms per entry]
8. Format and output results              [<1ms]
```

**Total Time**:
- Without replay: ~500ms - 1s
- With replay: ~2s - 30s

---

## Common Failure Scenarios

### 1. Structure Validation Failures
- Missing required fields in audit entries
- Empty audit log
- Malformed JSON

### 2. Hash-chain Failures
- Tampered entries (previousHash doesn't match)
- Entry reordering
- Missing entries

### 3. State Hash Failures
- NPL code has changed since audit
- State has been tampered with
- Projection mismatch (backend vs CLI)

### 4. Signature Failures
- Invalid JWS format
- Signature doesn't verify
- DID resolution failure
- Unsupported key type

### 5. Replay Failures
- Runtime not accessible
- Authentication failure
- NPL code has changed
- State transition mismatch
- Permission errors

---

## Testing Considerations

### Unit Testing
- Mock `DidResolver` for signature tests
- Mock HTTP client for replay tests
- Test canonicalization edge cases
- Test type normalization

### Integration Testing
- Test against real NPL runtime
- Test with real DID documents
- Test full audit trail replay
- Test fail-fast behavior

### Test Data Requirements
- Valid audit JSON files
- NPL source files
- Running NPL runtime (for replay)
- Auth server (for token acquisition)
- DID documents (or mock resolver)

---

## Summary

The `verify` command provides comprehensive audit trail verification through a multi-stage pipeline:

1. **VerifyCommand**: Command-line interface and parameter parsing
2. **AuditVerificationService**: Orchestrates all verification steps
3. **DidResolver**: Resolves DIDs to public keys for signature verification
4. **ReplayRunner**: Re-executes protocol actions to verify state transitions
5. **ReplayStateProjection**: Transforms states for hash comparison

The verification process ensures:
- ✅ Structural integrity (all required fields present)
- ✅ Cryptographic integrity (signatures verify with resolved DIDs)
- ✅ Hash-chain integrity (previousHash links valid)
- ✅ State integrity (final state matches last entry)
- ✅ Behavioral integrity (replay produces same state transitions)

This provides strong guarantees that the audit trail is:
- **Authentic**: Signed by authorized parties
- **Complete**: No missing or reordered entries
- **Tamper-proof**: Any modification breaks hash chain
- **Reproducible**: Replay produces same results

