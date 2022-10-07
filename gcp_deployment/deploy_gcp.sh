#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# TaskMAD deployment script. Assuming everything works, this aims to create
# all the required GCP resources for a full deployment of the system. The 
# resources used will currently include:
#   - 4 reserved static IPs
#   - a Docker artifact repository
#   - 2 persistent disks
#   - 4 K8S clusters (with varying machine-type and disk-sizes)

# define some friendly names for ANSII colour escape codes
RED="\e[31m"
GREEN="\e[32m"
YELLOW="\e[33m"
BLUE="\e[34m"
PURPLE="\e[35m"
CYAN="\e[36m"
NC="\e[0m"

script_path="$( dirname -- "$0"; )"

echo_color() {
    # Simple wrapper for the echo command to print coloured messages
    # 
    # $1 = message to print
    # $2 = color sequence (optional, defaults to green)
    #
    # Return value: ignored

    # this syntax sets "color" to ${GREEN} if a 2nd argument is not passed
    # to the function
    local color="${2:-${GREEN}}"
    echo -en "${color}${1}${NC}"
}

is_phase_enabled() {
    # Check if a deployment phase is enabled in the current configuration
    # 
    # Arguments:
    #   $1 = name of the phase
    #
    # Return value: 0 if the phase is enabled, 1 if not
    
    for phase in "${create_phases[@]}"
    do
        if [[ "${phase}" == "${1}" ]]
        then 
            return 0
        fi
    done
    return 1
}

is_response_not_empty() {
    # Check if a gcloud response is empty or not
    # 
    # $1 = response text
    #
    # Return value: 0 if argument is NOT empty, 1 if it IS empty
    [[ -n "${1}" ]]
}

is_gcp_service_enabled() {
    # Checks if a named GCP service is already enabled in the current account/project
    #
    # $1 = name of service to check, e.g. compute.googleapis.com
    #
    # Return value: 0 if service is enabled, 1 if not
    
    # response to this will be an empty string if the service is not enabled
    resp=$(gcloud services list --enabled --format=yaml --filter=config.name="${1}")
    is_response_not_empty "${resp}"
}

check_and_update_network_tier() {
    # Check if the project has the network tier set to PREMIUM and 
    # switch it over if not
    #
    # Return value: ignored (should exit on error)

    echo_color "Checking default network tier is PREMIUM..."
    current_tier=$(gcloud compute project-info describe --format='value(defaultNetworkTier)')
    if [[ "${current_tier}" == "STANDARD" ]]
    then
        echo_color "needs updated\n"
        gcloud compute project-info update --default-network-tier "PREMIUM"
        echo_color "> Network tier has been updated\n"
    else
        echo_color "already set\n"
    fi
}

check_and_enable_required_services() {
    # For each of the services required for a TaskMAD deployment, check if it 
    # has already been enabled and enable it if required.
    #
    # Return value: ignored (should exit on error)

    for service in "${required_services[@]}"
    do 
        echo_color "Checking if service ${service} is enabled..." 
        if is_gcp_service_enabled "${service}"
        then
            echo_color "already enabled!\n"
        else 
            echo_color "needs to be enabled\n"
            echo_color "> Enabling service %s..." "${service}"
            gcloud services enable "${service}"
            if is_gcp_service_enabled "${service}"
            then
                echo_color "Service enabled!\n"
            else
                echo_color "Failed to enable service!\n" "${RED}"
                exit 1
            fi
        fi
    done
}

does_named_ip_exist() {
    # Check if a reserved IP with a given label already exists
    #
    # $1 = label of a reserved IP
    #
    # Return value: 0 if IP exists, 1 if not

    resp=$(gcloud compute addresses list --global 2> /dev/null)
    is_response_not_empty "${resp}"
}

