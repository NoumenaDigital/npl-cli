# NPL CLI

You can find the full CLI documentation here: [cli.md](docs/runtime/tools/build-tools/cli.md)

## Versioning

The NPL CLI uses a versioning scheme aligned with Noumena Platform releases.

### Examples

- `2025.1.2` — Base release matching the Noumena Platform version.
- `2025.1.2-1` — First CLI-specific patch based on platform version `2025.1.2`.

This helps ensure consistency between the platform and CLI tool while allowing independent CLI updates when needed.

# Development

## Prerequisites

You'll need Maven and Java 24 (graalvm if you want to build binaries) or later installed on your system.

## Build Profiles

The project has three build profiles for different testing scenarios:

### Default Profile

Regular testing that runs integration tests directly without using binary:

```bash
mvn clean test
```

### Native Profile

Builds a native binary and runs integration tests against it:

```bash
mvn clean verify -Pnative
```

### Config Generation Profile

Builds a fat JAR and instruments it with the native agent to generate GraalVM native image configuration:

```bash
mvn clean verify -Pconfig-gen
```

## Automated checks and formatting

You can use `pre-commit` to run some automated checks as well as Kotlin (ktlint) and Markdown formatting whenever you
commit something.

```shell
brew install pre-commit
pre-commit install
```

The checks can be bypassed by running `git commit -n` or `git commit --no-verify`

### detect-secrets

Enforces accidental secret commits with a pre-commit hook using Yelp's detect-secrets tool. The pre-commit hook will
scan staged changes for potential secrets before allowing a commit. If any secrets are detected above baseline, the
commit will be aborted, ensuring that no sensitive information is pushed to the repository.

No need to install locally, just make sure you have the pre-commit hook installed.

To check locally:

```shell
pre-commit run detect-secrets --all-files
```

To generate a new baseline, install the version of `detect-secrets` that's configured in `.pre-commit-config.yaml`
locally (using `pip` or `brew` -- just double check that you have the right version) and run:

```shell
detect-secrets scan > .secrets.baseline
```

### ktlint

