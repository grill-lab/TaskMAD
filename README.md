# TaskMAD: Task Multimodal Agent Dialogue

# Project Overview

In this repository we introduce Task-oriented Multimodal Agent Dialogue (TaskMAD), a new platform that supports the creation of interactive multimodal and task-centric datasets in a Wizard-of-Oz experimental setup. TaskMAD includes support for text and voice, federated retrieval from text and knowledge bases, and structured logging of interactions for offline labeling. Its architecture supports a spectrum of tasks that span open-domain exploratory search to traditional frame-based dialogue tasks. It’s open-source and offers rich capability as a platform used to collect data for the Amazon Alexa Prize Taskbot challenge, TREC Conversational Assistance track, undergraduate student research, and others. 

TaskMAD has also been designed with the goal in mind of allowing researchers to plug-in existing bots for evaluation and building conversational training / test collections. 

## Chat Interface

In order to test the interface locally once cloned, run the following commands: 

```
cd agent-dialogue-ui
npm install 
npm start
```
The interface will be deployed locally at the address `http://localhost:3001`.

Moreover, we also provide `Dockerfile` and `yaml` files to quickly deploy the application on GCP or AWS.

In order to effectively connect the Chat interface with the backend server it is required to provide the configurations outlined below: 

* **Host Url:** The TaskMAD core public URL
* **User ID:** The User name as it will appear in the conversation.
* **Conversation ID:** Unique conversation ID to allow the communication between Wizard and User.
* **Select Recipe:** *Pick any recipe of personal choice*

## Wizard Interface 

To deploy and configure the Wizard of Oz Interface check the [TaskMAD-WoZ-Interface repository](https://github.com/grill-lab/TaskMAD-WoZ-Interface).

# Installation 

In this section, it will be described how to install and run the agent-dialogue system on a local machine. As an example, the system will be connected to the [Wizard of Oz Webapp](https://github.com/USC-ICT/WoZ) but any other supported agent integration would follow a very similar approach 

## Requirements 

In order to run the system locally, it is necessary to have the following programs installed on the local machine 

* [Docker](https://docs.docker.com/get-docker/): The system will create local images for both the gRPC server and the Envoy proxy 
* [Minikube](https://minikube.sigs.k8s.io/docs/start/): Used in order to orchestrate the deployment of the different services
* [Node.js](https://nodejs.org/en/download/): In order to run the web apps for both the chat and Wizard of Oz interfaces.  

## Additional Configurations 

### Firebase 

In order to store effectively the interaction between the user and the WoZ it is required to configure [Firebase](https://console.firebase.google.com/). To be more specific, it is necessary to create a Firestore database and define an empty collection (the program should create all the required documents and collections automatically when functioning). 

* In this context, it is also very important to remember to set the rules (by selecting the **Rules** tab on the Firestore Database interface) in order to specify the correct read/write permissions. 
* Moreover, under project settings -> service account, we need to create a Private key. This will be required in order to allow our app to interact with Firebase. The key needs to be stored (it will be used later on) and has the following format 

```json 
{
  "type": "service_account",
  "project_id": "<project_id>",
  "private_key_id": "<private_key_id>",
  "private_key": "<private_key>",
  "client_email": "<client_email>",
  "client_id": "<client_id>",
  "auth_uri": "<auth_uri>",
  "token_uri": "<token_uri>",
  "auth_provider_x509_cert_url": "<auth_provider_x509_cert_url>"
}
```

This private key should be stored in the folder `agent-dialogue-core/keys`

### Files Structure 

The main folders used in order to run the project are the following: 

**Agent Dialogue**

* `agent-dialogue-core`: This is the core folder in which agents and gRPC servers are defined. 
* `agent-dialogue-ui`: The main chat UI that users can use to interact with agents/Woz


### Configuration File

We do need a configuration file in order to specify some configuration settings of our agent. The configuration file must be a JSON file stored online (Cloud Storage or [JSONBIN.io](https://jsonbin.io/login)) as we need a publicly accessible URL. 

The file must have this format 

```json
{
  "grpc_server_port": "8070",
  "agents": [
    {
      "service_provider": "WIZARD",
      "project_id": "WizardOfOz",
      "configuration_file_URL": "<configuration_file_NAME>"
    }
  ]
}
```

The  **configuration\_file_URL** is the name of the JSON Firebase private key previously defined (which should be stored in the `agent-dialogue-core` folder). 

### Update agent-dialogue-core Dockerfile

The `Dockerfile` for the `agent-dialogue-core` service needs to be given a URL to the configuration file. Note that this URL should point to the JSON file described in the previous step, not the `configuration_file_URL` values defined inside the file itslef.

The URL should be supplied as a parameter when building the Docker image, e.g.:
```
docker build -f Dockerfile --build-arg config_url=https://somehost.com/config.json .
```

If you are creating a GCP deployment, this step is handled automatically (see the [README](gcp_deployment/README.md)).

## Local Deployment 

If everything has been configured correctly it is possible to deploy the system. This is a 3 steps process: 
 
1. We need to edit the `deployment-envoy.yaml` file. We need to remove all the configurations that refer to the deployment on Cloud. More precisely we need to comment out the following lines:

	- Remote IP address `loadBalancerIP: "35.241.45.255"`
	- Remove the volumes
	
		```yaml
		volumes:
	        - name: disk-core-volume
	          persistentVolumeClaim:
	            claimName: disk-core-claim
	            
	    ... 
	    
	    
	   		volumeMounts:
      			- mountPath: "code/keys"
       		  name: disk-core-volume 
		```
	- Change the files as follows to specify where to find the local images
	
		```yaml
	      - name: esp
	        image: envoy:latest
	        imagePullPolicy: Never
	        ports:
	          - containerPort: 10000
	      # [END envoy]
	      - name: core
	        image: grpc-server:latest
	        imagePullPolicy: Never
	        ports:
	          - containerPort: 8070
		```
	
2. Run the script `agent_dialogue_deployment.sh`. This will take care of building the required docker images and managing Minikube deployments. Eventually, the script should open a browser window exposing the public IP that can be used to access the gRPC server. 
2. From withing `agent-dialogue-ui` run `npm start` to start the chat interface (or build from the Dockerfile)

### Using the agent-dialogue system

Both web apps should prompt us with login interfaces. Here we should specify the following: 

* **Host URL:** This is the public URL resulting from running `agent_dialogue_deployment.sh`
* **User ID:** Any user ID of choice. This has to be the same one used in both web apps. 
* **Conversation ID:** This has to be the same one for both interfaces (so that the two apps can communicate)

If the process has been successful, we should be able to interact with the two apps, see real-time updates on both interfaces and the Firestore Database.  

## Deployment on Google Cloud

Deploying to Google Cloud requires multiple services to communicate and interact correclty. Before to proceed make sure to have done all the steps required up to **Local Deployment**. The Google Cloud deployment process is described in more detail [here](gcp_deployment/README.md).
