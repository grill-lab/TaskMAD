# TaskMAD Google Cloud Platform Deployment

## Summary

This document describes the manual and scripted methods for creating a new TaskMAD deployment on Google Cloud Platform. Both methods involve performing the same steps, the difference being the `deploy_gcp.sh` method can perform almost all of the necessary operations in a (mostly) automated way.

Normally the script will be the simplest and fastest method to use, but the manual steps will be a useful reference if you encounter any problems or wish to customise the deployment.

There are 4 components or services that make up a minimal TaskMAD deployment, named as follows:
 * `core`: a gRPC server which handles communication, logging, etc
 * `chat`: a React webapp allowing a user to interact with the system
 * `woz`: a React webapp for the wizard to interact with the system
 * `search`: a REST API implementation used by the other services to retrieve data

The deployment script will create a GCP K8S cluster for each component alongside any additional resources required (external IPs, SSL certificates, a Docker image repository, GCP disks).

## Prequisites and initial steps

### Source code structure

The current [TaskMAD repo](https://github.com/grill-lab/TaskMAD/) only contains 2 of the 4 components that need to be deployed, the `core` gRPC server and the `chat` webapp. The other 2 components are in separate repos and will need to be cloned to your local machine individually. The expected directory structure for the scripted deployment method is:
```sh
\<deployment root>
   \TaskMAD                     # core and chat components
   \WoZStudy                    # woz component
   \GroundedKnowledgeInterface  # search component
```

Future versions of TaskMAD will merge the `woz` and `search` components into the TaskMAD repository.

### Pre-deployment steps

Before continuing to deploy TaskMAD:
* Follow the documentation on configuring a Firebase instance and creating a JSON configuration file (this will need to be loaded by the `core` component)
* (optionally) Perform a local deployment with Minikube and check for any initial problems with building and running the various Docker images (`TODO`: the docs won't currently cover the wizard and search API deployments)
* If necessary, create a new Google Cloud project through the [Cloud Console](https://console.cloud.google.com) to host the deployment
* You will also need to [enable billing on your account](https://console.cloud.google.com/billing/) as the deployment will incur costs (although you can use free trial period for testing)
* Ensure you have a domain available which can be used to direct traffic to each of the components

## Deploying with the script

The `deploy_gcp_config` file defines a long list of parameters for the `deploy_gcp.sh` script, allowing many aspects of each deployment to be customised.

Most of the parameters already have suitable defaults. Change the following parameters in the "Cross-deployment" section to suit your intended deployment:
* set `region` and `zone` to your preferred GCP locations (e.g. `europe-west2` and `europe-west2-a` respectively) 
* set `config_url` to the URL of your JSON configuration file as described in the [TaskMAD documentation](https://github.com/grill-lab/TaskMAD/#configuration-file) and make sure the URL is publicly available
* set `recipe_url` to the URL of your recipes JSON file and make sure the URL is publicly available 

Additional per-deployment parameters that you will probably need to change from the defaults are:
* set the `domain` parameter for each component to the domain you want to assign to it (SSL certificate creation is handled by GCP)
* set `local_files_path` for the `core` and `search` deployments. Both of these deployments rely on a GCP persistent disk as file storage, and so the script needs to copy files from your local machine to the GCP disk before the deployment is rolled out. The `local_files_path` parameter is used to set the local directory containing the necessary files for each deployment. The paths should be set as follows:
  * `core[local_files_path]` should point to a folder containing the Firebase JSON API keys file
  * `search[local_files_path]` should point to a folder containing the search API index files
* you may need to adjust the `search[disk_size_gb]` if your indexes are particularly large (the default size is 100GB).

The remaining parameters will not normally need to be adjusted for a single new deployment, but if you want to create multiple deployments within the same GCP project you will need to update names of clusters aand other objects to avoid name clashes.

Running `./deploy_gcp.sh` will print some usage information. The script has several modes selected by the first argument:
* `./deploy_gcp.sh create`: create cloud IPs; build all Docker images; push images to GCP repo; create and format disks; create clusters
* `./deploy_gcp.sh clustercheck`: monitor the status of the created clusters periodically, notify when all are in a running state (this can take some time)
* `./deploy_gcp.sh deploy`: once all clusters are running, create the deployments 
* `./deploy_gcp.sh domains`: once all deployments are created, you can run this view the GCP IPs your domains should be associated with via DNS records
* `./deploy_gcp.sh manage <deployment_name>`: once all deployments are created, this can be used to set the `kubectl` context to a particular deployment
* `./deploy_gcp.sh cleanup`: attempts to delete all deployments, clusters, and other GCP resources
* `./deploy_gcp.sh dockercleanup`: remove all local Docker images

The typical intended workflow looks like this:
```bash
#   1. Create deployment resources (for all deployments)
./deploy_gcp.sh create
#   2. Wait until all cluster enter the "RUNNING" state
./deploy_gcp.sh clustercheck
#   3. Create the deployments on the clusters
./deploy_gcp.sh deploy
#   4. Check the IPs GCP has assigned to each cluster, then manually update
#       DNS records for your domains to match
./deploy_gcp.sh domains

...

#   5. Clean up resources when the deployments are no longer required
./deploy_gcp.sh cleanup
```


### Step 1 - creating GCP resources

Run `./deploy_gcp.sh create` to create all the required resources for the deployments. This will cause the script to create GCP static IPs, persistent disks, K8S clusters, and a Docker image repository. It may take some time to complete, especially if the various Docker images need to be built locally during the process. 

The script behaviour can optionally be modified by setting the values of the `DEPLOYMENTS` and `PHASES` environment variables. `DEPLOYMENTS` determines the TaskMAD components that will be operated on in `create` or `cleanup` modes. By default this includes all 4 components, i.e. the value is `"core chat search woz"`. This is defined in the first section of `deploy_gcp_config`. 

For example if you set `DEPLOYMENTS` to be `"core"`, the script would only operate on the `core` deployment and skip the other 3. The value of this variable also affects the `cleanup` mode, so you can use it to dispose of resources for a selected deployment without affecting others.

`PHASES` can be used to get more fine-grained control over the `create` mode by choosing to skip certain phases in the usual process. The phases are:
* `services`: checks if the necessary GCP services are enabled on the current account
* `tier`: checks if the current GCP project has been set to use the "PREMIUM" network tier
* `repo`: creates a Docker image repo in the selected GCP region
* `ips`: create the required static IPs
* `images`: build all Docker images and push them to the GCP Docker repo
* `disks`: create and format GCP persistent disks
* `clusters`: create all necessary GCP clusters

and are executed in that order. The default value of `PHASES` is again defined in `deploy_gcp_config` and contains all of these phases. By overriding `PHASES` you can for example skip the checks for enabled services and network tier by setting the value to `"repo ips images disks clusters"` (note that it is *not* necessary to list the phases in the correct order). 

#### Examples of using DEPLOYMENTS/PHASES env vars:

Override `DEPLOYMENTS`:
```bash
# Only create and deploy the "core" deployment
DEPLOYMENTS="core" ./deploy_gcp.sh create
DEPLOYMENTS="core" ./deploy_gcp.sh clustercheck
DEPLOYMENTS="core" ./deploy_gcp.sh deploy
```

Override `PHASES`:
```bash
# Create all deployments, but skip the "services" and "tier" phases if you
# know these are already correctly configured
PHASES="repo ips images disks clusters" ./deploy_gcp.sh create
```

Override both env vars:
```bash
# Create the "core" deployment only, skipping the same two phases
DEPLOYMENTS="core" PHASES="repo ips image disks clusters" ./deploy_gcp.sh create
```

### Step 2 - wait for cluster creation to complete

When the script finishes running the `create` step, the new K8S clusters will still be provisioning and not ready for deployments. At this point you can run `./deploy_gcp.sh clustercheck` to query the state of each cluster every 20s. The script will continue checking indefinitely until cancelled or until all clusters are in the `RUNNING` state. 

### Step 3 - creating deployments on the clusters

Once all clusters have started successfully, run `./deploy_gcp.sh deploy` to create the various K8S objects for each component on their respective clusters. If you want more detail on what the script is doing, see the `create_<component>_deployment.sh` scripts, e.g. `create_core_deployment.sh`. 

### Step 4 - configuring domains

As part of the deployment process in step 3, the script will create [ManagedCertificate](https://cloud.google.com/kubernetes-engine/docs/how-to/managed-certs) objects for the domains specified in `deploy_gcp_config`. Once these are created GCP will automatically begin a process of provisioning them, which can take up to 1 hour (during this time you will not be able to make successful SSL connections to the endpoints).

You will need to manually create DNS A records for your domains pointing them to the GCP-assigned static IP addresses. The required domain/IP mappings can be viewed by running `./deploy_gcp.sh domains`. 

The output will also indicate the current status of the certificates as "Provisioning" or "Active". 

### Step 5 - check connectivity

Once the output of `./deploy_gcp.sh domains` shows all certificates are active, you may also need to wait for the DNS records you created to propagate sufficiently. After the certificate provisioning and DNS propagation completes, you should then be able to access the services at the configured endpoints. Visiting the domains for the `chat` and `woz` components should show their respective login/configuration pages, and visiting `https://<search domain>/healthz` should return an HTTP 200 response. 

### Step 6 - deleting resources

When you're finished with a deployment you can run `./deploy_gcp.sh cleanup` to delete all the GCP resources created by the script. **Note that this may need run more than once to ensure everything is deleted**, e.g. it will fail to delete a persistent disk if that disk is still attached to a cluster that is in the process of being deleted but hasn't finished yet. Similarly SSL certificates are tied to cluster load balancers and can't be deleted while those still exist. 

It is recommended to manually check your [Cloud Console](https://console.cloud.google.com/) for any remaining resources to avoid extra billing charges. 

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
./build_core_images.sh ./deploy_gcp_config region-docker.pkg.dev/project_name/repo_name build
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
