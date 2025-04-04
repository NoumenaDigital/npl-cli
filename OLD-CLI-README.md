# CLI (old version, for cloud deployment only)

The old NPL CLI is a command line tool to support the development of projects written in NPL. It allows you to interact
with NOUMENA Cloud from your terminal.

## Main functionality

### Noumena Cloud

NPL CLI serves as a Noumena Cloud API wrapper

- List organisations
- List applications
- Create an application
- Deploy NPL to an application
- Get the detail of an application

### Environment

The CLI offers two login options:

1. Login with email and password
2. Login with a microsoft account

#### Email and password environment variables

This configuration is selected by setting `NC_ENV=DEV`

Environment variables:

- `NC_BASE_URL`: The base URL of the Noumena Cloud API (default: https://portal.noumena.cloud)
- `NC_EMAIL`: The email address of the user
- `NC_PASSWORD`: The password of the user

#### Microsoft account environment variables

This configuration is selected by setting `NC_ENV=PROD`. To authenticate with a microsoft account, use the
[azure CLI](https://learn.microsoft.com/en-us/cli/azure/):

`az login`

Environment variables:

- `NC_BASE_URL`: The base URL of the Noumena Cloud API (default: https://portal.noumena.cloud)
- `NC_EMAIL`: The email address of the user

### API

`Usage: npl <command> <subcommand> <args>`

#### Commands

| Command | Description                        |
| ------- | ---------------------------------- |
| org     | Manage Noumena Cloud Organizations |
| app     | Manage NPL Applications            |

#### Org subcommands

```
Usage: npl org <subcommand> <args>
```

| Subcommand | Description                      |
| ---------- | -------------------------------- |
| list       | List Noumena Cloud organizations |

#### App subcommands

```
Usage: npl app <subcommand> <args>
```

| Subcommand | Description                               |
| ---------- | ----------------------------------------- |
| create     | Create Application in Noumena Cloud       |
| deploy     | Deploy NPL application to Noumena Cloud   |
| clear      | Clear NPL application packages            |
| delete     | Delete NPL application from Noumena Cloud |
| detail     | Get application details                   |
| secrets    | Get application details                   |
| list       | List NPL applications                     |

## Supported Operating Systems and architectures

|         | ARM 64 | AMD 64 |
| ------- | ------ | ------ |
| Windows | x      | x      |
| MacOS   | x      | x      |
| Linux   | x      | x      |
