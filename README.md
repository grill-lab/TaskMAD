# TaskMAD: Task Multimodal Agent Dialogue

This is a modified version of the original TaskMAD system. The original version can be found [here](https://github.com/grill-lab/TaskMAD/). 

This document focuses on describing the changes and the instructions for setting up and using this version of TaskMAD. It is paired with a modified version of the [WoZStudy](https://github.com/alessandrospeggiorin/WoZStudy/tree/radboud_branch) repo. 

## Differences from the original TaskMAD

This version of TaskMAD has been adapted for use in a task where users engage in a conversation based around one of a set of predefined topics (e.g. Cooking, Travel). 

Unlike the original system where the "wizard" user would have a large set of buttons that could be clicked to generate responses in addition to a search feature, this version instead relies on an LLM to generate wizard responses (which may then be edited by the human user before being sent). The LLM integration is done through a new "Agent" class in the backend component of the system. Instead of calling an LLM API directly, this agent sends a request to an external API which then calls the LLM API internally after constructing the parameters based on the current conversation state. Each of the 2 webapps also load step-based instructions from external JSON files and display these to the users as the conversation progresses. Additional constraints have been aded to e.g. ensure that a conversational turn always consists of a single message from each user. 

A brief summary of changes and new/removed features:

 * Chat interface has had the recipe selection replaced with a topic selection
 * UI colours are updated to better distinguish different message roles and make links more visible
 * The `LLMAgent` class was added to the backend to handle communication with an external LLM wrapper API
 * Configuration file updates to support new functionality
 * New `role` field added to `OutputInteraction` protobufs (the LLM responses may have multiple roles that need to be acted on and logged)
 * Updated `WizardAgent` class in the backend to handle the new `role` field
 * JDK base image version for the backend updated from `openjdk:8` to `eclipse-temurin:17-jdk`
 * Docker deployment script added

## Configuration

Much of the configuration remains similar to the original system, but there are some changes. The use of the Docker deployment scripts in the `docker_deployment` directory is heavily recommended. This helps automate the process of building and starting the various Docker containers required for the complete system (including the `WoZStudy` webapp). See the "Deployment" section below for instructions on running the scripts. 

To get started, edit the `docker_deployment/deploy_config` file and look at the first set of parameters ("Cross-deployment parameters"). You will need to change the following parameters:

#### 1. config_url

`config_url` tells the backend service where to load its configuration data from. The URL here doesn't have to be public but must be accessible from the machine running the backend. The file format is described [here](https://github.com/grill-lab/TaskMAD#configuration-file), and an example is shown below:

```json
{
    "grpc_server_port": "8070",
    "max_number_of_simultaneous_conversations": "1000",
    "session_timeout_minutes": "180",
    "agents": [
        {
            "service_provider": "WIZARD",
            "project_id": "WizardOfOz",
            "configuration_file_URL": "configs/firebase_key.json"
        },
        {
            "service_provider": "LLM",
            "project_id": "LLMAgent",
            "configuration_file_URL": "configs/llm-config.json"
        }
    ]
}
```

The first 3 fields can usually be set to the values shown above as sensible defaults. The `agents` list defines the `Agent` subclasses that the backend will instantiate. The `service_provider` and `project_id` should be set as shown. 

Each of the agents then has its own *local* configuration file. The filenames can be changed but should always be `configs/name_of_file.json`. The local copies of the files should be placed in the `TaskMAD/docker_deployment/core_files` directory, e.g. for the above example there should exist both `TaskMAD/docker_deployment/core_files/firebase_key.json` and `TaskMAD/docker_deployment/core_files/llm-config.json`.

The configuration file for the `WizardOfOz` agent is simply a Firestore API key file. See [the original documentation](https://github.com/grill-lab/TaskMAD#firebase).

The configuration file for the `LLMAgent` agent should look like this:


```json
{
    "request_type": "POST",
    "api_endpoint": "http://..."
}
```

where `api_endpoint` defines the external LLM API endpoint.

#### 2. recipe_url (TODO)

`recipe_url` should point to a JSON file containing a list of conversation topics. This is parsed and displayed to the user when they start a conversation in the chat webapp. 

Example:

```json
{"recipes":[
    {"id": "0", "page_id": "0", "page_title": "Cooking" }, 
    {"id": "1", "page_id": "1", "page_title": "Travel"}, 
    {"id": "2", "page_id": "2", "page_title": "Movies"},
    {"id": "3", "page_id": "3", "page_title": "Music"},
]}
```

#### 3. data_url

`data_url` should point to a JSON file which defines things like the initial wizard message to be sent to the user at the start of a conversation, and the set of step-based instructions to be displayed during conversations. The file format current resuses the original TaskMAD format with a couple of new fields. An example can be found [here](http://gem.cs.ru.nl/grad-pkg/radboud_taskmad_data.json). 

#### 4. spreadsheet_url

This can be set to an empty string, it's no longer used.

TODO remove

#### 5. envoy_ssl_cert / envoy_ssl_privkey

These files are loaded by the Envoy proxy container to provide self-signed SSL certificate support. The files themselves should be placed in `TaskMAD/config/certs`, and the value of each parameter should be given as `./certs/<filename>`, i.e. relative to `TaskMAD/config`. 

#### 6. [domain]  parameters

There are a set of parameters for each of the services that make up the complete TaskMAD system. Most of these should not typically need changed. The deployment script assumes you have domains configured for each service, so you will need to set the `<service>[domain]` parameters appropriately to ensure the Envoy proxy container will route incoming connections to the appropriate container. The `search` service is not currently used, and `core` is the backend service. 

### Envoy 

The Docker configuration includes an [Envoy](https://www.envoyproxy.io/) container to handle the routing of connections to the appropriate container depending on the hostname (the assumption is you want to run all the services on a single machine and share a single port for all of them). Envoy is also required to handle incoming gRPC connections which the backend can't accept directly. 

The default Envoy configuration can be found in `TaskMAD/configs/envoy-taskmad-docker.yaml`. It will listen for connections on port 443 and handle SSL using the certificate and key file defined in the `deploy_config` file. It then routes connections based on the hostname given in the URL and forwards them to different local ports where the respective containers are listening. 

**TODO do this automatically** You will need to edit this file to update the domain names for each service to the set you wish to use, otherwise no incoming connections will be routed successfully.

If you want to change the port and/or interface that Envoy listens on, edit this line near the top of the file:

> address: {socket_address: {address: 0.0.0.0, port_value: 443}}

## Deployment

Pre-deployment checklist:
 * domains created and DNS updated
 * SSL certificate created and cert/key files placed in `TaskMAD/config/certs`, referenced by `envoy_ssl_cert` and `envoy_ssl_privkey` parameters
 * top-level backend configuration file available at URL defined by `config_url` parameter
 * agent configuration files placed in `TaskMAD/docker_deployment/core_files`
 * topics list JSON file available at URL defined by `recipe_url` parameter
 * other conversation data available at URL defined by `data_url` parameter
 * a copy of the `WozStudy` repo has been cloned into the same directory as the `TaskMAD` repo (i.e. there should be a parent directory containing both repos, `WoZStudy` shouldn't be cloned inside `TaskMAD`!)

To build the set of TaskMAD images:

> ./deploy.sh build

Once all images have been built, start the services:

> ./deploy.sh start

To stop services:

> ./deploy.sh stop

