#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Generate a deployment template for the search component. Assumes the
# target cluster already exists and kubectl auth configuration already
# completed. 
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = component name (should be "search")
#   $3 = image repo URL

if [[ $# -ne 3 ]]
then
    echo "Usage: create_search.sh <config file path> <component name> <image repo URL>"
    echo "       (this script is intended to be called by deploy_gcp.sh)"
    exit 1
fi

declare -r config_file="${1}"
declare -r component_name="${2}"
declare -r image_repo="${3}"

if [[ "${component_name}" != "search" ]]
then
    echo "Expected component=search, got component=${component_name}!"
    exit 1
fi

# shellcheck disable=1090
source "${config_file}"

# parameters for this component
params="${component_name}"
declare -r ip_name="${params}[ip]"
declare ip_addr
ip_addr=$(gcloud compute addresses list --format='value(address)' --filter=name="${!ip_name}")
if [[ -z "${ip_addr}" ]]
then
    echo "ERROR: Failed to retrieve address for IP ${!ip_name}!"
    exit 1
fi
declare -r deployment_name="${params}[deployment_name]"
declare -r pvc_name="${params}[pvc_name]"
declare -r pv_name="${params}[pvc_name]"
declare -r service_name="${params}[service_name]"
declare -r disk_name="${params}[disk_name]"
declare -r disk_size_gb="${params}[disk_size_gb]"

# (based on cloudbuild.yaml)
# declare -r CONFIG_PATH="../../GroundedKnowledgeInterface/api"
# TODO keeping the "template" versions of these files in the 
# TaskMAD repo for now so they can be included in the PR with
# everything else
declare -r K8_FILE="./template_files/search/api_deployment-template.yaml"
declare -r PV_FILE="./template_files/search/persistent_volume_k8-template.yaml"

# pushd "${CONFIG_PATH}" > /dev/null

# 1. Persistent volume + claim
sed < "${PV_FILE}" \
    -e "s/DISK_NAME/${!disk_name}/g" \
    -e "s/PVC_NAME/${!pvc_name}/g" \
    -e "s/PV_NAME/${!pv_name}/g" \
    -e "s/DISK_SIZE/${!disk_size_gb}/g" | kubectl apply -f -

# 2. Service and pods.

# NOTE: in the last expression we're replacing the usual delimiter to avoid having
# to escape the "/" chars in the image_repo string
sed < "${K8_FILE}" \
    -e "s/SERVICE_NAME/${!service_name}/g" \
    -e "s/DEPLOYMENT_NAME/${!deployment_name}/g" \
    -e "s/PVC_NAME/${!pvc_name}/g" \
    -e "s/IP_ADDR/${ip_addr}/g" \
    -e "s|IMAGE_REPO|${image_repo}|g" | kubectl apply -f -

# popd > /dev/null

# wait for the deployment to complete before exiting
kubectl rollout status deployment/"${!deployment_name}" --watch=true

exit 0
