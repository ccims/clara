# Configuration

The typical configuration for CLARA is in the YAML format.
Below, each available option is explained.
All options with a default value are optional.

!!! tip "Sensitive Information & Environment Variables"
    Sensitive information, like usernames and passwords, don't belong into configuration files!
    For that reason, each configuration option can be specified by an environment variable.
    Instead of the actual value, the BASH-like syntax `${NAME_OF_THE_ENV_VARIABLE}` can be used to specify the environment variable.

    Interpolation and specifying default values is possible as well, [just have a look here](https://github.com/sksamuel/hoplite?tab=readme-ov-file#built-in-preprocessors)!
    There you can also find other ways to set up the configuration to be effective, like referencing and substituting other parts of the configuration to make it more DRY.

---

## General configuration options

??? config-option "**_app.log-config_**"
    - Type: Boolean
    - Default: false
    - Description: Whether the whole configuration that CLARA will use should be logged at startup (at the info-level).

??? config-option "**_app.block-after-finish_**"
    - Type: Boolean
    - Default: false
    - Description: Whether the CLARA process should keep running after all exports are finished.
      This is useful when CLARA itself is running e.g. in Kubernetes and the Pod should not terminate because of the automatic restarts.

---

## Configuring the aggregation

### Platform: Kubernetes

??? config-option "**_aggregation.platforms.kubernetes.include-kube-namespaces_**"
    - Type: Boolean
    - Default: false
    - Description: Whether the namespaces with the `kube`-prefix should also get scanned by CLARA.
      Must be set to true when every namespace should be scanned, even when **_namespaces_** has the `*`-wildcard.

??? config-option "**_aggregation.platforms.kubernetes.namespaces_**"
    - Type: List of Strings
    - Default: empty List
    - Description: List all namespaces which CLARA should scan.
      To just scan all namespaces (except the `kube`-namespaces) set just the `*`-wildcard as the only element.
      The `*` needs to be in quotes.

#### Aggregator: Kubernetes API (optional)

??? config-option "**_aggregation.platforms.kubernetes.aggregators.kube-api.enable_**"
    - Type: Boolean
    - Default: true
    - Description: Simple way to disable this aggregator without removing all of its associated configuration.

#### Aggregator: DNS (optional)

??? config-option "**_aggregation.platforms.kubernetes.aggregators.dns.enable_**"
    - Type: Boolean
    - Default: true
    - Description: Simple way to disable this aggregator without removing all of its associated configuration.

??? config-option "**_aggregation.platforms.kubernetes.aggregators.dns.logs-since-time_**"
    - Type: String (formatted as RFC3339, like `2024-01-01T00:00:00Z`)
    - Default: empty String
    - Description: The DNS aggregator works by analyzing the logs of the Kubernetes DNS server which must be configured to log the queries.
      This option defines how recent the logs must be to be considered by CLARA.
      If this option is just an empty String (the default), all available logs will be used, which can lead to unwanted side effects, like old logs from a previous version of the deployment polluting the recovered architecture.

#### Aggregator: OpenTelemetry (optional)

??? config-option "**_aggregation.platforms.kubernetes.aggregators.open-telemetry.enable_**"
    - Type: Boolean
    - Default: true
    - Description: Simple way to disable this aggregator without removing all of its associated configuration.

??? config-option "**_aggregation.platforms.kubernetes.aggregators.open-telemetry.listen-port_**"
    - Type: Integer (must be a valid port number)
    - Description: The port CLARA will listen to incoming spans sent by an OpenTelemetry collector.

??? config-option "**_aggregation.platforms.kubernetes.aggregators.open-telemetry.listen-duration_**"
    - Type: String ([format here](https://github.com/sksamuel/hoplite?tab=readme-ov-file#duration-formats))
    - Description: Amount of time CLARA should listen to incoming spans sent by an OpenTelemetry collector.

---

## Configuring the merge

??? config-option "**_merge.comparison-strategy_**"
    - Type: String (one of `Equals`, `Prefix`, `Suffix`, `Contains`)
    - Description: Strategy for matching names of components aggregated by different aggregators.
      `Equals` needs the same names, `Prefix` and `Suffix` need to have matching strings on the start or the end respectively, `Contains` needs that one string is part of the other.

??? config-option "**_merge.show-messaging-communications-directly_**"
    - Type: Boolean
    - Description: If `true`, CLARA will define communications that go via a message broker directly between the components and removes the communications to the message broker. If `false` it show the communications via the message broker. 
---

## Configuring the filter

??? config-option "**_filter.remove-component-endpoints_**"
    - Type: Boolean
    - Default: false
    - Description: If `true`, the endpoints of the components are filtered out before the export, to improve visibility in complex architectures.

??? config-option "**_filter.remove-components-by-names_**"
    - Type: List of Strings
    - Default: empty List
    - Description: list of components that should be filtered out before the export (e.g. otel-collector-service).

---

## Configuring the export

??? config-option "**_export.on-empty_**"
    - Type: Boolean
    - Default: false
    - Description: If `true`, CLARA will export the recovered architecture using the enabled exporters, even if the architecture is completely empty.
      This could be useful for debugging purposes.

### Exporter: GraphViz (optional)

??? config-option "**_export.exporters.graphviz.enable_**"
    - Type: Boolean
    - Default: true
    - Description: Simple way to disable this exporter without removing all of its associated configuration.

??? config-option "**_export.exporters.graphviz.output-type_**"
    - Type: String (one of `BMP`, `DOT`, `GIF`, `JPG`, `JPEG`, `JSON`, `PDF`, `PNG`, `SVG`, `TIFF`)
    - Description: Output format of the export. `SVG` is known to work well and in most situations the best choice.

??? config-option "**_export.exporters.graphviz.output-file_**"
    - Type: String
    - Description: The file location (absolute or relative path) of the GraphViz output.
    - Example: `generated/architecture.svg`

### Exporter: Gropius (optional)

??? config-option "**_export.exporters.gropius.enable_**"
    - Type: Boolean
    - Default: true
    - Description: Simple way to disable this exporter without removing all of its associated configuration.

??? config-option "**_export.exporters.gropius.project-id_**"
    - Type: String
    - Description: The ID of the Gropius-project to export the recovered architecture to.

??? config-option "**_export.exporters.gropius.graphql-backend-url_**"
    - Type: String (a valid URL)
    - Description: The URL where CLARA can interact with the GraphQL-API of Gropius.

??? config-option-multi "**_export.exporters.gropius.graphql-backend-authentication_**"
    ??? config-option "**_export.exporters.gropius.graphql-backend-authentication.authentication-url_**"
        - Type: String (a valid URL)
        - Description: The URL where CLARA can obtain an authentication token from the Gropius-backend via username and password.
    ??? config-option "**_export.exporters.gropius.graphql-backend-authentication.username_**"
        - Type: String
        - Description: The username for obtaining an authentication token.
    ??? config-option "**_export.exporters.gropius.graphql-backend-authentication.password_**"
        - Type: String
        - Description: The password for obtaining an authentication token.
    ??? config-option "**_export.exporters.gropius.graphql-backend-authentication.client-id_**"
        - Type: String
        - Description: The OAuth client ID for obtaining an authentication token.

---

## A full example config

```yaml
app:
  log-config: true
  block-after-finish: false

aggregation:
  platforms:
    kubernetes:
      include-kube-namespaces: false
      namespaces:
        - abc
        - xyz
      aggregators:
        kube-api:
          enable: true
        dns:
          enable: true
          logs-since-time: 2024-02-01T00:00:00Z
        open-telemetry:
          enable: true
          listen-port: 7878
          listen-duration: 45 minutes

merge:
  comparison-strategy: Equals
  show-messaging-communications-directly: true

filter:
  remove-component-endpoints: false
  remove-components-by-names:
    - otel-collector-service

export:
  on-empty: false
  exporters:
    graphviz:
      enable: true
      output-type: SVG
      output-file: generated/architecture.svg
    gropius:
      enable: true
      project-id: aaaaaaaa-1111-bbbb-2222-cccccccccccc
      graphql-backend-url: http://my.backend.com:8080/graphql
      graphql-backend-authentication:
        authentication-url: http://my.backend.com:3000/authenticate/oauth/xxxxxxxx-1111-yyyy-2222-zzzzzzzzzzzz/token
        username: ${CLARA_GROPIUS_GRAPHQL_USERNAME}
        password: ${CLARA_GROPIUS_GRAPHQL_PASSWORD}
        client-id: ${CLARA_GROPIUS_GRAPHQL_CLIENT_ID}
```
