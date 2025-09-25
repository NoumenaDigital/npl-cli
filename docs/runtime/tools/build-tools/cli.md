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

## Supported Operating Systems and architectures

|         | ARM 64 | AMD 64 |
| ------- | ------ | ------ |
| MacOS   | Yes    | Yes    |
| Linux   | Yes    | Yes    |
| Windows | Yes    | Yes    |

## Model Context Protocol (MCP) server

Once you've installed the CLI, you can use it in MCP mode with your local AI tools.

[![Install MCP Server](https://cursor.com/deeplink/mcp-install-dark.svg)](cursor://anysphere.cursor-deeplink/mcp/install?name=npl-cli&config=ewogICAgImNvbW1hbmQiOiAibnBsIiwKICAgICJhcmdzIjogWyJtY3AiXQp9Cg==)

[Install in VS Code](vscode:mcp/install?%7B%22name%22%3A%22NPL%20CLI%22%2C%22command%22%3A%22npl%22%2C%22args%22%3A%5B%22mcp%22%5D%7D)

Simply add `npl-cli` to your MCP configuration file, e.g.

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

## Service Accounts

The NPL CLI supports the use of service accounts for authentication and authorization when interacting with NOUMENA
Cloud. Service accounts are special accounts that belong to your application or a virtual machine (VM), instead of to an
individual end user. They are used for machine-to-machine communication.

To create a secret key for a service account, login to your NOUMENA Cloud account, navigate to the "Settings" page of
_your tenant_. You will find the "Service Accounts" section where you can generate a secret key. Note that this option
is only available to the tenant's owner(s).

The NPL CLI will check if the `NPL_SERVICE_ACCOUNT_CLIENT_SECRET` environment variable is set. If it is, the CLI will
use it for the deployment, otherwise it will fall back to the user authentication and prompt for login.
