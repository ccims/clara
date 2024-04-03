# Filtering

Filtering is applied as third step in the data pipeline, see [concept](../concept/index.md).
Filters can be added/removed by plug-and-play. For details see the [configurations](../configuration/index.md) page.

## Filtering options 
- Remove Component Endpoints: The attribute "Endpoints" is filtered out (in order to make the [SVG export](../export/svg/index.md) more visually appealing). 
- Remove Components by Name: Components can be filtered out if they are not directly part of the application (e.g. the OpenTelemetry Collector).