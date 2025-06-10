---
weight: 20
---

# NPL Command Line Interface (CLI)

The NPL CLI is a command line tool to support the development of projects written in Noumena Protocol Language (NPL). It
offers several useful commands for interacting with your NPL projects.

## Commands

To see a description of how to use each command, run `npl help`

| Command       | Description                                                                               |
| ------------- | ----------------------------------------------------------------------------------------- |
| `npl version` | Displays the current version of the NPL CLI                                               |
| `npl help`    | Displays help information for the NPL CLI                                                 |
| `npl check`   | Checks the NPL for compilation errors and warnings                                        |
| `npl test`    | Runs the NPL tests                                                                        |
| `npl puml`    | Generates a puml diagram from NPL source                                                  |
| `npl openapi` | Generates the openapi specs for NPL protocols                                             |
| `npl deploy`  | Deploys NPL sources to a configured Noumena Engine target. [See details](#deploy-command) |

## Supported Operating Systems and architectures

|       | ARM 64 | AMD 64 |
| ----- | ------ | ------ |
| MacOS | Yes    | Yes    |
| Linux | Yes    | Yes    |

## How to use?

You can find more examples and details about the commands in our tracks: follow the
[link](/tracks/developing-NPL-local.md)
