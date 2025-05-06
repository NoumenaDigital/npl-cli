# NPL CLI

The NPL CLI is a command line tool to support the development of projects written in Noumena Protocol Language (NPL). It
offers several useful commands for interacting with your NPL projects.

## Commands

- `npl version` - Displays the current version of the NPL CLI.
- `npl help` - Displays help information for the NPL CLI.
- `npl check` - Checks the NPL for compilation errors and warnings. An optional path to a directory can be provided as
  an argument. If no path is provided, the current working directory is used.
- `npl test` - Run the NPL tests. An optional path to a directory can be provided as an argument. If no path is
  provided, the current working directory is used.
- `npl openapi` - Generates the openapi specs for NPL protocols. An optional path to a directory can be provided as an
  argument. If no path is provided, the current working directory is used.
- `npl deploy` - Deploys NPL sources to a configured Noumena Engine target. [See details](#deploy-command).

## Supported Operating Systems and architectures

|       | ARM 64 | AMD 64 |
| ----- | ------ | ------ |
| MacOS | Yes    | Yes    |
| Linux | Yes    | Yes    |

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

The `deploy` command allows you to deploy NPL sources to a Noumena Engine instance.

### Usage

```bash
npl deploy --target <target> --sourceDir <directory> [--clear]
```

Where:

- `[target]` (required, unless `defaultTarget` is present in the config) is the named target from the configuration file
- `[directory]` (required) is the path to the directory containing NPL sources
- `[--clear]` (optional) clears the application contents before deployment

### Configuration

The deploy command requires configuration settings that are read from a YAML file. The CLI looks for configuration in
the following locations (in order):

1. `./.npl/deploy.yml` (current directory)
2. `~/.npl/deploy.yml` (user's home directory)

The configuration file contains multiple named deployment targets, allowing you to quickly switch between different
environments (dev, test, prod, etc.) without changing the command.

#### Configuration Schema (YAML)

```yaml
schemaVersion: v1
defaultTarget: target-name # Optional
targets:
  target-name:
    type: engine # Currently only 'engine' type is supported
    engineManagementUrl: http://server:port
    authUrl: http://auth-server:port/realms/your-realm # Include the realm in the URL
    username: your-username
    password: your-password
    clientId: client-id
    clientSecret: client-secret
  another-target:
    # Another target configuration
    ...
```

#### Properties for Each Target

| Property              | Description                              | Default Value                             |
| --------------------- | ---------------------------------------- | ----------------------------------------- |
| `engineManagementUrl` | URL of the Noumena Engine Management API | `"http://localhost:12400/realms/noumena"` |
| `authUrl`             | URL of the authentication endpoint       | `"http://localhost:11000"`                |
| `username`            | Username for authentication              | (Required)                                |
| `password`            | Password for authentication              | (Required)                                |
| `clientId`            | Client ID for authentication             | `"foo"`                                   |
| `clientSecret`        | Client secret for authentication         | `"bar"`                                   |

#### Example Configuration

A sample configuration file is available at `
