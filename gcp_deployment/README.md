# TaskMAD Google Cloud Platform Deployment

## Summary

This document describes the manual and (mostly) automated methods for creating a new TaskMAD deployment on Google Cloud Platform. Both methods involve performing the same steps, the difference being the `deploy_gcp.sh` method can perform almost all of the necessary operations in a (mostly) automated way.  

Normally the script will be the simplest and fastest method to use, but the manual steps will be a useful reference if you encounter any problems or wish to customise the deployment beyond what is offered by the script.

## Prequisites and initial steps

Before attempting to deploy TaskMAD to GCP:
* Follow the documentation on configuring a Firebase instance and creating a JSON configuration file
* Perform a local deployment with Minikube and check for any initial problems with building and running the various Docker images (`TODO`: the docs won't currently cover the wizard and search API deployments)
* If necessary, create a new Google Cloud project through the [Cloud Console](https://console.cloud.google.com)
* You will also need to [enable billing on your account](https://console.cloud.google.com/billing/) as the deployment will incur costs (although you can use free trial period for testing)

## Deploying with the script

The `deploy_gcp_config` file defines a long list of parameters for the `deploy_gcp.sh` script, allowing many aspects of each deployment to be customised. 

Typically you will only need to adjust some of the values in the first "Cross-deployment parameters" section:
* set `region` and `zone` to your preferred GCP locations (e.g. `europe-west2` and `europe-west2-a` respectively) 
* set `config_url` to the URL of your JSON configuration file as described in the [TaskMAD documentation](https://github.com/grill-lab/TaskMAD/#configuration-file)

Additional per-deployment parameters that you will probably need to change from the defaults are:
* `local_files_path` for the `core` and `search` deployments. Both of these deployments rely on a GCP persistent disk as file storage, and so the script needs to copy these files from your local machine to the GCP disk before the deployment is rolled out. The `local_files_path` parameter is used to set the local directory containing the necessary files for each deployment
* you may need to adjust the `search[disk_size_gb]` if your indexes are particularly large (the default size is 100GB).
* TODO probably also certificate stuff

The remaining parameters will not normally need to be adjusted for a single new deployment, but if you want to create multiple deployments within the same GCP project you will need to update names of clusters aand other objects to avoid name clashes. 

### Running the script

Running `./deploy_gcp.sh` will print some usage information. The script has several modes selected by the first argument:
* `./deploy_gcp.sh create`: create cloud IPs; build all Docker images; push images to GCP repo; create and format disks; create clusters
* `./deploy_gcp.sh clustercheck`: monitor the status of the created clusters periodically, notify when all are in a running state (this can take some time)
* `./deploy_gcp.sh deploy`: once all clusters are running, create the deployments 
* `./deploy_gcp.sh cleanup`: attempts to delete all deployments, clusters, and other GCP resources

Additionally the script behaviour can be modified by setting the values of the `DEPLOYMENTS` and `PHASES` environment variables. 

`DEPLOYMENTS` determines the TaskMAD components that will be operated on in `create` or `cleanup` modes. By default this includes all 4 components: the core gRPC server, the agent/user UI, the wizard UI, and the search API. The default value is defined in `deploy_gcp_config` and is "core chat search woz". For example, if you define `DEPLOYMENTS` as "core", the script would only operate on the core deployment and skip the other 3. The value of this variable also affects the `cleanup` mode, so you can e.g. use it to dispose of resources for a selected deployment without affecting others. 

`PHASES` can be used to get more fine-grained control over the `create` mode by choosing to skip certain phases in the usual process. The phases are:
* `services`: checks if the necessary GCP services are enabled on the current account 
* `tier`: checks if the current GCP project has been set to use the "PREMIUM" network tier
* `repo`: creates a Docker image repo in the selected GCP region
* `ips`: create the required static IPs
* `images`: build all Docker images and push them to the GCP Docker repo
* `disks`: create and format GCP persistent disks
* `clusters`: create all necessary GCP clusters

and are executed in that order. The default value of `PHASES` is defined in `deploy_gcp_config` and contains all of these phases. By overriding `PHASES` you can for example skip the checks for enabled services and network tier by setting the value to "repo ips images disks clusters" (note that it is *not* necessary to list the phases in the correct order). 

### Examples:

Defaults:
```bash
#   1. Create deployment resources (for all deployments)
./deploy_gcp.sh create
#   2. Wait until all cluster enter the "RUNNING" state
./deploy_gcp.sh clustercheck
#   3. Create the deployments on the clusters
./deploy_gcp.sh deploy
#   4. Clean up resources when the deployments are no longer required
./deploy_gcp.sh cleanup
```

Override `DEPLOYMENTS`:
```bash
# Only create and deploy the "core" deployment
DEPLOYMENTS="core" ./deploy_gcp.sh create
DEPLOYMENTS="core" ./deploy_gcp.sh clustercheck
DEPLOYMENTS="core" ./deploy_gcp.sh deploy
```

Override `PHASES`:
```bash
# Create all deployments, but skip the "services" and "tier" phases
PHASES="repo ips images disks clusters" ./deploy_gcp.sh create

```

Override both:
```bash
# Create the "core" deployment only, skipping the same two phases
DEPLOYMENTS="core" PHASES="repo ips image disks clusters" ./deploy_gcp.sh create
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
Go to the [Artifact Repositories](https://console.cloud.google.com/artifacts) page, and click the `Create repository` button. Enter a name (e.g. `taskmad-repo`), set "Format" to `Docker`, set the "Location type" to `Region`, select your desired region, and then click `Create`. 

You should now see a list of repositories. Click the name of the one you just created. Near the top of the next page, there will be a folder path displayed, ending with the name of your repository. Click the `Copy` icon next to this and paste the resulting path somewhere. The path should have the form `region-docker.pkg.dev/project_name/repo_name`, where `project_name` is the name of your active GCP project and `repo_name` is the name you selected for the repository. This path is required when configuring the deployment files so that Docker images can be pulled from the repository.

### 4. Create external IP addresses 
Go to the [IP addresses](https://console.cloud.google.com/networking/addresses/list) page. Click `Reserve external static address`. On the following page, enter the name `ip-core`, set the Network Tier to `Premium`, set the region appropriately, and then click `Reserve`. Repeat this process with names `ip-chat`, `ip-search`, and `ip-woz`. You will able to view the assigned IPs on this page which again will be useful later in the process. 

### 5. Build and push Docker images to the image repository

It might be helpful to read [about pushing and pulling images](https://cloud.google.com/container-registry/docs/pushing-and-pulling) and [authenticating to an image repo](https://cloud.google.com/container-registry/docs/advanced-authentication) at this stage. 

Before you can push images to the GCP Docker repo, you need to configure gcloud as a credential helper for your local Docker client. You will need to run the following commands
```bash
# replace ${docker_repo_id} with the full path from step 3, i.e. region-docker.pkg.dev/project_name/repo_name
gcloud auth configure-docker "${docker_repo_id}"
# replace ${docker_repo_hostname} with the first part of the same path, i.e. region-docker.pkg.dev
gcloud auth print-access-token | docker login -u oauth2accesstoken --password-stdin "https://${docker_repo_hostname}"
```

You can find the commands for building the various images in the `build_images_*.sh` files, or in the TaskMAD documentation. Be careful to tag the images with the names you will use in the deployment YAML files if you are not using the defaults. 

Pushing images is done with a standard `docker push` command with the URL of the repo (see step 3). 

If you want to reuse the existing scripts to build and push the images, you can do this by calling them with the expected parameters as follows:
```bash
# example of building + pushing core deployment images
# arguments are the path to deploy_gcp_config and the image repo path
./build_core_images.sh ./deploy_gcp_config region-docker.pkg.dev/project_name/repo_name
```

### 6. Setting up persistent disks

The "core" and "search" TaskMAD components require persistent disks for file storage. Disks can be created manually [from this page](https://console.cloud.google.com/compute/disks), and you can find the default parameters in `deploy_gcp_config`: `disk_name` sets the name of the disk, `disk_size_gb` sets the size in gigabytes. You'll need to use the same "Region" and "Zone" settings when creating the disk as you've used for other resources.

Newly created disks will need to be formatted before use. You can do this by creating a temporary VM instance [from here](https://console.cloud.google.com/compute/instances), attaching the disks, [SSH'ing to the VM](https://cloud.google.com/sdk/gcloud/reference/compute/ssh), and formatting them with standard Linux commands (`mkfs.ext4 /dev/sdX` etc). 

For the deployments that require disks it's also necessary to copy some files to them before creating the deployments. This can be done with the [gcloud SCP command](https://cloud.google.com/sdk/gcloud/reference/compute/scp). 

Similarly to step 5, you may wish to reuse the helper script that `deploy_gcp.sh` calls to perform all of these steps except for creating the disk. This can be done as follows:

```bash
# The script requires several parameters:
#  - the name of the temporary VM instance to create
#  - the name of the already-created GCP disk
#  - the GCP zone you created the disk in
#  - local source directory for the files to be copied
#  - remote path structure to copy the files into (e.g. /some/path/)
#  - true if the disk was newly created (needs formatted), false if not 
./copy_files_to_gcp_disk.sh tempvm-core disk-core-disk europe-west2-a ~/core_disk_files /core_files true
```

### 7. Create clusters

The final step before creating the deployments themselves is to create the necessary clusters through [this page](https://console.cloud.google.com/kubernetes/list/overview). You can refer to `deploy_gcp_config` for default parameters that should work for each component. 

Click the `Create` button and choose "Standard" mode. Set your desired cluster name, select a "Zonal" cluster using the same zone as for other resources. Leave the other options at their defaults. Then click "default-pool" on the menu to the left, and adjust the number of nodes to match the `node_count` value from `deploy_gcp_config` for the current deployment. Then click "Nodes" under "default-pool" and set the "Series", "Machine type" and "Boot disk size" parameters as a minimum. 

---
TODO: describe how to actually deploy services to clusters. this shouldn't be too complex, mostly running the kubectl auth command, then manually editing parameters in deployment files and running kubectl apply commands...
