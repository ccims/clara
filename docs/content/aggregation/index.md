# Aggregation

For aggregating data from the cluster four different data sources are used in CLARA:

- [Kubernetes API](./platforms/kubernetes/api/index.md)
- [Kubernetes DNS-Server](./platforms/kubernetes/dns/index.md)
- [OpenTelemetry Traces](./platforms/kubernetes/opentelemetry/index.md)
- [anchore/syft](./platforms/kubernetes/syft/index.md)

Aggregations can be enabled/disabled, yet it is recommended to always use all available aggregators to get a holistic view of the examined architecture. For details see [configurations](../configuration/index.md).