# Quick Start: Replay Verification

## TL;DR

```bash
# Basic usage - starts Docker, deploys sources, replays audit
npl verify --audit audit.json --sources ./my-protocol

# With cleanup
NPL_CLEANUP=true npl verify --audit audit.json --sources ./my-protocol

# Use existing runtime (skip Docker)
NPL_SKIP_DOCKER=true npl verify --audit audit.json --sources ./my-protocol

# Disable replay (crypto verification only)
npl verify --audit audit.json --sources ./my-protocol --no-replay
```

## Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `NPL_BASE_URL` | `http://localhost:12000` | Runtime API endpoint |
| `NPL_SKIP_DOCKER` | `false` | Skip Docker startup |
| `NPL_CLEANUP` | `false` | Tear down Docker after |
| `NPL_DOCKER_COMPOSE_CMD` | `docker compose up -d --wait` | Docker command |
| `NPL_DEPLOY_CMD` | `npl deploy` | Deploy command (reads npl.yml) |

**Note**: Ensure `npl.yml` exists in your sources directory with deploy configuration:
```yaml
structure:
  sourceDir: ./main
local:
  username: admin
  password: admin
```

## What Gets Verified?

1. ✓ Structure validation (audit format)
2. ✓ Hash chain (previousHash linkage)
3. ✓ State hashes (cryptographic integrity)
4. ✓ Signatures (DID-based authentication)
5. ✓ **Replay** (state evolution accuracy)

## Replay Process

```
Start Docker → Deploy Sources → Parse Protocol → Execute Actions → Verify State Hashes
```

For each audit entry:
1. Execute the action (constructor, permission, or obligation)
2. Fetch the resulting protocol state via REST API
3. Compute hash using JCS + SHA-256
4. Compare to audit entry's `stateHash`

## Common Issues

### "Docker not found"
Install Docker or use `NPL_SKIP_DOCKER=true` with a pre-running runtime.

### "Deploy failed"
Ensure NPL sources are valid and runtime is accessible.

### "State hash mismatch"
- Protocol logic may be non-deterministic (uses timestamps, random values)
- Runtime version mismatch
- Sources don't match original audit trail

## Example Script

Use the provided example:

```bash
./example/verify-with-replay.sh audit.json ./my-protocol
```

## Full Documentation

See `docs/replay-verification.md` for complete details.

