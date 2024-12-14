# Evaluation against T2-Reference-Architecture

CLARA has been evaluated against the [T2-Project Reference-Architecture (Microservices)](https://t2-documentation.readthedocs.io/en/latest/index.html).

Follow this guideline step-by-step to recreate the evaluation of CLARA using the T2-Project, locally on a [minikube](https://minikube.sigs.k8s.io/docs/) cluster.


## Step-By-Step Setup and Execution Instructions
The setup consists of the Gropius setup, the minikube setup, the T2-Project setup, and the CLARA setup.

## 1. Gropius Setup
### 1.1 Getting Gropius

- Clone the [Gropius repository](https://github.com/ccims/gropius) recursive with all submodules.

    ```sh
    git clone --recurse-submodules https://github.com/ccims/gropius.git
    ```
  or
    ```sh
    git clone --recurse-submodules git@github.com:ccims/gropius.git
    ```
!!! warning "Docker Installation"
    Make sure you have a local container environment (e.g. Docker) installed and configured on your machine.

- Locally deploy the Gropius testing environment using docker-compose:
    ```sh
    docker-compose -f docker-compose-testing.yaml up -d
    ```
- Check availability by visiting http://localhost:4200.

### 1.2 Creating Gropius OAuth Client
- To authenticate against Gropius you need an OAuth2 client. You can create one in the Gropius UI.
- Open Gropius in your browser under http://localhost:4200 and log in as the default admin (admin/admin).
- Click on the tab `Admin` in the top menu, then select `OAuth2` on the left menu.
- On the right menu hit the `+` to create a new OAuth2 client.
- In the opening dialog enter the following:
    * Name: `CLARA`
    * Redirect URLs: `http://localhost:7878`
    * Client credential flow user: type 'admin' and select the `Admin` account.
    * Check `requires secret`.
    * Check `is valid`.
    * Hit `Create auth client`.
- Now, you should see an entry named `CLARA` in the list.
  - On the right, click the `ID`-icon and copy the client-id and store it where you find it again.
  - On the right, click the key-symbol and create a new secret access key. Copy it **immediately** as you won't see it again and store it next to the client-id. 

### 1.3 Create Gropius Project
- Create a new Project in Gropius by again opening the Gropius UI under http://localhost:4200 and logging in as the default admin (admin/admin). 
- Click on the tab `Projects` in the top menu.
- On the right menu hit the `+` to create a Project:
    * Enter any name that suites you.
    * Enter any description that suites you. 
    * For repository URL simply enter: `https://example.org`
    * Hit `create project`.
- Click on the newly created project in the list and copy the project's UUID from the URL and store it where you find it again.

### 1.4 Import Gropius Default Templates
- The Gropius metamodel is ontological and works with templates.
- To install the templates necessary for CLARA, you need to import them into Gropius.
- Clone the [template-importer](https://github.com/ccims/template-importer).
    ```sh
    git clone https://github.com/ccims/template-importer.git
    ```
  or
    ```sh
    git clone git@github.com:ccims/template-importer.git
    ```
- Make sure you have npm installed.
- In the base directory of the template-importer run:
    ```sh
    npm i
    npm run build
    ```
- Next, run the import script. Make sure you use your configured OAuth2 client-id and -secret:
    ```sh
    npm start <path-to-clara>/gropius_templates.json <your-client-id> <your-client-secret> http://localhost:4200
    ```
  
## 2. Setup minikube and kubectl
- Install **minikube** for your local environment as described in their [official docs](https://minikube.sigs.k8s.io/docs/start).
- Ensure that [kubectl](https://kubernetes.io/docs/reference/kubectl/) is installed on your machine.
- Start minikube with your preferred configuration. The default is:
    ```sh
    minikube start
    ```
- Verify minikube is the configured context:
    ```sh
    kubectl ctx
    ```
- Create a new namespace called `clara` in the minikube cluster:
    ```sh
    kubectl create ns clara
    ```

## 3. T2-Project Configuration
- Clone the [T2-Project's devops subproject](https://github.com/t2-project/devops).
    ```sh
  git clone https://github.com/t2-project/devops.git
    ```
  or
    ```sh
    git clone git@github.com:t2-project/devops.git
    ```
- Navigate to the directory `devops/k8s/t2-microservices/base`, where you find the deployment manifests for the T2-Project microservices.
- Insert the following into each of the `Deployment` part of the respective yaml file (except for the postgres services):
    ```yaml
    spec:
      template:
        metadata:
          annotations: 
            instrumentation.opentelemetry.io/inject-java: "true"
    ```
- You will deploy the microservices below in [5. Deploy T2-Project](#5-deploy-t2-project) **after** the CLARA setup.
## 4. CLARA Setup
- Setup CLARA on your local machine as described in steps 1 and 2 on the [setup page](../../setup/index.md#1-prerequisites).
    * Use `clara` as the target namespace.
    * In step 2.1 you can skip the injection of the annotations into the deployments, as you have already done this.
- **DO NOT RUN CLARA YET**, as the T2-Project is not yet deployed.

## 5. Deploy T2-Project
- In the T2-Project's `devops`-repository navigate back to `devops/k8s` and execute the following to install the T2-Project into the cluster:
    ```sh
    chmod +x ./start-microservices.sh
    ./start-microservices.sh clara
    ```
- Describe the pods with `kubectl -n clara describe pod <any-pod>` and ensure they have `OTLP` attributes inside the description-yaml.
    * If not, check the OpenTelemetry auto-instrumentation [troubleshooting page](https://opentelemetry.io/docs/kubernetes/operator/troubleshooting/automatic/).
- For further questions regarding the T2-Project, check the official [deployment instructions](https://t2-documentation.readthedocs.io/en/latest/microservices/deploy.html).


## 6. Execution
### 6.1 Create Traffic
- Create traffic during the execution by manually clicking around the web shop.
- To do that, create a port forward to the T2-Project UI from your shell:
    ```sh
    kubectl -n clara port-forward svc/ui 7000:80
    ```
- Open the shop under http://localhost:7000/ui/products and click around the shop and order some tea to create traffic.

### 6.2 Execute CLARA
- Execute CLARA as described in step 3 on the [setup page](../../setup/index.md#3-run-clara). 
- You can use the default config as provided. No need to change anything.
- Check the CLARA logs during execution and ensure spans are coming in, when clicking around the web shop. 

### 6.3 Visit the results
- CLARA should now execute without any issues.
- If so, in the end open the Gropius UI under http://localhost:4200 and open your project.
- You should see the recovered architecture of the T2-Project in the UI now.