get_address_for_ip() {
    # Given a label for a reserved IP, return the actual IP
    #
    # $1 = label of a reserved IP
    #
    # Return value: string containing raw IP address, exits on error
    resp=$(gcloud compute addresses list --format='value(address)' --filter=name="${1}")

    if [[ -z "${resp}" ]]
    then
        echo_color "> Failed to retrieve address for IP ${1}!\n" "${RED}"
        exit 1
    fi
}

does_disk_exist() {
    # Check if a disk with the given label already exists
    #
    # $1 = label of the disk
    # $2 = zone to filter on
    #
    # Return value: 0 if disk exists, 1 if not

    resp=$(gcloud compute disks list --zones="${2}" --filter=name="${1}" 2> /dev/null)
    is_response_not_empty "${resp}"
}

setup_external_ips() {
    # Creates (if necessary) the reserved static IPs for the deployment, 
    #
    # Return value: ignored (should exit on error)

    # for each IP
    for d in "${deployments[@]}"
    do 
        ip="${d}[ip]"
        echo_color "> Checking IP with label ${!ip}..."
        if does_named_ip_exist "${!ip}"
        then
            echo_color "already exists!\n"
        else
            echo_color "needs to be created\n"
            echo_color "> Creating IP with label ${!ip}..."
            gcloud compute addresses create --global --quiet --no-user-output-enabled "${!ip}" 
            if does_named_ip_exist "${!ip}"
            then
                echo_color "IP created!\n"
            else
                echo_color "Failed to create IP ${!ip}!\n" "${RED}"
                exit 1
            fi
        fi
    done
}

does_artifact_repository_exist() {
    # Checks if a named artifact repository exists in a selected region+project
    #
    # $1 = project ID
    #
    # Return value: 0 if the repository exists, 1 if not

    # the "name" field has format "projects/PROJECT_ID/locations/REGION/repositories/REPO_NAME"
    full_repo_name="projects/${1}/locations/${region}/repositories/${repo_name}"
    resp=$(gcloud artifacts repositories list --format=yaml --location="${region}" --filter=name="${full_repo_name}" 2> /dev/null)
    is_response_not_empty "${resp}"
}

setup_artifact_repository() {
    # Creates (if necessary) the artifact repository required by the deployment
    #
    # $1 = project ID
    #
    # Return value: ignored (should exit on error)
    echo_color "> Checking if artifact repository ${repo_name} exists in region ${region}..."
    if does_artifact_repository_exist "${1}"
    then
        echo_color "already exists!\n"
    else
        echo_color "needs to be created!\n"
        echo_color "> Creating a new artifact repository ${repo_name} in region ${region} with format docker..."
        gcloud artifacts repositories create --repository-format=docker --location="${region}" "${repo_name}" 2> /dev/null
        if does_artifact_repository_exist "${1}"
        then
            echo_color "repository created!\n"
        else
            echo_color "Failed to create repository!\n" "${RED}"
            exit 1
        fi
    fi
}

build_and_push_local_images() {
    # Builds all the TaskMAD Docker images locally and pushes them to an 
    # artifact repository
    #
    # Return value: ignored (should exit on error)

    echo_color "> Attempting to authenticate with remote Docker repo...\n"
    gcloud auth configure-docker "${docker_repo_id}"
    gcloud auth print-access-token | docker login -u oauth2accesstoken --password-stdin "https://${docker_repo_hostname}"

    echo_color "> Building images\n"
    for d in "${deployments[@]}"
    do
        build_cmd="${d}[docker_build_script]"
        if [[ -n "${!build_cmd}" ]]
        then
            echo_color "> Running build script for ${d}...\n"
            eval "${!build_cmd}" "${script_path}/deploy_gcp_config" "${docker_repo_id}"
        else
            echo_color "> Skipping Docker build for ${d}...\n" "${YELLOW}"
        fi
    done

    echo_color "> All images built and pushed to remote repo!\n"
}

