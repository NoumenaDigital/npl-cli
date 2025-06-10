# Platform documentation

The following is a brief overview of the most important things to keep in mind when contributing to the platform
information. Please refer to the [documentation repo README](https://github.com/NoumenaDigital/documentation#readme) for
more details.

## Development

### Build and view

First, make sure you are authenticated with the GitHub container repository (`docker login ghcr.io`).

Then, run:

```bash
docker compose up --build
```

or

```bash
make serve
```

Note that you will have to `ctrl-c` and run the command again if you want to see changes to the documentation.

### Verify links

To verify that the html doesn't contain dead links (or other common mistakes), run

```shell
make htmltest
```

If certain links result in failure despite being correct in their context, they can be ignored by specifying
[`data-proofer-ignore`](https://github.com/wjdp/htmltest#see_no_evil-ignoring-content) in the link's
[attribute list](https://python-markdown.github.io/extensions/attr_list/), e.g.:

```markdown
[very secret stuff](https://vpnprotected.noumenadigital.com){ data-proofer-ignore }
```

### Work from the top level directory

Note that you do not need to be in this module's directory to run the commands. If you are in the top-level directory,
you can run:

```bash
make -C docs serve
```

or

```bash
docker compose -f docs/docker-compose.yml up --build
```

### Documentation restructure

Refer to https://github.com/NoumenaDigital/documentation/blob/master/README.md#documentation-restructure for details.

## How to work with docs

This directory provides documentation for our website (https://documentation.noumenadigital.com/). When the repository
which you are in right now gets merged to master, the `documentation` repo will pull it in as a submodule and copy over
the markdown contents found here.

Please refer to the [documentation repo README](https://github.com/NoumenaDigital/documentation#readme) for more
details.
