# SearchAPI Module

In this section, we'll see how you can easily make your datasets available to the Wizard through our custom SeachAPI module. This will make your own documents accessible to the Wizard on the Wizard Interface. 

## Requirements

Before proceeding futher, make sure you have the following tools installed in your local machine. 

* Python3
* Docker 
* Kubernetes
* GCloud utils


## Convert your datasets to Anserini Format

In TaskMAD we use [Pyserini](https://github.com/castorini/pyserini/) in order to index and retrieve documents. Hence, you need to first convert your datasets into a format that's compatible with  the framework and specifically with TaskMAD.

 To do so, we provide a set of useful scripts and classes you can use inside the directory `data_to_anserini`. A comprehensive guide on such conversion can be found [here](/search_api/data_to_anserini/README.md).

## Build your own Indexes

Once you have your datasets in Anserini format, you can easily build your own indexes by following this guide on [Index your own documents](https://github.com/castorini/pyserini/#how-do-i-index-and-search-my-own-documents).

After creating your index, to make it available to the SearchAPI, you have to place it inside the `/search_api/api/data` folder. 

## SearchAPI In details

The SearchAPI consists of a Flask App which exposes several endpoints for searching and retrieval. The core functionalities of this module are provided in `main.py`. 

However, before deploying the module, there are some required configurations. 

### SearchAPI Configurations

As aforementioned, all the API configurations are defined in `main.py`. Specifically you can configure the following aspects: 

* **DEPLOYMENT_HOST:** Deployment host. By default the value is `localhost/0.0.0.0`
* **DEPLOYMENT_PORT:** Defaults to `5050`. If modified, remember to uptate also all the relevant files in `deployment/` and `Dockerfile`.
* **indexes_folders:** This is a dictionary that defines exactly where your indexes are located. The key values in the dictionary (i.e. `wikipedia`) are the values you need to pass as `knowledge_source` parameters when calling the various endpoints. 
* **NUMBER_DOCUMENTS_TO_RETRIEVE:** Number of documents we want to retrieve when performing search.
* **SEARCHER_TITLE_WEIGHT:** Weight given to the page title when performing serch (higher weight means higher importance for the ranking algorithm). Weight defaults to `0.5`.
* **SEARCHER_CONTENTS_WEIGHT:** Weight given to the actual document when performing search. Weight defaults to `1`.

The relevant code is outlined below: 

```python
DEPLOYMENT_HOST = '0.0.0.0'
DEPLOYMENT_PORT = 5050

indexes_folders = {
    'wikipedia': 'data/wiki_index'
}

# Searcher default parameters
NUMBER_DOCUMENTS_TO_RETRIEVE = 5
SEARCHER_TITLE_WEIGHT = 0.5
SEARCHER_CONTENTS_WEIGHT = 1
```

### Available SearchAPI-Endpoints 

The API exposes 3 `GET` endpoints:

* `/search`:
    * **Overview:** This endpoint simply searches and returns documents given a query.
    * **Request Body:**
        ```json
        {
            "query": "(string) -> Search Query we want to issue",
            "knowledge_source": "(string) -> The index where we want to perform the search. The value must be the dictionary key as defined in indexes_folders",
            "number_documents_to_retrieve": "(string) -> Number specifying how many documents to return. If this parameter is missing then it defaults to 5"
        }
        ```
    * **Output**
        ```json
        {
            "documents": "(List[string]) -> List of documents in Anserini format",
            "errors": "(List[string]) -> List of errors that occurred. Empty list if no errors",
        }
        ```
* `/extract_page`:
    * **Overview:** This endpoint extracts a full page given a single document.
    * **Request Body:**
        ```json
        {
            "section_id": "(string) -> Anserini id of the a specific section whose page we want to retrieve.",
            "page_id": "(string) -> The page id from where section_id comes from.",
            "knowledge_source": "(string) -> The index to consider. The value must be the dictionary key as defined in indexes_folders",
            
        }
        ```
    * **Output**
        ```json
        {
            "documents": "(List[string]) -> List of all the documents belonging to that page (in Anserini format). Documents are ordered as they appear on the page. So you can assume the one at index 0 corresponds to the first one.",
            "errors": "(List[string]) -> List of errors that occurred. Empty list if no errors",
        }
        ```
* `/doc_by_id`:
    * **Overview:** This endpoint extracts a document given its id.
    * **Request Body:**
        ```json
        {
            "doc_id": "(string) -> Document id that we want to extract",
            "knowledge_source": "(string) -> The index to consider. The value must be the dictionary key as defined in indexes_folders",
            
        }
        ```
    * **Output**
        ```json
        {
            "document": "(json) -> A single json document in Anserini format.",
            "errors": "(List[string]) -> List of errors that occurred. Empty list if no errors",
        }
        ```

## Running the SearchAPI Locally

In this section we will see how to run the SearchAPI module locally as a Python script. Running locally simply helps for local testing and debugging. For remote deployment it's recommended to check the *Deploying and Running SeachAPI on Google Cloud* section. 

As aforementioned, your custom indexes must be inside `search_api/api/data`

To run the module from the terminal, simply navigate to `search_api/api/` and run the command `python3 main.py debug`. This will start the SearchAPI which will be available at `http://0.0.0.0:5050`.

## Deploying and Running SeachAPI on Google Cloud 

To make the SearchAPI module accessible, it is possible to deploy it on any cloud service of choice. We use Docker and Kubernetes for scalable deployment. All the relevant deployment files can be found in the `deployment` folder. 

**It is also essential to mention, that indexes are not included as part of the image being built with Docker in order to keep it light. However, indexes will have to be transferred inside the running container in order to make them availalbe.**

In the next section we'll provide instruction on how to fully deploy the module on Google Cloud. 

### Deployment on GCP with Docker & Kubernetes

1. The first step is to create a Kubernetes cluster on GCP where our application will be deployed. Therefore, go to GCP -> Kubernetes Engine -> Clusters. Here press on the create button and create a GCP standard cluster. For the purpose of this guide we will call the cluster `search-api-cluster`
    * **Required Configurations:**
        * default-pool -> Nodes -> Machine Type -> Make sure it has at least 4CPUs and 8GBs of RAM 
        * default-pool -> Nodes -> Boot disk Size -> Increase to at least 100GB
        * default-pool -> Security -> Access Scopes -> Allow full access to all Cloud APIs

2. We now need to create an external IP address that will be associated to our running instance.    
    * To do so, go to VPC Network -> IP Addresses. 
    * Here click on Reserve External Static Address
    * The Type must be regional and the region has to correspond to the one selected for the cluster. 
    * Once created, this will reserve a static IP address which you should be able to see under the column *IP Address*.
    * Copy this IP address and paste it in `api/deployment/api/deployment.yaml` under `loadBalancerIP: "<YOUR_EXTERNAL_IP>"`

3. We now need to create a disk that will be used in order to store our indexes. To do so, in GCP, go to Compute Engine -> Disks. 
    * Create a disk and name it `disk-indexes-disk`
    * The location has to correspond to the one of the `search-api-cluster`
    * Select as Disk Type *Standard persistent disk*
    * Specify a Size that matches the size of all the indexes you have. 

4. It's now time to build are search api Docker image and store it on Google Cloud. We will be using Artifact Registry to store our image. The steps required are listed below: 
    * Create an Artifact Registry repository. Hence, go to Artifact Registry -> Repositories. 
    * Here, press on Create Repository
    * Select Docker as Format
    * For region pick the most convenient one. I would recommend going for the same one used for the SearchAPI to reduce latency. 
    * Once created, go back to your local machine and build the image. To do so, you need to navigate to `search_api/api/` and run the command `docker build -t search-api:latest .`. This will build the image and will name it `search-api`
    * In order to push the image to artifact registry we can execute the following commands: 
        * `gcloud auth configure-docker <YOUR_REPO_REGION>-docker.pkg.dev`
        * `docker tag search_api:latest <YOUR_REPO_REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/<YOUR_REPO_NAME>/search_api:latest`
        * `docker push <YOUR_REPO_REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/<YOUR_REPO_NAME>/search_api:latest`

    * Once the image has been pushed, we need to update our `api_deployment.yaml` file in `search_api/api/deployment` to point to the newly pushed image. 
        * Here simply change `image:<YOUR_REPO_REGION>-docker.pkg.dev/<YOUR_PROJECT_ID>/<YOUR_REPO_NAME>/search_api:latest`

5. Now that we have our cluster running, our external ip address, our image on artifact registry and all the deployment configurations done, it's time to deploy our image on the cluster. However, before proceeding further we need to first create a persistent volume which will then get attached to our running container. This will allow us to store our indexes and keep the data persistent even if the running container/pod will go offline or will get destroyed. We will also have to move our indexes inside our running container to make them available. 

    To do all these steps, let's proceed as follows:  
    * From our local machine, we first need to connect to the cluster. Usually this requires issuing the following commands: 
        * `gcloud container clusters get-credentials search-api-cluster --zone <YOUR_CLUSTER_ZONE>`.
        * Now we need to create our persistent volume. Hence, navigate to `search_api/api/deployment` and run the command `kubectl apply -f persistent_volume_k8.yaml`
        * Now we can launch our container inside the cluster simply by running the command  `kubectl apply -f api_deployment.yaml`. Our application will be availalbe at `http://<YOUR_EXTERNAL_IP>`. 
        * However, we still have to copy our indexes inside the running container. To do so we have 2 different approaches: 
            1. Upload all our indexes inside a Google Cloud Bucket and download them inside the running container (Preferred Approach).
                * Check this [guide](https://cloud.google.com/storage/docs/uploading-objects#gsutil) on how to push data to a cloud bucket 
                * Get the id of the running pod in which our container is running with the command `kubectl get pods` (this assumes that we are already connected to the cluster)
                * Go inside the container `kubectl exec -it <YOUR_POD_ID> -- bash`
                * Navigate inside the `data` folder and from here download all your indexes by issuing the command described [here](https://cloud.google.com/storage/docs/downloading-objects#prereq-cli). 
            2. Copy our indexes directly from our local machine
                * To do so, we first need to get the id of the pod in which our container is running with the command `kubectl get pods` (this assumes that we are already connected to the cluster)
                * Copy the ID of the cluster, and then run the command `kubectl cp <YOUR_INDEX_FOLDE> default/<YOUR_POD_ID>:data`. This will copy your full index inside the `data` folder within the cluster. 



At the end of this process you should be able to access to the SearchAPI and perform request.





