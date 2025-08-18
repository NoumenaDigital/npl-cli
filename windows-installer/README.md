# NPL CLI Windows Installer

## Directory Structure

- **build** Directory containing files used during the compilation process of the installer artifact. This directory
  must be created as explained below.

- **dist** Destination directory for all produced artifacts (e.g., the final installer executable). This directory will
  be created during the build process.

---

## Building the Installer

To build the installer, follow the steps listed below. All steps are written in relation to the current directory
`windows-installer``:

1. Download [EnVar plugin](https://nsis.sourceforge.io/mediawiki/images/7/7f/EnVar_plugin.zip), place it the current
   directory and expand

   ```bash
      curl -LO https://nsis.sourceforge.io/mediawiki/images/7/7f/EnVar_plugin.zip
      unzip -d EnVar_plugin EnVar_plugin.zip
   ```

2. Create `build` sub-directory and place the following artifacts there.

   - Copy `License.md` from npl-cli root directory
     ```bash
        mkdir build
        cp ../LICENSE.md build
     ```
   - Download the `npl-cli` windows executable from the
     [releases page](https://github.com/NoumenaDigital/npl-cli/releases/latest). Replace `{VERSION}` with your desired
     version number. Do not rename, the result should be `build/npl-cli-windows-x86_64-{VERSION}.exe`
     ```bash
        export VERSION=2025.1.10
        curl -L --output-dir build -O https://github.com/NoumenaDigital/npl-cli/releases/download/$VERSION/npl-cli-windows-x86_64-$VERSION.exe
     ```

3. Run the following command to build the installer using the NSIS docker. Replace `2025.1.10` with the desired version
   of the artifact:
   ```bash
      export VERSION=2025.1.10
      docker run \
       -v .:/npl-cli-installer \
       -v ./EnVar_plugin/Plugins:/usr/share/nsis/Plugins \
       hp41/nsis:3.01-1 \
       -DVERSION=$VERSION /npl-cli-installer/npl-cli.nsi
   ```
4. Upon a successful build, The resulting installer will be generated as: `dist/npl-cli-installer.exe`
