#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Generate a deployment template for the woz component. Assumes the
# target cluster already exists and kubectl auth configuration already
# completed. 
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = component name (should be "woz")
#   $3 = image repo URL

if [[ $# -ne 3 ]]
then
    echo "Usage: create_woz_deployment.sh <config file path> <component name> <image repo URL>"
    echo "       (this script is intended to be called by deploy_gcp.sh)"
    exit 1
fi

declare -r config_file="${1}"
declare -r component_name="${2}"
declare -r image_repo="${3}"

if [[ "${component_name}" != "woz" ]]
then
    echo "Expected component=woz, got component=${component_name}!"
    exit 1
fi

# shellcheck disable=1090
source "${config_file}"

# parameters for this component
params="${component_name}"
declare -r ip_name="${params}[ip]"
declare -r deployment_name="${params}[deployment_name]"
declare -r service_name="${params}[service_name]"
declare -r cert_name="${params}[cert_name]"
declare -r domain="${params}[domain]"

# (based on cloudbuild.yaml)
# declare -r CONFIG_PATH="../../WoZStudy"
# these are the files still located in the original woz repo as they
# don't require any templating
declare -r CONFIG_PATH="../../WoZStudy"
declare -r FRONTEND_CONFIG_FILE="./template_files/frontend_config.yaml"
# TODO keeping the "template" versions of these files in the 
# TaskMAD repo for now so they can be included in the PR with
# everything else
declare -r K8_FILE="./template_files/woz/woz_deployment_nginx-template.yaml"
declare -r K8_INGRESS_FILE="./template_files/woz/woz_managed_cert_ingress-template.yaml"
declare -r CERT_FILE="./template_files/managed_cert.yaml"
#
# 1. Managed certificate
sed < "${CERT_FILE}" \
    -e "s/CERT_NAME/${!cert_name}/g" \
    -e "s/DOMAIN/${!domain}/g" | kubectl apply -f -

# 2. Frontend (no templating required)
kubectl apply -f "${FRONTEND_CONFIG_FILE}"

# 3. Service and pods.

# NOTE: in the last expression we're replacing the usual delimiter to avoid having
# to escape the "/" chars in the image_repo string
sed < "${K8_FILE}" \
    -e "s/SERVICE_NAME/${!service_name}/g" \
    -e "s/DEPLOYMENT_NAME/${!deployment_name}/g" \
    -e "s|IMAGE_REPO|${image_repo}|g" | kubectl apply -f -

# 4. Ingress
#   Values to substitute in here:
#    - SERVICE_NAME
#    - IP_NAME
#    - CERT_NAME
sed < "${K8_INGRESS_FILE}" \
    -e "s/SERVICE_NAME/${!service_name}/g" \
    -e "s/CERT_NAME/${!cert_name}/g" \
    -e "s/IP_NAME/${!ip_name}/g" | kubectl apply -f -


# wait for the deployment to complete before exiting
kubectl rollout status deployment/"${!deployment_name}" --watch=true

exit 0
