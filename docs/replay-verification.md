# Replay Verification

## Overview

The NPL CLI implements **replay verification** for NOUMENA audit trails, allowing you to verify that the state evolution recorded in an audit log can be reproduced by executing the same actions against a live NPL runtime.

## What is Replay Verification?

Replay verification complements cryptographic verification by:

1. **Starting a clean NPL runtime** (using Docker Compose)
2. **Deploying the protocol sources** that were used to create the audit trail
3. **Executing each action** from the audit log in sequence
4. **Comparing the resulting state hash** after each action to the hash recorded in the audit trail

This ensures that:
- The audit trail accurately represents the state evolution
- The protocol logic produces deterministic results
- The state hashes in the audit are correct

## Usage

### Basic Usage

```bash
npl verify --audit audit.json --sources /path/to/npl-sources
```

By default, replay verification is **enabled** when both `--audit` and `--sources` are provided.

### Disable Replay

To run only cryptographic verification (hash chain, signatures, etc.):

```bash
npl verify --audit audit.json --sources /path/to/npl-sources --no-replay
```

### Environment Variables

You can customize replay behavior using environment variables:

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| `NPL_BASE_URL` | Auto-detected from `npl.yml` or `http://localhost:12000` | Base URL of the NPL runtime (overrides auto-detection) |
| `NPL_DOCKER_COMPOSE_CMD` | `docker compose up -d --wait` | Command to start Docker runtime |
| `NPL_DEPLOY_CMD` | `npl deploy` | Command to deploy NPL sources (reads from npl.yml) |
| `NPL_SKIP_DOCKER` | `false` | Skip Docker startup (assumes runtime is already running) |
| `NPL_CLEANUP` | `false` | Tear down Docker after replay |

**Automatic URL Detection**: If `NPL_BASE_URL` is not set, the replay runner automatically reads `local.managementUrl` from `npl.yml`. This ensures deployment and replay use the same runtime endpoint.

**Note**: The deploy command reads configuration from `npl.yml` in your sources directory. Ensure your `npl.yml` contains:
```yaml
structure:
  sourceDir: ./path/to/sources
local:
  username: admin
  password: admin
  managementUrl: http://localhost:12000
```

### Examples

#### Use an already-running runtime

```bash
NPL_SKIP_DOCKER=true npl verify --audit audit.json --sources /path/to/sources
```

#### Clean up after verification

```bash
NPL_CLEANUP=true npl verify --audit audit.json --sources /path/to/sources
```

#### Use a custom runtime URL

```bash
NPL_BASE_URL=http://localhost:8080 npl verify --audit audit.json --sources /path/to/sources
```

## How Replay Works

### 1. Protocol Identity Parsing

Each audit entry has an ID in URN format:

```
urn:npl:{host}/npl/{packagePath}/{protocolName}/{uuid}#{index}
```

Example:
```
urn:npl:localhost/npl/demo/Iou/550e8400-e29b-41d4-a716-446655440000#0
```

This is parsed to determine:
- **Package path**: `demo`
- **Protocol name**: `Iou`
- **Protocol ID**: `550e8400-e29b-41d4-a716-446655440000`

### 2. Runtime Startup

If `NPL_SKIP_DOCKER=false` (default):
1. Execute `docker compose up -d --wait` in the sources directory
2. Wait for runtime to become healthy
3. Execute `npl deploy` to deploy the protocol sources

### 3. Action Replay

For each entry in the audit log:

#### Constructor Actions

```http
POST /npl/{packagePath}/{protocolName}
Content-Type: application/json

{
  "@parties": { ... },
  "param1": value1,
  "param2": value2
}
```

The `@parties` are extracted from the audit response's final state.

#### Permission/Obligation Actions

```http
POST /npl/{packagePath}/{protocolName}/{id}/{actionName}
Content-Type: application/json

{
  "0": value1,
  "1": value2
}
```

### 4. State Hash Verification

After each action:

1. Fetch the protocol state: `GET /npl/{packagePath}/{protocolName}/{id}`
2. Canonicalize the JSON response using **JCS (RFC 8785)**
3. Compute SHA-256 hash
4. Prefix with `sha256:`
5. Compare to the `stateHash` in the audit entry

