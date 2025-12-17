# Change Log

All notable changes to the NPL CLI will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [2025.2.7]

### Added

- `--frontend` flag to `npl init` command for initializing frontend project templates.

## [2025.2.4]

### Added

- The option to use the npl.yaml configuration file, as documented
  [here](docs/runtime/tools/build-tools/cli.md#configuration-file).
- Windows installer.

### Removed

- Support for the "target" configuration files that were placed in either `~/.npl/` or `.npl/` directories.

## [2025.2.1]

### Added

- Service account support.
- Support for the simplified party model.

### Removed

- Support for the old party model.

## [2025.1.10]

### Added

- `npl cloud deploy frontend` command.

## [2025.1.8]

### Added

- MCP server.
- NPL init command.

## [2025.1.5]

Initial "new" CLI, with the following commands:

- help
- version
- check
- test
- openapi
- deploy

### Removed

All of the features of the old CLI (all related to cloud deployment).
