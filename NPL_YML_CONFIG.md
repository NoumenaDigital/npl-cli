
# NPL.yml Configuration for Replay Verification

When using replay verification, the `npl deploy` command requires a `npl.yml` configuration file. The replay runner will automatically search for this file:

1. In the sources directory specified by `--sources`
2. In parent directories (up to 5 levels up)

This allows flexible project structures where `npl.yml` might be at the project root.

## Required Configuration

Create a `npl.yml` file with the following structure:

```yaml
structure:
  sourceDir: ./main  # Path to your NPL sources (relative to npl.yml location)

local:
  managementUrl: http://localhost:12000  # NPL runtime management URL
  authUrl: http://localhost:11000        # OIDC authentication server URL
  
  # Option 1: Multiple parties with credentials (recommended - tokens obtained automatically)
  parties:
    alice:
      username: "alice"
      password: "password123"
    bob:
      username: "bob"
      password: "password456"
    charlie:
      username: "charlie"
      password: "password789"
  
  # Option 2: Multiple parties with pre-existing JWT tokens
  # parties:
  #   alice: "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9..."
  #   bob: "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9..."
  
  # Option 3: Single user credentials (legacy, tokens obtained for single user)
  # username: admin
  # password: admin
  
  # Option 4: Single authorization token (legacy, fallback for all parties)
  # authorization: "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9..."
```

### Authentication for Replay Verification

When replaying an audit trail, the system needs to authenticate as different parties to execute their actions. The replay runner intelligently determines which party should execute each action by:

1. **Parsing the NPL source code** to extract permission/obligation definitions
2. **Matching actions to parties** based on the NPL syntax: `permission[party] actionName(...)`
3. **Using the correct token** for that party when replaying the action

**Example NPL code:**
```npl
protocol[issuer, payee] Iou(var forAmount: Number) {
    permission[issuer] pay(amount: Number) {
        // Only issuer can call this
    }
    
    permission[payee] forgive() {
        // Only payee can call this
    }
    
    permission[issuer|payee] getAmountOwed() {
        // Either party can call this
    }
}
```

**Replay behavior:**
- When replaying `pay` action → automatically uses `issuer` token
- When replaying `forgive` action → automatically uses `payee` token
- When replaying `getAmountOwed` → tries `issuer` first, then `payee`
- Fallback: If NPL parsing fails or party not found, tries all available tokens

**Token selection priority:**
1. **NPL-defined party** for the action (highest priority)
2. **Protocol parties** from the `@parties` field in state
3. **All available tokens** (fallback)

## Example Directory Structure

```
my-protocol/
├── npl.yml                  ← Configuration file
├── docker-compose.yml       ← Docker runtime setup
└── main/                    ← NPL sources
    ├── npl-1.0/
    │   └── demo/
    │       └── Iou.npl
    └── migration.yml
```

## Running Replay Verification

With the `npl.yml` in place:

```bash
# From the protocol directory (where npl.yml is located)
npl verify --audit audit.json --sources .

# Or specify the full path
npl verify --audit audit.json --sources /path/to/my-protocol
```

## Automatic URL Detection

The replay runner automatically detects the `managementUrl` from your `npl.yml` file and uses it for replay verification. This ensures that the deployment and replay use the same runtime endpoint.

For example, if your `npl.yml` contains:
```yaml
local:
  managementUrl: http://localhost:12400
```

The replay will automatically use `http://localhost:12400` for REST API calls.

## Environment Variables

You can still override settings with environment variables:

```bash
# Override runtime URL (takes precedence over npl.yml)
NPL_BASE_URL=http://localhost:8080 npl verify --audit audit.json --sources .

# Skip Docker if runtime is already running
NPL_SKIP_DOCKER=true npl verify --audit audit.json --sources .

# Clean up after verification
NPL_CLEANUP=true npl verify --audit audit.json --sources .
```

**Note**: If `NPL_BASE_URL` is set, it takes precedence over the `managementUrl` in `npl.yml`.

## Troubleshooting

### "Missing required parameter(s): source-dir, username, password"

This error means `npl.yml` is missing or incorrectly configured.

**Solution**:
1. Ensure `npl.yml` exists in the sources directory
2. Verify the structure matches the example above
3. Check that `sourceDir` path is correct relative to `npl.yml`

### "Failed to deploy sources"

**Possible causes**:
- Runtime is not running (ensure Docker is started)
- `managementUrl` doesn't match the runtime
- Invalid credentials
- Invalid NPL sources

**Solution**:
```bash
# Check if runtime is accessible
curl http://localhost:12000/health

# Check Docker status
docker compose ps

# Try deploying manually first
npl deploy --source-dir ./main --username admin --password admin
```

## Full Example npl.yml

For a complete setup including cloud deployment:

```yaml
structure:
  sourceDir: ./main
  testSourceDir: ./test
  frontend: ./frontend

local:
  managementUrl: http://localhost:12000
  authUrl: http://localhost:12000
  username: admin
  password: admin

# Optional: Cloud deployment configuration
cloud:
  tenant: my-tenant
  app: my-app
  url: https://my-tenant.noumenadigital.com
```

## See Also

- [Replay Verification Documentation](docs/replay-verification.md)
- [Quick Start Guide](REPLAY_QUICKSTART.md)
- [NPL CLI Documentation](docs/runtime/tools/build-tools/cli.md)

