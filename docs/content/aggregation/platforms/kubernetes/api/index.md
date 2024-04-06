# API

CLARA utilizes the Kubernetes API to retrieve basic information about the pods, services and deployments running in the cluster.

The [fabric8 Kubernetes client](https://mvnrepository.com/artifact/io.fabric8/kubernetes-client) is used by CLARA to communicate to the Kubernetes API.

### Concept
The Kubernetes API is queried for pods and services from the configured namespace (see [configurations](../../../../configuration/index.md)).  
Pods are then matched to a service and all services with their respective pods and all unmatched pods are provided into the datapipeline.