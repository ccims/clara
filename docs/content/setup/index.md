# Setup instructions for CLARA step-by-step

These instructions will walk you through the initial installation and setup of the CLARA project. A deployed instance of the Gropius project as well as access to a kubernetes cluster are required.

---

## 1. Prerequisites
### 1.1. getting CLARA
- clone the [CLARA repository](https://github.com/SteveBinary/clara)

    ```sh
    git clone https://github.com/SteveBinary/clara.git
    ```
    or
    ```sh
    git clone git@github.com:SteveBinary/clara.git 
    ```
!!! warning "Java Installation"
    Make sure you have at least a Java 17 JVM installed and configured on your machine.

### 1.2. kube-api

- Ensure you have administrative rights for your Kubernetes cluster.
- Ensure you have configured the target namespace you want to analyze, in the context of your local kube-config.

### 1.3. Install ktunnel:

[ktunnel](https://ktunnel.readthedocs.io/en/stable/) allows CLARA to stream data from inside the cluster to the outside, thus not needing to be deployed inside the cluster.

Either use homebrew:
```sh
brew tap omrikiei/ktunnel && brew install omrikiei/ktunnel/ktunnel
```
or fetch the binaries from the [release page](https://github.com/omrikiei/ktunnel/releases).

## 2. Aggregator Setup and Configuration
CLARA relies on different aggregation components, that each need individual preparation. Although each aggregator is not mandatory, it is recommended to go through the setup of all following aggregators.

### 2.1 OpenTelemetry auto-instrumentation
CLARA utilizes the [opentelemetry auto-instrumentation](https://opentelemetry.io/docs/kubernetes/operator/automatic/#) to add spans to the cluster's communication.

- Check if [cert manager](https://cert-manager.io/) is installed in the cluster. If not run:
    ```sh
    kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml
    ```
- Install the OpenTelemetry operator into the cluster:
    ```sh
    kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
    ```
- Install the OpenTelemetry collector into the target namespace:
    ```sh
    kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/configmap.yml
    kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/deployment.yml	
    ```
- Add the instrumentation object into the target namespace:
    ```sh
    kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/autoinstrumentation.yml
    ```
- Instrument all existing deployments (that use below listed frameworks/technology) in the target namespace by configuring the `<your-namespace>.yaml`:
    ```yml
    metadata:
      annotations:
        instrumentation.opentelemetry.io/inject-java: "true"
        instrumentation.opentelemetry.io/inject-dotnet: "true" 
        instrumentation.opentelemetry.io/inject-go: "true" 
        instrumentation.opentelemetry.io/inject-nodejs: "true" 
        instrumentation.opentelemetry.io/inject-python: "true" 
    ```

### 2.2. CoreDNS
Ensure you can access and if necessary configure the **kube-dns** in the **kube-system** namespace.
When using a managed cluster from a service provider, changes to core components of Kubernetes might not be allowed directly.
Please consult the documentation of your respective provider.
    
- Ensure you can see the logs of your kube-dns component and it logs DNS requests by running:
    ```sh
    kubectl logs -l k8s-app=kube-dns -n kube-system
    ```
- Ensure you see logs of this format:
    ```txt
    [INFO] 10.244.0.19:35065 - 3179 "A IN kubernetes.default.svc.cluster.local.svc.cluster.local. udp 72 false 512" NXDOMAIN qr,aa,rd 165 0.0000838s
    ```
- If you don't see such logs, configure your kube-dns accordingly, based on your service-provider.

### 2.3. Install anchore/syft
CLARA uses syft to generate SBOMs from container images.

- Install the binary from [anchore/syft](https://github.com/anchore/syft) for your respective OS:  
macOS:
    ```sh
    brew install syft
    ```
    All OS:
    ```sh
    curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sh -s -- -b /usr/local/bin 
    ```

## 3. Run CLARA
- CLARA comes with a default config, yet it is recommended to check the configuration options and adjust them to your needs.
   It is also recommended to make some dry runs with stripped down, minimal config to see if the configuration works properly.
   For configuration options see: [configurations page](../configuration/index.md).  
   The config file of CLARA can be found at `<path-to-clara>/clara-app/src/main/resources/config.yml`
- In this `config.yml` please insert your specific URLs and authorization information for accessing your deployed Gropius instance.
   Sensitive credentials are prepared to be set as environment variables.
- To build CLARA run in the clara dictionary:
    ```sh
    ./gradlew clean build standaloneJar
    ```
- Start CLARA by executing the application:
    ```sh
    java -jar clara-app/build/libs/clara-app-*.jar #TODO ENV VARS
    ```
- Run ktunnel: 
    ```sh
    ktunnel inject deployment otel-collector-deployment 7878 -n <your-namespace>
    ```
