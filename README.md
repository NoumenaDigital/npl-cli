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
