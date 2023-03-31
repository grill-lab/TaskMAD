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

# avoid using deprecated kubectl auth method, see 
# https://cloud.google.com/blog/products/containers-kubernetes/kubectl-auth-changes-in-gke
export USE_GKE_GCLOUD_AUTH_PLUGIN=True

declare deployment_ok=true

exit_handler() {
    # Handler for the bash EXIT signal. The script is set up to exit
    # if a command returns an unsuccessful status code, but depending
    # on when/how that happens it might not be immediately obvious to 
    # the user that something went wrong. 
    #
    # This handler just uses the exit_ok variable to check if the
    # exit event happened prematurely due to an error, and reports
    # that to the user (currently only for the deployment phase, 
    # as that's by far the most complex).
    # 
    # TODO: some of the steps redirect stderr to /dev/null, should
    # probably make sure it's visible instead to help debug problems
    if [[ "${deployment_ok}" == false ]]
    then
        echo -e "\n\n*** The selected TaskMAD deployment steps were NOT successfully completed!"
        echo "*** Check the output above for error messages"
    else
        echo -e "\n\nThe selected TaskMAD deployment steps were successfully completed!"
    fi
}

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

    echo_color "> Checking default network tier is PREMIUM..."
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
        echo_color "> Checking if service ${service} is enabled..." 
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

    resp=$(gcloud compute addresses list --global --filter=name="${1}" 2> /dev/null)
    is_response_not_empty "${resp}"
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
            eval "${!build_cmd}" "${script_path}/deploy_gcp_config" "${docker_repo_id}" "build"
        else
            echo_color "> Skipping Docker build for ${d}...\n" "${YELLOW}"
        fi
    done

    echo_color "> All images built and pushed to remote repo!\n"
}

setup_disks() {
    # Creates (if necessary) the disks used to back the persistent volumes for 
    # the deployments
    #
    # Return value: ignored (should exit on error)

    echo_color "> Creating disks (if necessary) in zone ${zone}\n"
    for d in "${deployments[@]}"
    do
        # check if this deployment needs a disk at all
        disk_name="${d}[disk_name]"
        if [[ -n "${!disk_name}" ]]
        then
            disk_size="${d}[disk_size_gb]"
            local_files_path="${d}[local_files_path]"
            remote_files_path="${d}[remote_files_path]"

            # check if the disk already exists and leave it alone if so
            local is_new_disk=false
            if does_disk_exist "${!disk_name}" "${zone}"
            then
                echo_color " - Disk ${!disk_name} already exists\n" "${YELLOW}"
            else
                echo_color " - Creating disk ${!disk_name} with size ${!disk_size}GB\n"
                gcloud compute disks create "${!disk_name}" --size="${!disk_size}GB" --zone="${zone}" >/dev/null 2>&1 
                is_new_disk=true
            fi

            # call out to copy_files_to_gcp_disk.sh to handle the rest of the process.
            # parameters are: VM name, disk name, zone, source path, remote path, is_new_disk
            echo_color "> Copying files to new disk...\n"
            eval ./copy_files_to_gcp_disk.sh "${vm_name}-${d}" "${!disk_name}" "${zone}" "${!local_files_path}" "${!remote_files_path}" "${is_new_disk}"
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
        if [[ -z "${zone}" ]]
        then
            resp=$(gcloud container clusters list --filter "name=${!cluster_name}" --region "${region}" 2> /dev/null)
        else
            resp=$(gcloud container clusters list --filter "name=${!cluster_name}" --zone "${zone}" 2> /dev/null)
        fi

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
    echo_color "> Configuring kubectl authentication credentials for cluster: ${1}\n"
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

        echo_color "> Beginning setup of deployment ${d} on cluster ${!cluster_name}\n"
        
        # check the cluster at least is actually available
        if ! is_cluster_up "${!cluster_name}"
        then
            echo_color "Aborting ${d} deployment, target cluster ${!cluster_name} is not yet available/does not exist!\n" "${RED}"
            exit 1
        fi

        setup_kubectl_for_deployment "${!cluster_name}" "${region}" "${zone}"

        setup_deployment "${d}"
    done

    echo_color "> Selected deployments have been created.\n"
    echo_color "\n   NOTE: you will need to manually configure DNS records for the domains for each deployment.\n"
    echo_color "   You can view the static IPs to point the domains at by running the command:\n"
    echo_color "      ./deploy_gcp.sh domains\n"
}