format_disk() {
    # Format a newly-created gcloud disk so it can be used as backing
    # for a volume (they can't be auto-formatted if used in ReadOnly mode
    # it seems)
    #
    #  $1 = disk name
    #
    # Return value: ignored (should exit on error)

    # There isn't a simple command to format a disk, so the best way I've been
    # able to find is to create a small VM instance, attach the disk, format it
    # through that, and then detach it and dispose of the VM again...

    # creating a basic VM
    echo_color " - Creating a VM to format the new disk...\n"
    gcloud compute instances create "${vm_name}" --image-family=debian-11 --image-project=debian-cloud --machine-type=f1-micro --network-tier=STANDARD 2>/dev/null

    # attach the newly created disk
    echo_color " - Attaching disk...\n"
    gcloud compute instances attach-disk "${vm_name}" --disk "${1}" 2>/dev/null

    # check SSH access works (and generate a key silently with --quiet)
    gcloud compute ssh "${vm_name}" --command "ls /" --quiet >/dev/null 2>&1

    # the disk should now be mounted at /dev/sdb, format it
    # TODO does the disk name show up in the OS so the device can be confirmed?
    echo_color " - Formatting the disk...\n"
    gcloud compute ssh "${vm_name}" --command "sudo /sbin/mkfs.ext4 -q /dev/sdb"

    # detach the disk from the VM
    echo_color " - Detaching disk and deleting VM...\n"
    gcloud compute instances detach-disk "${vm_name}" --disk "${1}" 2>/dev/null

    # dispose of VM
    gcloud compute instances delete "${vm_name}" --quiet 2>/dev/null

    echo_color " - Disk was successfully formatted!\n"
}

copy_keys_to_disk() {
    # TODO 
    # copying keys could be done something like this while the disk is attached to a VM
    #
    # gcloud compute ssh testvm --command "sudo mkdir /mnt/pd && sudo mount /dev/sdb /mnt/pd && sudo chmod -R a+rwx /mnt/pd"
    # gcloud compute scp server-cert.pem server-key.pem tempvm:/mnt/pd/keys
    # gcloud compute ssh testvm --command "sudo chmod a+r /mnt/pd/* && umount /mnt/pd"
    0
}

setup_disks() {
    # Creates (if necessary) the disks used to back the persistent volumes for 
    # the deployments
    #
    # Return value: ignored (should exit on error)

    echo_color "> Creating disks (if necessary)\n"
    for d in "${deployments[@]}"
    do
        # check if this deployment needs a disk at all
        disk_name="${d}[disk_name]"
        if [[ -n "${!disk_name}" ]]
        then
            disk_size="${d}[disk_size_gb]"

            # check if the disk already exists and leave it alone if so
            if does_disk_exist "${!disk_name}" "${zone}"
            then
                echo_color " - Disk ${!disk_name} already exists\n" "${YELLOW}"
            else
                echo_color " - Creating disk ${!disk_name} with size ${!disk_size}GB\n"
                gcloud compute disks create "${!disk_name}" --size="${!disk_size}GB" --zone="${zone}" >/dev/null 2>&1 

                format_disk "${!disk_name}"
            fi
        fi
    done
}

is_cluster_up() {
    # Check the status of a K8S cluster
    #
    #   $1 = cluster name
    #
    # Return value: 0 if cluster exists and has status "RUNNING", 1 if not 

    # query the cluster status 
    resp=$(gcloud container clusters list --filter "status=RUNNING AND name=${1}")
    is_response_not_empty "${resp}"
}

