---
weight: 20
---

# NPL CLI

The NPL CLI is a command line tool to support the development of projects written in Noumena Protocol Language (NPL). It
offers several useful commands for interacting with your NPL projects.

## How to install

=== "Homebrew (MacOS)"

    Simply run:

    ``` shell
    brew install NoumenaDigital/tools/npl
    ```

    !!! note
        If you have previously installed the NPL CLI using the `curl` command, you may need to remove the old version before installing the new one. You can do this by running `rm -rf ~/.npl` and then reinstalling the new version with brew.

=== "binary (MacOS/Linux)"

    Download and install the NPL CLI using the following command:

    ``` shell
    curl -s https://documentation.noumenadigital.com/get-npl-cli.sh | bash
    ```

    You may have to restart the terminal to ensure the CLI is available in your PATH after installing the NPL CLI.

    Alternatively, download the latest release [here](https://github.com/NoumenaDigital/npl-cli/releases) and add it to your $PATH.

=== "binary (Windows with WSL)"

    Download and install the NPL CLI using the following command:

    ``` shell
    curl -s https://documentation.noumenadigital.com/get-npl-cli.sh | bash
    ```

    You will need to manually add the `npl` executable to your PATH. The script above will not do that for you.

=== "Windows without WSL"

    Download and install the latest `.exe` executable [here](https://github.com/NoumenaDigital/npl-cli/releases).

## Commands

To see a description of how to use each command, run `npl help`

| Command                     | Description                                                                                   |
| --------------------------- | --------------------------------------------------------------------------------------------- |
| `npl version`               | Displays the current version of the NPL CLI                                                   |
| `npl help`                  | Displays help information for the NPL CLI                                                     |
| `npl init`                  | Initializes a new project                                                                     |
| `npl check`                 | Checks the NPL for compilation errors and warnings                                            |
| `npl test`                  | Runs the NPL tests                                                                            |
| `npl puml`                  | Generates a puml diagram from NPL source                                                      |
| `npl openapi`               | Generates the openapi specs for NPL protocols                                                 |
| `npl deploy`                | Deploys NPL sources to a configured NOUMENA Engine target                                     |
| `npl cloud help`            | Displays help information for the NPL CLI cloud commands                                      |
| `npl cloud login`           | Handles the NPL CLI login to NOUMENA Сloud                                                    |
| `npl cloud logout`          | Handles the NPL CLI logout from NOUMENA Cloud                                                 |
| `npl cloud deploy npl`      | Deploys NPL sources to a NOUMENA Cloud Application                                            |
| `npl cloud deploy frontend` | Deploys frontend build sources to a NOUMENA Cloud Application                                 |
| `npl cloud clear`           | Deletes NPL sources and clears protocols from the database from the NOUMENA Cloud Application |

## Configuration file

The NPL CLI uses a per-project configuration file named `npl.yml` to store project-specific settings. Below is an
example configuration file:

```yaml
runtime: # Configuration for the Noumena Platform runtime
  version: 2025.2.2 # Specify the Noumena Platform runtime version

cloud: # Configuration for NOUMENA Cloud deployment
  tenant: my-tenant # Slug of the NOUMENA Cloud tenant
  app: my-npl-app # Slug of the NOUMENA Cloud application
  clear: false # Whether to clear existing protocols and data before deployment

local: # Configuration for deploying to a local NOUMENA Engine instance
  managementUrl: http://localhost:12400 # URL of the local NOUMENA Engine management API
  authUrl: http://localhost:11000 # URL of IAM service for authentication
  username: my-user # Username for authentication
  password: my-pass # Password for authentication
  clientId: npl-cli # Client ID for authentication
  clientSecret: secret # Client secret for authentication

structure: # Configuration for project structure
  sourceDir: src/main/npl # Directory containing NPL source files
  testSourceDir: src/test/npl # Directory containing NPL test source files
  outputDir: output/ # Directory for generated files (e.g., OpenAPI specs, PUML diagrams)
  frontend: frontend/output # Directory containing frontend build files (must contain an index.html file)
  migration: src/main/migration.yml # Migration file – required for cloud deployments
  rules: src/main/rules/rules.yml # Rules file for party automation (used by openapi command if present)
  coverage: true # Whether to generate code coverage reports when running tests
```

## Supported Operating Systems and architectures

|         | ARM 64 | AMD 64 |
| ------- | ------ | ------ |
| MacOS   | Yes    | Yes    |
| Linux   | Yes    | Yes    |
| Windows | Yes    | Yes    |

## Model Context Protocol (MCP) server

Once you've installed the CLI, you can use it in MCP mode with your local AI tools.

### VS Code and Cursor integration

Follow one of the methods below to install NPL CLI as an MCP server in Cursor or VS Code:

- [![Install MCP Server](https://cursor.com/deeplink/mcp-install-dark.svg)](cursor://anysphere.cursor-deeplink/mcp/install?name=npl-cli&config=ewogICAgImNvbW1hbmQiOiAibnBsIiwKICAgICJhcmdzIjogWyJtY3AiXQp9Cg==)

- [Install in VS Code](vscode:mcp/install?%7B%22name%22%3A%22NPL%20CLI%22%2C%22command%22%3A%22npl%22%2C%22args%22%3A%5B%22mcp%22%5D%7D)
  (select `Install server`)

Or simply add `npl-cli` to your MCP configuration file, e.g.

```json
{
  "mcpServers": {
    "npl-cli": {
      "name": "NPL CLI",
      "command": "npl",
      "args": ["mcp"]
    }
  }
}
```

### Claude Code integration

Type the following in the Claude Code terminal to add NPL CLI as an MCP server:

```shell
claude mcp add npl -- npl mcp
```

## Service Accounts

The NPL CLI supports the use of service accounts for authentication and authorization when interacting with NOUMENA
Cloud. Service accounts are special accounts that belong to your application, instead of to an individual end user. They
are used for machine-to-machine communication.

To create a secret key for a service account, log in to your NOUMENA Cloud account and navigate to the "Settings" page
of your tenant. You will find the "Service Accounts" section where you can generate a secret key. Note that this option
is only available to the tenant's owner(s).

The NPL CLI checks if the `NPL_SERVICE_ACCOUNT_CLIENT_SECRET` environment variable is set. If it is, the CLI uses it for
the deployment, otherwise it will fall back to the user authentication and prompt for login.
