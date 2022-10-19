# TaskMAD Google Cloud Platform Deployment

## Summary

This document describes the manual and (mostly) automated methods for creating a new TaskMAD deployment on Google Cloud Platform. Both methods involve performing the same steps, the difference being the `deploy_gcp.sh` method can perform almost all of the necessary operations. 

Normally the script will be the simplest and fastest method to use, but the manual steps will be a useful reference if you encounter any problems or wish to customise the deployment beyond what is offered by the script.

## Prequisites and initial steps

Before attempting to deploy TaskMAD to GCP:
* Follow the documentation on configuring a Firebase instance and creating a JSON configuration file
* Then perform a local deployment with Minikube and check for any initial problems with building and running the various Docker images (`TODO`: the docs won't currently cover the wizard and search API deployments)
* If necessary, create a new Google Cloud project through the [Cloud Console](https://console.cloud.google.com)
* You will also need to enable billing on your account as the deployment will incur costs (although you can use free trial period for testing)

## Deploying with the script

The `deploy_gcp_config` file defines a long list of parameters for the `deploy_gcp.sh` script, allowing many aspects of each deployment to be customised. However typically you will only need to adjust some of the values in the first "Cross-deployment parameters" section:
* set `region` and `zone` to your preferred GCP locations (e.g. `europe-west2` and `europe-west2-a` respectively) 
* set `config_url` to the URL of your JSON configuration file as described in the [TaskMAD documentation](https://github.com/grill-lab/TaskMAD/#configuration-file)

The remaining parameters will not typically need to be adjusted for a single new deployment, but if you want to create multiple deployments within the same GCP project you will need to update names of clusters aand other objects to avoid name clashes. 

You may also need to adjust the `search[disk_size_gb]` if your indexes are particularly large (default is 100GB).

### Running the script

Running `./deploy_gcp.sh` will print some usage information. The script has several modes selected by the first argument:
* `./deploy_gcp.sh create`: create cloud IPs; build all Docker images; push images to GCP repo; create and format disks; create clusters
* `./deploy_gcp.sh clustercheck`: monitor the status of the created clusters periodically, notify when all are in a running state (this can take some time)
* `./deploy_gcp.sh deploy`: once all clusters are running, create the deployments 
* `./deploy_gcp.sh cleanup`: attempts to delete all deployments, clusters, and other GCP resources

Additionally the script behaviour can be modified by setting the values of the `DEPLOYMENTS` and `PHASES` environment variables. 

`DEPLOYMENTS` determines the TaskMAD components that will have deployments created. By default this includes all 4 components: the core gRPC server, the agent/user UI, the wizard UI, and the search API. The default value is defined in `deploy_gcp_config` and is "core chat search woz".

`PHASES` can be used to get more fine-grained control over the `create` mode by choosing to skip certain phases in the usual process. The phases are:
* `services`: checks if the necessary GCP services are enabled on the current account 
* `tier`: checks if the current GCP project has been set to use the "PREMIUM" network tier
* `repo`: creates a Docker image repo in the selected GCP region
* `ips`: create the required static IPs
* `images`: build all Docker images and push them to the GCP Docker repo
* `disks`: create and format GCP persistent disks
* `clusters`: create all necessary GCP clusters

The default value of `PHASES` is defined in `deploy_gcp_config` and contains all of these phases. By overriding `PHASES` you can for example skip the checks for enabled services and network tier by setting the value to "repo ips images disks clusters". 

### Examples:

Defaults:
```shell
#   1. Create deployment resources 
./deploy_gcp.sh create
#   2. Wait for cluster creation to complete
./deploy_gcp.sh clustercheck
#   3. Create the deployments
./deploy_gcp.sh deploy
#   4. Clean up resources when deployment no longer required
./deploy_gcp.sh cleanup
```

Override `DEPLOYMENTS`:
```shell
# Only create resources for the core gRPC
DEPLOYMENTS="core" ./deploy_gcp.sh create
```

Override `PHASES`:
```shell
# Skip the services and tier phases
PHASES="repo ips images disks clusters" ./deploy_gcp.sh create
```

## Deploying manually

### 1. Enable required APIs
The Google Cloud Platform consists of many different APIs and services, most of which need to be enabled before you can use them. There are 3 APIs which need to be enabled to allow the deployment to proceed. Visit each of the pages below and if necessary click the "Enable API" button.
* Compute Engine API: https://console.cloud.google.com/apis/api/compute.googleapis.com/
* Artifact Registry API: https://console.cloud.google.com/apis/api/artifactregistry.googleapis.com/
* Kubernetes Engine API: https://console.cloud.google.com/apis/api/container.googleapis.com/

### 2. Set network tier
Visit [this page](https://console.cloud.google.com/net-tier/tiers/details) and set the default network tier for your project to `Premium`.

### 3. Create an artifact repository to store Docker images
Go to the [Artifact Repositories] page, and click `Create repository`.  Enter a name (e.g. `taskmad-repo`), set "Format" to `Docker`, select a single Region, and then click `Create`. 

You should now see a list of repositories. Click the name of the one you just created. Near the top of the next page, there will be a folder path displayed, ending with the name of your repository. Click the `Copy` icon next to this and paste the resulting path somewhere. The path should have the form `region-docker.pkg.dev/project_name/repo_name`. 

### 4. Create external IP addresses 
Go to the [IP addresses](https://console.cloud.google.com/networking/addresses/list) page. Click `Reserve external static address`. On the following page, enter the name `ip-core`, set the Network Tier to `Premium`, set the region appropriately, and then click `Reserve`. Repeat this process with names `ip-chat`, `ip-search`, and `ip-woz`. 

### 5. Build and push Docker images to the image repository

It might be helpful to read [about pushing and pulling images](https://cloud.google.com/container-registry/docs/pushing-and-pulling) and [authenticating to an image repo](https://cloud.google.com/container-registry/docs/advanced-authentication). The commands used by the script to perform this step are:

```shell
gcloud auth configure-docker "${docker_repo_id}"
gcloud auth print-access-token | docker login -u oauth2accesstoken --password-stdin "https://${docker_repo_hostname}"
```

You can find the commands for building the various images in the `build_images_*.sh` files, or in the TaskMAD documentation. Be careful to tag the images with the names you will use in the deployment YAML files! 

Pushing images is done with a standard `docker push` command with the URL of the repo (see step 3). 

### 6. Create and format persistent disks

Some of the TaskMAD components require persistent disks for file storage. Disks can be created [from this page](https://console.cloud.google.com/compute/disks). Newly created disks will need to be formatted before use. You can do this by creating a temporary VM instance [from here](https://console.cloud.google.com/compute/instances), attaching the disks, SSH'ing to the VM, and formatting them with standard Linux commands (`fdisk /dev/sdX`, `mkfs.ext4 /dev/sdX1` etc). 

### 7. Create clusters

The final step before creating the deployments themselves is to create the necessary clusters through [this page](https://console.cloud.google.com/kubernetes/list/overview). You can refer to `deploy_gcp_config` for default parameters that should work for each component. 

---
TODO: describe how to actually deploy services to clusters