setup_deployment() {
    # Creates a new deployment (removing any existing one first) of the 
    # selected TaskMAD component (e.g. "core").
    #
    # $1 = deployment name
    # 
    # Return value: ignored (should exit on error)

    declare -r params="${1}"
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

show_domain_info() {
    # Show IP address and configured domain for each deployment
    #
    # Return value: ignored (should exit on error)

    # TODO use kubectl config current-context to save and then (below) restore
    # the current context
    
    for d in "${deployments[@]}"
    do
        declare ip="${d}[ip]"
        declare domain="${d}[domain]"
        declare cluster_name="${d}[cluster_name]"
        declare cert_name="${d}[cert_name]"
        declare ip_addr
        ip_addr=$(gcloud compute addresses list --format='value(address)' --filter=name="${!ip}")

        if [[ -z "${ip_addr}" ]]
        then
            echo_color "> Failed to retrieve address for IP ${!ip}!\n" "${RED}"
            exit 1
        fi

        setup_kubectl_for_deployment "${!cluster_name}" "${region}" "${zone}"
    
        echo_color "> Deployment ${d}\n"
        echo_color "    Point domain ${YELLOW}'${!domain}'${NC} to ${YELLOW}${ip_addr}${NC}\n"
        declare cert_status
        cert_status=$(kubectl get managedcertificate --field-selector metadata.name=="${!cert_name}" --no-headers=true --ignore-not-found | awk '{ print $3; }' 2>&1)
        if [[ "${cert_status}" == "Active" ]]
        then
            echo_color "    Current certificate status: Active (deployment should be accessible through the domain)\n"
        else
            if [[ "${cert_status}" == "" ]]
            then
                echo_color "    Certificate has not been created yet (is the deployment created?)\n" "${RED}"
            else
                echo_color "    ${GREEN}Current certificate status: ${YELLOW}${cert_status} ${GREEN}(check DNS configuration, wait 60 mins from cert creation)\n"
            fi
        fi

        echo_color "\n"
    done
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
    #  - delete managed SSL certs

    # retrieve list of SSL certificates currently existing
    # TODO could be a function
    declare -A certinfo
    certinfo=()

    while read -r name domain;
    do
        if [[ "${name}" == "NAME" ]]
        then
            # skip header line
            continue
        fi
        if [[ -z "${name}" ]]
        then
            # skip empty output lines
            continue
        fi
        certinfo["${name}"]="${domain}"
    done <<<$(gcloud compute ssl-certificates list --format="table(name, managed.domains[0])")

    echo_color "*** WARNING: this will attempt to delete the following resources\n" "${RED}"
    for d in "${deployments[@]}"
    do
        cluster_name="${d}[cluster_name]"
        disk_name="${d}[disk_name]"
        ip="${d}[ip]"

        echo_color "[${d}]\n"
        echo_color " - Cluster:\t ${!cluster_name}\n" "${YELLOW}"
        if [[ -n "${!disk_name}" ]]
        then
            echo_color " - Disk:\t ${!disk_name}\n" "${YELLOW}"
            echo_color " - VM instance:\t ${vm_name}-${d}\n" "${YELLOW}"
        fi
        echo_color " - Static IP:\t ${!ip}\n" "${YELLOW}"
        echo_color "\n"
    done
    echo_color "[other]\n"
    echo_color " - Repository:\t ${repo_name}\n" "${YELLOW}"
    echo_color " - Managed SSL certs: \t ${#certinfo[@]} certs found\n" "${YELLOW}"
    echo_color "\n"

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
            if ! gcloud container clusters delete "${!cluster_name}" --zone="${zone}" --quiet --async 2> /dev/null
            then
                echo_color " ! Failed to delete cluster (may already have been deleted or still provisioning)\n" "${YELLOW}"
            fi

            if [[ -n "${!disk_name}" ]]
            then
                echo_color " - Deleting K8S disk ${!disk_name}\n"
                if ! gcloud compute disks delete "${!disk_name}" --zone="${zone}" --quiet 2> /dev/null
                then
                    echo_color " ! Failed to delete disk (may already have been deleted OR still in use by a cluster!)\n" "${YELLOW}"
                fi
            else
                echo_color " - No K8S disk defined for ${d}\n"
            fi

            echo_color " - Deleting reserved IP ${!ip}\n"
            if ! gcloud compute addresses delete "${!ip}" --global --quiet 2> /dev/null
            then 
                echo_color " ! Failed to delete reserved IP (may already have been deleted)\n" "${YELLOW}"
            fi

            echo_color " - Deleting temporary VM instance ${vm_name}-${d}\n"
            if ! gcloud compute instances delete "${vm_name}-${d}" --zone="${zone}" --quiet 2> /dev/null
            then
                echo_color " - Failed to delete VM instance (may already have been deleted)\n" "${YELLOW}"
            fi

            echo_color "\n"
        done

        echo_color "> Deleting artifact repo ${repo_name}\n"
        if ! gcloud artifacts repositories delete "${repo_name}" --location="${region}" --quiet 2> /dev/null
        then
            echo_color "> Failed to delete artifact repo (may already have been deleted)\n" "${YELLOW}"
        fi

        echo_color "> Deleting SSL certs...\n"
        for name in "${!certinfo[@]}"
        do 
            echo_color "\tDeleting cert ID ${name} for ${certinfo[${name}]}\n" "${YELLOW}"
            if ! gcloud compute ssl-certificates delete "${name}" --global --quiet 2> /dev/null
            then
                echo_color "> Failed to delete this certificate!\n" "${YELLOW}"
            fi
        done

    else
        echo_color "No resources have been deleted.\n"
        exit 0
    fi
}

clean_docker_images() {
    for d in "${deployments[@]}"
    do
        build_cmd="${d}[docker_build_script]"
        echo_color "> Removing local images for ${d}\n"
        eval "${!build_cmd}" "${script_path}/deploy_gcp_config" "${docker_repo_id}" "clean"
    done
}

check_gcloud() {
    # check for gcloud binary
    if ! command -v gcloud &> /dev/null 
    then
        echo "gcloud binary not installed or not on PATH - you might need to install the Google Cloud SDK"
        exit 1
    fi
}

check_clusters() {
    # Continually check the status of each cluster until interrupted or
    # all 4 exist and are "RUNNING"
    #
    # Return code: ignored (should exit on error)

    declare -r interval=20
    echo_color "Showing cluster states every ${interval}s\n\n"

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

# require a parameter to be passed to perform any actions
if [[ $# -lt 1 ]]
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
    echo -e "   ${GREEN}manage <deployment>${NC}: setup kubectl to interact with the selected deployment."
    echo -e "   ${GREEN}domains${NC}: display the IPs you will need to point your domains at."
    echo -e "   ${GREEN}cleanup${NC}: delete all created GCP resources."
    echo -e "   ${GREEN}dockercleanup${NC}: delete all local Docker images for the deployments."
    exit 0
fi

pushd "${script_path}" > /dev/null

if [[ "${1}" == "cleanup" ]]
then
    # cleanup resources from previous deployments if "cleanup" parameter is used
    cleanup_resources 
elif [[ "${1}" == "clustercheck" ]]
then
    # monitor status of clusters until they're all running
    check_clusters
elif [[ "${1}" == "deploy" ]]
then
    # launch the deployments in the now-running clusters
    setup_deployments
elif [[ "${1}" == "create" ]]
then
    # hook up the exit_handler function above to the bash "EXIT" signal, 
    # so it will be called when the script exits (for any reason)
    trap exit_handler EXIT

    # see exit_handler above
    deployment_ok=false

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

    # for exit_handler, see above
    deployment_ok=true
elif [[ "${1}" == "manage" ]]
then
    # manage a cluster with kubectl
    if [[ $# -ne 2 ]]
    then
        echo_color "You must select a deployment to manage, e.g. './deploy_gcp.sh manage core'\n" "${RED}"
        exit 1
    fi
    declare -r params="${2}"
    declare -r cluster_name="${params}[cluster_name]"
    
    setup_kubectl_for_deployment "${!cluster_name}" "${region}" "${zone}"
elif [[ "${1}" == "domains" ]]
then
    show_domain_info
elif [[ "${1}" == "dockercleanup" ]]
then
    clean_docker_images
else
    echo_color "Unrecognised parameter \"${1}\"\n"
    exit 1
fi

popd > /dev/null
exit 0
