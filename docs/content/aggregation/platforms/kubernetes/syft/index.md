# SBOM

CLARA can utilize [anchore/syft](https://github.com/anchore/syft) to create SBOM files in SPDX format from the recovered components.
This is done to extract the dependencies and external libraries of the recovered architecture.

### Concept
In order to get the library information of each component, CLARA passes the recovered image and version tag to syft.
The syft binary then fetches the image from [docker-hub](https://hub.docker.com) and analyzes its contents and creates the SPDX files.
Lastly, the SPDX files for the components are read by CLARA and each library and version from is added to the respective component.

### Setup
Install the binary from [anchore/syft](https://github.com/anchore/syft) for your respective OS:  
macOS:
```sh
brew install syft
```
All OS:
```sh
curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sh -s -- -b /usr/local/bin 
```
For configuration options please see the [configurations page](../../../../configuration/index.md).