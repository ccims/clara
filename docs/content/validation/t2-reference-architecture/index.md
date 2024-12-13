# Evaluation against T2-Reference-Architecture

CLARA has been evaluated against the [T2-Project Reference-Architecture (Microservices)](https://t2-documentation.readthedocs.io/en/latest/index.html).

Follow this guideline step-by-step to recreate the evaluation of CLARA using the T2-Project, locally on a [minikube](https://minikube.sigs.k8s.io/docs/) cluster.


## Setup
The setup consists of the Gropius setup, the minikube setup, the T2-Project setup, and the CLARA setup.

### 1. Gropius Setup: 
#### 1.1 Getting Gropius

- Clone the [Gropius repository](https://github.com/ccims/gropius).

    ```sh
    git clone https://github.com/ccims/gropius.git
    ```
  or
    ```sh
    git clone git@github.com:ccims/gropius.git
    ```
!!! warning "Docker Installation"
Make sure you have a local container environment (e.g. Docker) installed and configured on your machine.

- Locally deploy the Gropius testing environment using docker-compose:
    ```sh
    docker-compose -f docker-compose-testing.yaml up -d
    ```
- Check availability by visiting http://localhost:4200.
#### 1.2 Creating Gropius OAuth Client
- To authenticate against Gropius you need an OAuth2 client. You can create one in the Gropius UI.
- Open Gropius in your browser under http://localhost:4200 and log in as the default admin (admin/admin).
- Click on the tab 'Admin' in the top menu, then select 'OAuth2' on the left menu.
- On the right menu hit the '+' to create a new OAuth2 client.
- In the opening dialog enter the following:
  - Name: CLARA
  - Redirect URLs: http://localhost:7878
  - Client credential flow user: type 'admin' and select the admin account.
  - Check 'requires secret'.
  - Check 'is valid'.
  - Hit 'Create auth client'.
- Now, you should see an entry named 'CLARA' in the list.
  - On the right, click the 'ID'-icon and copy the client-id and store it where you find it again.
  - On the right, click the key-symbol and create a new secret access key. Copy it **immediately** as you won't see it again and store it next to the client-id. 

#### 1.3 Create Gropius Project
- Create a new Project in Gropius by again opening the Gropius UI under http://localhost:4200 and logging in as the default admin (admin/admin). 
- Click on the tab 'Projects' in the top menu.
- On the right menu hit the '+' to create a Project:
  - Enter any name that suites you.
  - Enter any description that suites you. 
  - For repository URL simply enter: https://example.org
  - Hit 'create project'.
- Click on the newly created project in the list and copy the project's UUID from the header and store it where you find it again.

#### 1.4 Import Gropius Default Templates
- #TODO
  
### 2. Setup minikube and kubectl
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
- Create a new namespace called 'clara' in the minikube cluster:
    ```sh
    kubectl create ns clara
    ```

### 3. deploy T2 into the cluster
- Check the official [deployment instructions](https://t2-documentation.readthedocs.io/en/latest/microservices/deploy.html).
- Deploy the T2-Project microservices into the clara namespace by executing this in the T2-Project base folder:
    ```sh
    chmod +x ./devops/k8s/start-microservices.sh
    ./devops/k8s/start-microservices.sh clara
    ```

### 4. CLARA Setup
#### 4.1 Install CLARA
- Setup CLARA on your local machine as described in steps 1 and 2 on the [setup page](../../setup/index.md#1-prerequisites) (use clara as the target namespace).
- **Do NOT execute CLARA yet**, as the default config won't provide satisfying results.

#### 4.2 Restart all pods of the T2-Project 
- To ensure the pods of the T2-Project actually pick up the auto-instrumentation config, each deployment needs to be restarted. **DO NOT use the T2-Project install script again**, as it overwrites the namespace config of clara again.
- #TODO CHECK IF RESTART IS ENOUGH OR WE NEED RE-CREATION

#### 4.3 CLARA Config
- Configure CLARA with the following config as described in step 3 on the [setup page](../../setup/index.md#3-run-clara).
- #TODO insert CONFIG
- #TODO adapt the config with the OAuth Client


## Execution
### 1. Execute CLARA
- Execute CLARA as described in step 3 on the [setup page](../../setup/index.md#3-run-clara).
### 2. Visit the results
- CLARA should now execute without any issues.
- If so, in the end open the Gropius UI under http://localhost:4200 and open your project.
- You should see the recovered architecture of the T2-Project in the UI now.