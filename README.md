# NPL CLI

The NPL CLI is a command line tool to support the development of projects written in NPL. It offers several useful for
commands for interacting with your NPL projects.

## Commands

## Supported Operating Systems and architectures

|         | ARM 64 | AMD 64 |
| ------- | ------ | ------ |
| MacOS   | Yes    | Yes    |
| Windows | Beta   | No     |
| Linux   | Yes    | Yes    |

## Old CLI

The older CLI was intended for use with Noumena Cloud. It is no longer supported and has been replaced by the new CLI.
The old CLI is still available under the GitHub releases, and is documented in the [old CLI README](OLD-CLI-README.md).

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