setup_cluster() {
# Creates a new K8S cluster on GCP to host a deployment
    #
    #   $1 = name of deployment 
    #
    # Return value: ignored (should exit on error)

    cluster_name="${d}[cluster_name]"
    node_count="${d}[node_count]"
    boot_disk_size_gb="${d}[boot_disk_size_gb]"
    machine_type="${d}[machine_type]"

    # start a cluster 
    if [[ -z "${zone}" ]]
    then
        # regional cluster (multi-zone, more expensive!)
        echo_color "> Creating a REGIONAL cluster in region ${region}\n"
        gcloud container clusters create --machine-type="${!machine_type}" --num-nodes="${!node_count}" \
            --disk-size="${!boot_disk_size_gb}" --async --region="${region}" "${!cluster_name}"
    else
        # zonal cluster, zone must be in the region used for other resources
        echo_color "> Creating a ZONAL cluster in region ${zone}\n"
        gcloud container clusters create --machine-type="${!machine_type}" --num-nodes="${!node_count}" \
            --disk-size="${!boot_disk_size_gb}" --async --zone="${zone}" "${!cluster_name}"
    fi

    echo_color "> Cluster deployment initiated (may take several minutes to complete!)\n"
    echo_color "  (you may wish to run deploy_gcp.sh clustercheck to monitor the cluster creation)\n"
}

setup_clusters() {
    # Go through each of the required deployments, check if a cluster already
    # exists to host it, and create one if not
    #
    # Return value: ignored (should exit on error)

    echo_color "> Setting up clusters for each deployment\n"
    for d in "${deployments[@]}"
    do
        cluster_name="${d}[cluster_name]"
        # check if there's a cluster already running with the expected name
        echo_color "> Checking for existing cluster with name ${!cluster_name}..."
        resp=$(gcloud container clusters list --filter "name=${!cluster_name}" 2> /dev/null)
        if is_response_not_empty "${resp}"
        then
            echo_color "cluster found!\n"
        else
            echo_color "no existing cluster found, starting one!\n"
            setup_cluster "${d}"
        fi
    done
}

setup_kubectl_for_deployment() {
    # Configure K8S credentials for a particular cluster
    # 
    #   $1 = name of cluster
    #   $2 = region
    #   $3 = zone
    #
    #   Return value: ignored (should exit on error)
    echo_color "> Configuring kubectl authentication credentials\n"
    if [[ -z "${3}" ]]
    then
        gcloud container clusters get-credentials "${1}" --region "${2}"
    else
        gcloud container clusters get-credentials "${1}" --zone "${3}"
    fi
}

setup_deployments() {
    # Creates deployments, expecting to find clusters etc already existing.
    #
    # Return value: ignored (should exit on error)

    for d in "${deployments[@]}"
    do
        cluster_name="${d}[cluster_name]"
        
        # check the cluster at least is actually available
        if ! is_cluster_up "${!cluster_name}"
        then
            echo_color "Aborting ${d} deployment, target cluster ${!cluster_name} is not available/does not exist!" "${RED}\n"
            exit 1
        fi

        setup_kubectl_for_deployment "${!cluster_name}" "${region}" "${zone}"

        setup_deployment "${d}"
    done
}

setup_deployment() {
    # Creates a new deployment (removing any existing one first) of the 
    # selected TaskMAD component (e.g. "core").
    #
    # $1 = deployment name
    # 
    # Return value: ignored (should exit on error)

    # # retrieve the IP addresses already created
    # core_ip_addr=$(get_address_for_ip "${3}")
    # chat_ip_addr=$(get_address_for_ip "${4}")

    declare -r params="${deployments[${1}]}"
    declare -r deployment_name="${params}[deployment_name]"
    declare -r deployment_script="${params}[deployment_script]"

    echo_color "> Setting up deployment ${!deployment_name} for component ${1}...\n"
    
    # echo_color "> Deleting any existing deployment named ${depl}...\n"
    # resp=$(kubectl get deployment --field-selector=metadata.name="${depl}" 2> /dev/null)
    # if is_response_not_empty "${resp}"
    # then
    #     kubectl delete deployment "${depl}"
    #     sleep 10
    # fi

    # run the separate deployment script for this component, which should take
    # care of applying the required files via kubectl
    echo_color "> Running deployment script ${!deployment_script}\n"

    # script expects arguments: <path to config file> <name of deployment> <image repo>
    eval "${!deployment_script}" "${script_path}/deploy_gcp_config" "${1}" "${docker_repo_id}"

    echo_color "> Deployment of ${!deployment_name} completed!\n"
}