If any state hash doesn't match, replay verification fails with a detailed error message.

## State Hash Computation

State hashes are computed as follows:

```kotlin
// 1. Get JSON state from runtime
val stateJson = httpGet("/npl/demo/Iou/{id}")

// 2. Canonicalize using JCS
val canonicalJson = JsonCanonicalizer(stateJson).encodedString

// 3. Compute SHA-256
val hashBytes = MessageDigest.getInstance("SHA-256")
    .digest(canonicalJson.toByteArray(StandardCharsets.UTF_8))

// 4. Format as hex with prefix
val stateHash = "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
```

This ensures deterministic hashing regardless of key order or whitespace in the JSON.

## Troubleshooting

### Docker Not Found

```
Failed to start Docker runtime: docker: command not found
```

**Solution**: Install Docker and Docker Compose, or use `NPL_SKIP_DOCKER=true` with a pre-running runtime.

### Deploy Failed

```
Failed to deploy sources: npl deploy exited with code 1
```

**Solution**: Ensure your sources are valid NPL code and the runtime is accessible. Check that `npl` CLI is in your PATH.

### State Hash Mismatch

```
State hash mismatch at entry 2.
    Expected: sha256:abc123...
    Computed: sha256:def456...
```

**Possible causes**:
1. **Non-deterministic protocol logic** (e.g., using timestamps, random values)
2. **Different runtime version** than was used to create the audit
3. **Modified sources** that don't match the original audit trail
4. **Runtime includes extra metadata** (e.g., `@actions` links with different hosts)

**Solutions**:
- Ensure protocol logic is deterministic
- Use the same runtime version
- Verify sources match the audit trail exactly
- Check if `@actions` or other metadata causes hash differences

### UUID Mismatch

```
WARNING: Created ID (123e4567-...) differs from audit ID (550e8400-...)
```

This warning indicates the runtime generated a different UUID than expected. This is usually harmless if the runtime doesn't support client-provided IDs, but the rest of replay will use the runtime-generated ID.

## Implementation Notes

### For Protocol Developers

To ensure your protocols are replay-compatible:

1. **Avoid non-determinism**:
   - Don't use `now()` or current timestamps
   - Don't use random number generation
   - Don't depend on external API calls during state computation

2. **Use deterministic ordering**:
   - If iterating over sets or maps, use sorted order
   - Avoid relying on insertion order

3. **Test replay locally**:
   ```bash
   npl verify --audit audit.json --sources . --cleanup
   ```

### For Runtime Developers

State returned by `GET /npl/{package}/{protocol}/{id}` should:
- Be the "pretty" protocol state (user-friendly representation)
- Exclude volatile metadata like timestamps unless part of protocol state
- Use consistent JSON serialization (key order doesn't matter due to JCS)

## Architecture

The replay implementation consists of:

- **`ReplayRunner`**: Main orchestration class
  - Manages Docker lifecycle
  - Parses protocol identity from URNs
  - Executes HTTP calls to runtime
  - Computes and compares state hashes

- **`ReplayConfig`**: Configuration data class
  - Sources path
  - Runtime URL
  - Docker commands
  - Flags (skipDocker, cleanup, verbose)

- **`AuditVerificationService.verifyReplay()`**: Integration point
  - Called when replay is enabled
  - Converts replay errors to verification errors

## Future Enhancements

Potential improvements for replay verification:

1. **Notification verification**: Compare notification hashes to actual notifications
2. **OpenAPI introspection**: Parse OpenAPI spec to validate action signatures
3. **Parallel replay**: Replay multiple audit trails simultaneously
4. **Snapshot checkpoints**: Save runtime state at intervals for faster partial replays
5. **Differential replay**: Only replay from a specific checkpoint
6. **Multi-protocol replay**: Handle audit trails spanning multiple protocols

## See Also

- [Verification Documentation](../docs/verify.md)
- [Audit Trail Specification](../docs/audit-trail-spec.md)
- [NPL Runtime API](https://documentation.noumenadigital.com/)

