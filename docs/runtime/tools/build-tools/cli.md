---
weight: 20
---

# NPL CLI

The [NPL CLI](https://github.com/NoumenaDigital/npl-cli) is a command line tool to support the development of projects
written in Noumena Protocol Language (NPL). It offers several useful commands for interacting with your NPL projects.

## Commands

To see a description of how to use each command, run `npl help`

| Command            | Description                                                                                                                                 |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `npl version`      | Displays the current version of the NPL CLI                                                                                                 |
| `npl help`         | Displays help information for the NPL CLI                                                                                                   |
| `npl check`        | Checks the NPL for compilation errors and warnings                                                                                          |
| `npl test`         | Runs the NPL tests                                                                                                                          |
| `npl puml`         | Generates a puml diagram from NPL source                                                                                                    |
| `npl openapi`      | Generates the openapi specs for NPL protocols                                                                                               |
| `npl deploy`       | Deploys NPL sources to a configured NOUMENA Engine target                                                                                   |
| `npl cloud help`   | Displays help information for the NPL CLI cloud commands                                                                                    |
| `npl cloud login`  | Handles the login to NOUMENA cloud                                                                                                          |
| `npl cloud logout` | Handles the NPL CLI logout from NOUMENA cloud                                                                                               |
| `npl cloud deploy` | Deploys NPL sources to a NOUMENA cloud                                                                                                      |
| `npl cloud clear`  | Deletes all source files and resets the application’s current state — including variables, temporary data, and any objects currently in use |

## Supported Operating Systems and architectures

|         | ARM 64 | AMD 64 |
| ------- | ------ | ------ |
| MacOS   | Yes    | Yes    |
| Linux   | Yes    | Yes    |
| Windows | Yes    | Yes    |
