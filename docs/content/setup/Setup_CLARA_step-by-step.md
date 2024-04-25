Setup CLARA step-by-step

0. CLARA
    1. clone the [CLARA-repo](https://github.com/SteveBinary/clara)
        ```sh
        git clone https://github.com/SteveBinary/clara.git
        ```
       or
        ```sh
        git clone git@github.com:SteveBinary/clara.git 
        ```
    2. Make sure you have at least a Java 17 JVM as well as the Kotlin compiler installed and configured on your machine.
1. kube-api

    1. Ensure you have administrative rights for your Kubernetes cluster.
    2. Ensure you have configured your target namespace in the context of your local kube-config.

2. OpenTelemetry auto-instrumentation [(in case of questions see official docs)](https://opentelemetry.io/docs/kubernetes/operator/automatic/#)
    1. Check if cert manager is installed in the cluster. If not run:
        ```sh
        kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml
        ```
    2. Install the OpenTelemetry operator into the cluster:
        ```sh
        kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
        ```
    3. Install the OpenTelemetry Collector into the target namespace:
        ```sh
        kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/configmap.yml
        kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/deployment.yml	
        ```
    4. Add the instrumentation object into the target namespace:
        ```sh
        kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/autoinstrumentation.yml
        ```
    5. Instrument all existing deployments in the namespace by configuring the `<your-namespace>.yaml:
        ```yml
        metadata:
          annotations:
            instrumentation.opentelemetry.io/inject-java: "true"
            instrumentation.opentelemetry.io/inject-dotnet: "true" 
            instrumentation.opentelemetry.io/inject-go: "true" 
            instrumentation.opentelemetry.io/inject-nodejs: "true" 
            instrumentation.opentelemetry.io/inject-python: "true" 
        ```
    6. Install ktunnel:

       Either use homebrew:
        ```sh
        brew tap omrikiei/ktunnel && brew install omrikiei/ktunnel/ktunnel
        ```
       or fetch the binaries from the [release page](https://github.com/omrikiei/ktunnel/releases).

3. CoreDNS
    1. Ensure you can access and if necessary configure the **kube-dns** in the **kube-system** namespace.
       When using a managed cluster from a service provider, changes to core components of Kubernetes might be not allowed directly.
       Please consult the documentation of your respective provider.
    2. Ensure you can see the logs of your kube-dns component and it logs DNS requests by running:
         ```sh
         kubectl logs -l k8s-app=kube-dns -n kube-system
         ```
    3. Ensure you see logs of the format:
       ```txt
       [INFO] 10.244.0.19:35065 - 3179 "A IN kubernetes.default.svc.cluster.local.svc.cluster.local. udp 72 false 512" NXDOMAIN qr,aa,rd 165 0.0000838s
       ```
    4. If you don't see such logs configure your kube-dns accordingly based on your service-provider.

4. Install anchore/syft
    1. Install the binary from [anchore/syft](https://github.com/anchore/syft) for your respective OS:
        ```
        macOS: brew install syft
        All OS: curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sh -s -- -b /usr/local/bin 
        ```
5. configure CLARA
    1. CLARA comes with a default config, yet it is recommended to check the configuration options and adjust them to your needs.
       It is also recommended to make some dry runs with little configs to see if the configuration works properly.
       For configuration options see: [configurations page](../configuration/index.md).
       The config file of CLARA can be found at `<path-to-clara>/clara-app/src/main/resources/config.yml`
    3. In the config.yml please insert your specific URLs and authorization information for accessing Gropius.
    2. To build CLARA run in the clara dictionary:
        ```sh
        ./gradlew clean build standaloneJar
        ```
    3. Start CLARA by executing the Kotlin application:
        ```sh
        java -jar clara-app/build/libs/clara-app-*.jar #TODO ENV VARS
        ```
    4. Run ktunnel: 
        ```sh
        ktunnel inject deployment otel-collector-deployment 7878 -n <your-namespace>
        ```