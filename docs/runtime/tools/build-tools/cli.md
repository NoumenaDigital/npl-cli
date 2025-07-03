---
weight: 20
---

# NPL CLI

The NPL CLI is a command line tool to support the development of projects written in Noumena Protocol Language (NPL). It
offers several useful commands for interacting with your NPL projects.

## Commands

To see a description of how to use each command, run `npl help`

| Command            | Description                                                                                   |
| ------------------ | --------------------------------------------------------------------------------------------- |
| `npl version`      | Displays the current version of the NPL CLI                                                   |
| `npl help`         | Displays help information for the NPL CLI                                                     |
| `npl check`        | Checks the NPL for compilation errors and warnings                                            |
| `npl test`         | Runs the NPL tests                                                                            |
| `npl puml`         | Generates a puml diagram from NPL source                                                      |
| `npl openapi`      | Generates the openapi specs for NPL protocols                                                 |
| `npl deploy`       | Deploys NPL sources to a configured NOUMENA Engine target                                     |
| `npl cloud help`   | Displays help information for the NPL CLI cloud commands                                      |
| `npl cloud login`  | Handles the NPL CLI login to NOUMENA Сloud                                                    |
| `npl cloud logout` | Handles the NPL CLI logout from NOUMENA Cloud                                                 |
| `npl cloud deploy` | Deploys NPL sources to a NOUMENA Cloud Application                                            |
| `npl cloud clear`  | Deletes NPL sources and clears protocols from the database from the NOUMENA Cloud Application |

## Supported Operating Systems and architectures

|         | ARM 64 | AMD 64 |
| ------- | ------ | ------ |
| MacOS   | Yes    | Yes    |
| Linux   | Yes    | Yes    |
| Windows | Yes    | Yes    |

## How to install

For MacOS, Linux, and Windows running WSL, you can install the NPL CLI using

```shell
curl -s https://documentation.noumenadigital.com/get-npl-cli.sh | bash
```

or download the latest release [here](https://github.com/NoumenaDigital/npl-cli/releases) and add it to your $PATH. On
Windows running WSL, you will need to manually add the `npl` executable to your PATH. The script above will not do that
for you.

For Windows without WSL, you may download the latest `.exe` executable
[here](https://github.com/NoumenaDigital/npl-cli/releases)