cleanup_resources() {
    # Attempts to release all the resources used in a deployment
    #
    # delete things in reverse order
    #  - delete the whole cluster to remove the volumes and deployments
    #  - delete the disk used for persistent storage
    #  - delete artifact repo to get rid of the images inside
    #  - delete the reserved IPs 
    #  - delete the temporary VM used to format disks (might be left running if an error occurs)

    echo_color "*** WARNING: this will attempt to delete the following resources\n" "${RED}"
    echo_color " - Repository:\t ${repo_name}\n" "${YELLOW}"
    for d in "${deployments[@]}"
    do
        cluster_name="${d}[cluster_name]"
        disk_name="${d}[disk_name]"
        ip="${d}[ip]"
        echo_color " - Cluster:\t ${!cluster_name}\n" "${YELLOW}"
        if [[ -n "${!disk_name}" ]]
        then
            echo_color " - Disk:\t ${!disk_name}\n" "${YELLOW}"
        fi
        echo_color " - Static IP:\t ${!ip}\n" "${YELLOW}"
    done
    echo_color " - VM instance:\t ${vm_name}\n" "${YELLOW}"

    echo ""
    read -p "Do you wish to continue (y/n)? " -n 1 -r
    echo
    if [[ "${REPLY}" =~ ^[Yy]$ ]]
    then
        echo_color "> Deleting resources...\n" 

        for d in "${deployments[@]}"
        do
            cluster_name="${d}[cluster_name]"
            disk_name="${d}[disk_name]"
            ip="${d}[ip]"

            echo_color "> Deleting resources for deployment ${d}...\n"

            echo_color " - Deleting K8S cluster ${!cluster_name} (this can take a few minutes to complete once started!)\n"
            if ! gcloud container clusters delete "${!cluster_name}" --quiet --async 2> /dev/null
            then
                echo_color " ! Failed to delete cluster (may already have been deleted or still provisioning)\n" "${YELLOW}"
            fi

            if [[ -n "${!disk_name}" ]]
            then
                echo_color " - Deleting K8S disk ${!disk_name}\n"
                if ! gcloud compute disks delete "${!disk_name}" --zone="${zone}" --quiet 2> /dev/null
                then
                    echo_color " ! Failed to delete disk (may already have been deleted)\n" "${YELLOW}"
                fi
            else
                echo_color " - No K8S disk defined for ${d}\n"
            fi

            echo_color " - Deleting reserved IP ${!ip}\n"
            if ! gcloud compute addresses delete "${!ip}" --global --quiet 2> /dev/null
            then 
                echo_color " ! Failed to delete reserved IP (may already have been deleted)\n" "${YELLOW}"
            fi
        done

        echo_color "> Deleting artifact repo ${repo_name}\n"
        if ! gcloud artifacts repositories delete "${repo_name}" --location="${region}" --quiet 2> /dev/null
        then
            echo_color "> Failed to delete artifact repo (may already have been deleted)\n" "${YELLOW}"
        fi

        echo_color "> Deleting temporary VM instances ${vm_name}\n"
        if ! gcloud compute instances delete "${vm_name}" --quiet 2> /dev/null
        then
            echo_color "> Failed to delete VM instance (may already have been deleted)\n" "${YELLOW}"
        fi
    else
        echo_color "No resources have been deleted.\n"
        exit 0
    fi
}

check_gcloud() {
    # check for gcloud binary
    if ! command -v gcloud &> /dev/null 
    then
        printf "gcloud binary not installed or not on PATH - you might need to install the Google Cloud SDK"
        exit 1
    fi
}

