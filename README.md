# TaskMAD: Task Multimodal Agent Dialogue

# Project Overview

**A Framework Agent Dialog, Experimentation and Analysis**

The proposed project aims to develop a meta-agent framework for conversational search research (MetaBot).

Metabot will allow researchers to plug-in existing bots for evaluation and building conversational training / test collections. It addresses fundamental research problems on agent system design.

**Metabot key features**

Allow testing bots from a variety of frameworks (end-to-end ML bots, rule-based systems, and existing Google DialogFlow / Alexa Skills bots). 
Logging and analytics of conversations
Dialogue simulation and crowdsourcing using a web simulator 


Metabot core is the main dialogue system. It will support flexible third-party APIs for major components  -- speech recognition, natural language understanding, dialogue management, and language generation (and TTS). 

Metabot core is a federated agent system.  It runs and coordinates multiple internal and external agent systems. 
Agent intent routing - Determines what agents are relevant to the current step in the conversation
Agent response ranking - Perform relevance ranking of the returned agents
Conversation state management - Keeps a version of the current state of the conversation 
Metabot core will be written in a mixture of Java & Kotlin.  We plan to use RxJava (or a similar framework) for asynchronous event handling. 

**Metabot Simulator**

A web application interface that allows users to play (and simulate) offline and online conversations. 
This is used to create crowdsourced conversational datasets.
Metabot simulator developed with Kotlin.

**Research Goals**

It will be used as the platform for a new proposed TREC track (government-sponsored competition) on conversational agents in 2019.  We will start the development of the tasks over the summer.
Proposed tasks: Federated agent ranking (and response ranking)
Informational dialogue - Using Wikipedia and third-parties APIs

# Online Demo

It is possible to test both the Chat and Wizard interfaces by using the urls provided below: 

## Chat Interface

To access to the chat interface simply visit the following url [http://chat-uog.online](http://chat-uog.online)

In order to effectively connect the Chat interface with the backend server it is required to provide the configurations outlined below: 

* **Host Url:** http://34.118.18.219
* **User ID:** user
* **Conversation ID:** *Any conversation ID of personal choice i.e. conv1*
* **Select Recipe:** *Pick any recipe of personal choice*

## Wizard Interface 

To access the Wizard interface simply visit the following url [http://woz-uog.online](http://woz-uog.online)

In order to effectively connect the Wizard interface with the backend server it is required to provide the configurations outlined below: 

* First of all, from the **Selected Connector** section select the **Agent Dialogue** option. Here specify the following: 
	* **Server Url:** http://34.118.18.219
	* **User ID:** wizard
	* **Conversation ID:** *The same conversation ID of specified for the chat*
* Tick the checkbox **Show chat transcript**
* Then click on the *Upload Excel Spreadsheet* and upload the file *woz_input_excel.xlsx* provided in the repository.  

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

We need to update the `Dockerfile ` in the `agent-dialogue-core` in order to tell it where to find the configuration file. The URL mapping to the configuration file has to be specified in this line in the `Dockerfile`

```yaml
CMD ["java", "-jar", "target/agent-dialogue-core-0.1-SNAPSHOT.jar",
 "<URL_CONFIG_FILE>"]
``` 

## Local Deployment 

If everything has been configured correctly it is possible to deploy the system. This is a 3 steps process: 
 
1. We need to edit the `deployment-envoy.yaml` file. We need to remove all the configurations that refer to the deployment on Cloud. More precisely we need to comment out the following lines:

	- Remote IP address `loadBalancerIP: "34.118.18.219"`
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

Deploying to Google Cloud requires multiple services to communicate and interact correclty. Before to proceed make sure to have done all the steps required up to **Local Deployment**. 

### 1. Creating static external IP addresses

1. The first thing we need to do is to create static external IP addresses that won't change as we redeploy our application. This will make our applications accessible. To do so, in the Google Cloud console, go to the *External IP addresses* under the *VPC Network* tab. 
2. Reserve 2 different ip addresses one for the chat interface and one for the grpc server. Here you can pick either regional or global addresses as desired. 
3. Now, specify the reserved IP addresses in the files `chat_deployment_nginx.yaml` and `deployment-envoy.yaml` under the tag `loadBalancerIP`. 

### 2. Create Artifact Registry Repository

1. Create a an Artifact repository on Google Cloud with the preferred name. Select Docker as format. 
2. Now we need to change again `chat_deployment_nginx.yaml` and `deployment-envoy.yaml` in order to specify from where our images will need to be pulled from when deploying to kubernetes. The path we are looking for should similar to the following one `europe-west2-docker.pkg.dev/agentdialoguesystem/agent-dialogue-system`
	- Hence, in both .yaml files replace the `image` tag with the path previously defined in order to point to the images we will push later on. 

### 3. Creating and Pusing Images to Artifact Registry

At this point, we can create the images we need locally and push them to Artifact registry. To do so we proceed as follows: 

1. Run the command `docker build -t envoy:latest -f envoy_updated.Dockerfile .` from within the `config` folder
2. Run the command `docker build -t grpc-server:latest  .` from within the `agent-dialogue-core` folder
3. Run the command `docker build -t chat:latest .` from within the folder `agent-dialogue-ui`
4. Now push the images to Artifact registry by followign [this guide.](https://cloud.google.com/artifact-registry/docs/docker/pushing-and-pulling)





