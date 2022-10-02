#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Generate a deployment template for the chat component. Assumes the
# target cluster already exists and kubectl auth configuration already
# completed. 
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = component name (should be "chat")
#   $3 = image repo URL

if [[ $# -ne 3 ]]
then
    echo "Usage: create_chat_deployment.sh <config file path> <component name> <image repo URL>"
    echo "       (this script is intended to be called by deploy_gcp.sh)"
    exit 1
fi

declare -r config_file="${1}"
declare -r component_name="${2}"
declare -r image_repo="${3}"

if [[ "${component_name}" != "chat" ]]
then
    echo "Expected component=chat, got component=${component_name}!"
    exit 1
fi

# shellcheck disable=1090
source "${config_file}"

# parameters for this component
params="${component_name}"
declare -r ip_name="${params}[ip]"
declare -r deployment_name="${params}[deployment_name]"
declare -r service_name="${params}[service_name]"

# (based on cloudbuild.yaml)
declare -r CONFIG_PATH="../agent-dialogue-ui/"
declare -r FRONTEND_CONFIG_FILE="frontend_config.yaml"
declare -r K8_INGRESS_FILE="chat_managed_cert_ingress-template.yaml"
declare -r K8_FILE="chat_deployment_nginx-template.yaml"

pushd "${CONFIG_PATH}" > /dev/null

# 1. Frontend
kubectl apply -f "${FRONTEND_CONFIG_FILE}"

# 2. Service and pods.

# NOTE: in the last expression we're replacing the usual delimiter to avoid having
# to escape the "/" chars in the image_repo string
sed < "${K8_FILE}" \
    -e "s/SERVICE_NAME/${!service_name}/g" \
    -e "s/DEPLOYMENT_NAME/${!deployment_name}/g" \
    -e "s|IMAGE_REPO|${image_repo}|g" | kubectl apply -f -

# 3. Ingress
#   Values to substitute in here:
#    - SERVICE_NAME
#    - IP_NAME
sed < "${K8_INGRESS_FILE}" \
    -e "s/SERVICE_NAME/${!service_name}/g" \
    -e "s/IP_NAME/${!ip_name}/g" | kubectl apply -f -

popd > /dev/null

# wait for the deployment to complete before exiting
kubectl rollout status deployment/"${!deployment_name}" --watch=true

exit 0
