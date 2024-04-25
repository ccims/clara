Setup CLARA step-by-step

1. Auto Instrumentation [(in case of questions see official docs)](https://opentelemetry.io/docs/kubernetes/operator/automatic/#)
    
    1.1 Check if cert manager is installed in the cluster. If not run:
	```sh
	kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml
	```

    1.2 Install the OpenTelemetry operator into the cluster:
	```sh
	kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
	```

    1.3 Install the OpenTelemetry Collector into the target namespace:
	```sh
	kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/configmap.yml
	kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/deployment.yml	
	```

    1.4 Add the instrumentation object into the target namespace:
	```sh
	kubectl apply -f <path-to-clara>/deployment/open-telemetry-collector/autoinstrumentation.yml
	```
    
    1.5 Instrument all existing deployments in the namespace by configuring the namespace.yaml: 
	```yml
	metadata:
	  annotations:
	  	instrumentation.opentelemetry.io/inject-java: "true"
	  	instrumentation.opentelemetry.io/inject-dotnet: "true" 
	  	instrumentation.opentelemetry.io/inject-go: "true" 
	  	instrumentation.opentelemetry.io/inject-nodejs: "true" 
	  	instrumentation.opentelemetry.io/inject-python: "true" 
	```

2. kube-api

3. CoreDNS