This project enforces a standard code formatting style using [ktlint](https://github.com/pinterest/ktlint) via the
automatic `pretty-format-kotlin` [pre-commit hook](https://github.com/macisamuele/language-formatters-pre-commit-hooks).

The `pretty-format-kotlin` hook automatically formats Kotlin code with ktlint rules before committing it.

You can run ktlint for the entire project using the `pre-commit` like so:

```shell
pre-commit run pretty-format-kotlin --all-files
```

### prettier

We use [prettier](https://prettier.io) to format our Markdown. The configuration is found in
[.prettierrc.yml](.prettierrc.yml).

To format all Markdown files in the project, run (needed if e.g. the corresponding job is failing):

```shell
pre-commit run prettier --all-files
```

Note that `prettier` formats tables differently than IntelliJ, so you might want to disable IntelliJ's
`Incorrect table formatting` Markdown inspection.

## Version

The NPL CLI follows a versioning scheme aligned with Noumena Platform releases.

### Updating the Version

- You must manually update the version in the `pom.xml` file.
- For a **first-time release**, set the version to match the Noumena Platform version (e.g., `2025.1.2`).
- For **CLI patches**, append a patch number starting from `-1` (e.g., `2025.1.2-1`).

Make sure the version you set correctly reflects the corresponding Noumena Platform release.

## Old CLI

The older CLI was intended for use with Noumena Cloud. It is no longer supported and has been replaced by the new CLI.
The old CLI is still available under the GitHub releases, and is documented in the [old CLI README](OLD-CLI-README.md).

## Deploy Command

The `deploy` command allows you to deploy NPL sources to a Noumena Engine instance. In order to use the command, you
need to configure appropriate settings in the [npl.yml file](docs/runtime/tools/build-tools/cli.md#configuration-file).

### Usage

```bash
npl deploy --source-dir <directory> [--clear]
```

Where:

- `[directory]` (required) is the path to the directory containing NPL sources
- `[--clear]` (optional) clears the application contents before deployment

## Cloud login command

The `cloud login` command allows you to login to NOUMENA cloud with device token flow.

### Usage

```bash
npl cloud login
```

That command will login you to the NOUMENA cloud and store the access token in the ~/.noumena folder.

| Args         | Default values                             | Can be overridden |
| ------------ | ------------------------------------------ | ----------------- |
| clientId     | paas                                       | Yes               |
| clientSecret | paas                                       | Yes               |
| url          | https://keycloak.noumena.cloud/realms/paas | Yes               |

## Cloud deploy command

The `cloud deploy` command allows you to deploy NPL sources to a NOUMENA cloud application.

### Usage

```bash
npl cloud deploy --appId <applicationUUID> --source-dir <directory>
```

That command will deploy your sources to the NOUMENA cloud application.

| Args         | Default values                             | Can be overridden |
| ------------ | ------------------------------------------ | ----------------- |
| app          | -                                          | Yes               |
| tenant       | -                                          | Yes               |
| url          | https://portal.noumena.cloud               | Yes               |
| sourceDir    | .                                          | Yes               |
| authUrl      | https://keycloak.noumena.cloud/realms/paas | Yes               |
| clientId     | paas                                       | Yes               |
| clientSecret | paas                                       | Yes               |

## Cloud clear command

The `cloud clear` command allows you to clear the contents of a NOUMENA cloud application.

### Usage

```bash
npl cloud clear --appId <applicationUUID>
```

That command will remove your application from the NOUMENA cloud.

| Args         | Default values                             | Can be overridden |
| ------------ | ------------------------------------------ | ----------------- |
| app          | -                                          | Yes               |
| tenant       | -                                          | Yes               |
| url          | https://portal.noumena.cloud               | Yes               |
| authUrl      | https://keycloak.noumena.cloud/realms/paas | Yes               |
| clientId     | paas                                       | Yes               |
| clientSecret | paas                                       | Yes               |

## Cloud status command

The `cloud status` command allows you to list all your tenants and applications in NOUMENA cloud with their current
status.

### Usage

```bash
npl cloud status
```

That command will display a tree of your tenants and their applications with status indicators.

Example output:

```
📂 My Tenant (my-tenant) [active] 🟢
  ├── 📦 My App (my-app) [active] 🟢
  └── 📦 Another App (another-app) [pending] 🟡
📂 Other Tenant (other-tenant) [deactivated] 🔴
```

| Args         | Default values                             | Can be overridden |
| ------------ | ------------------------------------------ | ----------------- |
| url          | https://portal.noumena.cloud               | Yes               |
| authUrl      | https://keycloak.noumena.cloud/realms/paas | Yes               |
| clientId     | paas                                       | Yes               |
| clientSecret | paas                                       | Yes               |

# NPL CLI Verify Command Implementation

## Overview

The `npl verify` command has been implemented to verify NOUMENA verifiable protocol audit trails according to the NOUMENA Network whitepaper.

## Command Usage

```bash
npl verify --audit <file-or-url> --sources <path> [options]
```

### Required Arguments
- `--audit <file-or-url>` - Path to audit JSON file or HTTP(S) URL
- `--sources <path>` - Path to local NPL sources directory or zip

### Optional Arguments
- `--did-scheme <http|https>` - Scheme for DID resolution (default: https)
- `--did-host-override <host:port>` - Override host for DID resolution (e.g., localhost:8080 for testing)
- `--fail-fast` - Stop verification on first error
- `--json` - Output results in JSON format
- `--no-replay` - Disable replay verification

## Verification Steps

The implementation performs the following verification steps according to the whitepaper:

### A) Structure Validation
- Validates that `audit_log` array and `state` object exist
- Checks that each entry has all required fields
- Validates proof fields exist

### B) Hash-Chain Completeness
- Recomputes unsigned entry (entry with `proof` removed)
- Computes `entryHashBytes = SHA-256(JCS(unsignedEntry))`
- Verifies `previousHash` chain:
    - First entry: `previousHash` must be null
    - Subsequent entries: `previousHash` must equal `"sha256:" + hex(entryHashBytes)` of previous entry

### C) State Hash Verification
- Computes `computedStateHash = "sha256:" + hex(SHA-256(JCS(state)))`
- Verifies it matches `audit_log.last().stateHash`

### D) DID Resolution + Signature Verification
For each entry:
- Resolves `proof.verificationMethod` as did:web
    - Example: `did:web:example.com` => GET `{scheme}://example.com/.well-known/did.json`
    - Supports `--did-host-override` for development/testing
- Extracts Ed25519 public key from DID document's `publicKeyJwk` (kty=OKP, crv=Ed25519)
- Parses compact JWS into 3 parts (header.payload.signature)
- Verifies JWS payload equals `entryHashBytes` exactly
- Verifies Ed25519 signature over "headerB64.payloadB64" using `Signature.getInstance("Ed25519")`

### E) Replay Verification (Stub)
- Currently returns a warning that replay verification is not fully implemented
- Cryptographic checks (steps A-D) are fully functional
- Full replay would require:
    - Extracting protocol info from `entry.id` URN
    - Loading and compiling NPL sources
    - Instantiating protocol and replaying all actions
    - Comparing computed state/notification hashes with entry values

### JSON Canonicalization

Custom JCS implementation following RFC 8785:
- Lexicographic sorting of object keys
- Specific number formatting rules
- Proper string escaping
- Deterministic output for hashing

### Ed25519 Signature Verification

- Uses Java's built-in `Signature.getInstance("Ed25519")`
- Extracts public key from JWK format
- Verifies compact JWS signatures
- Base64url decoding/encoding support

## Exit Codes

- `0` - Verification successful
- `1` - Verification failed (DATA_ERROR)
- `2` - General error (file not found, invalid arguments, etc.)

## Examples

### Basic Verification
```bash
npl verify --audit audit.json --sources ./npl-sources
```

### With DID Host Override (for local testing)
```bash
npl verify \
  --audit audit.json \
  --sources ./npl-sources \
  --did-scheme http \
  --did-host-override localhost:8080
```

### JSON Output
```bash
npl verify --audit audit.json --sources ./npl-sources --json
```

Output:
```json
{
  "success": false,
  "errors": [
    {
      "step": "Signature",
      "message": "Entry 0: Signature verification failed"
    }
  ]
}
```

### Fail-Fast Mode
```bash
npl verify --audit audit.json --sources ./npl-sources --fail-fast
```

### Using YAML Configuration

You can also configure the verify command in `npl.yml`:

```yaml
verify:
  audit: ./audit.json
  sources: ./api/src/main
  didScheme: https
  didHostOverride: localhost:8080
  failFast: false
```

Then run:
```bash
npl verify
```