check_clusters() {
    # Continually check the status of each cluster until interrupted or
    # all 4 exist and are "RUNNING"
    #
    # Return code: ignored (should exit on error)

    declare -r interval=20
    echo_color "Showing cluster states every ${interval}s!\n\n"

    while true
    do
        declare num_up=0
        declare total=0

        echo_color "Current state:\n"
        for d in "${deployments[@]}"
        do
            cluster_name="${d}[cluster_name]"
            if [[ -z "${cluster_name}" ]]
            then
                continue
            else
                total=$((total+1))
            fi

            if is_cluster_up "${!cluster_name}"
            then
                echo_color " + ${!cluster_name} is fully available\n" "${GREEN}"
                num_up=$((num_up+1))
            else
                echo_color " - ${!cluster_name} is not running\n" "${YELLOW}"
            fi
        done

        if [[ ${num_up} -eq ${total} ]]
        then
            echo_color "All clusters are in the 'RUNNING' state!\n" "${GREEN}"
            break
        fi

        echo_color "(Ctrl-C to exit)\n\n"
        sleep 20
    done
}

###

# Check if gcloud binary is available 
check_gcloud
declare gcloud_project_id
gcloud_project_id=$(gcloud config get-value project)
echo_color "> Using gcloud project ID: ${gcloud_project_id}\n"

# source user-defined variables (VSCode: if this generates a shellcheck warning 
# add "-x" as a custom argument in the extension settings)
# shellcheck source=gcp_deployment/deploy_gcp_config
source "${script_path}/deploy_gcp_config"

declare -r docker_repo_id="${region:?}-docker.pkg.dev/${gcloud_project_id}/${repo_name:?}"
declare -r docker_repo_hostname="${region:?}-docker.pkg.dev"

pushd "${script_path}" > /dev/null

# require a parameter to be passed to perform any actions
if [[ $# -ne 1 ]]
then
    echo "Usage: deploy_gcp.sh <create|clustercheck|deploy|cleanup>"
    echo ""
    echo "Available commands:"
    echo -e "   ${GREEN}create${NC}: set up all required GCP resources (IPs, image repo, clusters)."
    echo -e "   Also builds and pushes all Docker images to the image repo. Does NOT"
    echo -e "   wait for cluster creation to complete (this can take some time!)"
    echo -e "   ${GREEN}clustercheck${NC}: monitor the status of newly-created clusters until all"
    echo -e "   of them have reaching RUNNING status and are ready for deployments."
    echo -e "   ${GREEN}deploy${NC}: create the various TaskMAD deployments on the clusters."
    echo -e "   ${GREEN}cleanup${NC}: delete all created GCP resources."
    exit 0
fi

# cleanup resources from previous deployments if "cleanup" parameter is used
if [[ "${1}" == "cleanup" ]]
then
    cleanup_resources 
elif [[ "${1}" == "clustercheck" ]]
then
    check_clusters
elif [[ "${1}" == "deploy" ]]
then
    # launch the deployments in the now-running clusters
    setup_deployments
elif [[ "${1}" == "create" ]]
then
    # allow only selected steps to be executed using phases variable

    # check for the required GCP services being enabled and enable them if necessary
    if is_phase_enabled "services"
    then
        check_and_enable_required_services
    fi

    # check the project is set to PREMIUM network tier
    if is_phase_enabled "tier"
    then
        check_and_update_network_tier
    fi

    # create an artifact repository to host all the deployment Docker images in the desired region
    if is_phase_enabled "repo"
    then
        setup_artifact_repository "${gcloud_project_id}"
    fi

    # create the external IPs required for the deployment in the desired region using the specified network tier
    if is_phase_enabled "ips"
    then 
        setup_external_ips 
    fi

    # build all local Docker images and push them to the newly created repo
    if is_phase_enabled "images"
    then
        build_and_push_local_images 
    fi

    # create the disks required for persistent storage 
    if is_phase_enabled "disks"
    then
        setup_disks 
    fi

    # (if necessary) create a Kubernetes cluster to host each deployment using the supplied parameters
    if is_phase_enabled "clusters"
    then
        setup_clusters 
    fi
else
    echo_color "Unrecognised parameter \"${1}\"\n"
    exit 1
fi

popd > /dev/null
exit 0
