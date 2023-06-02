#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the core component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = either "build" to build images, or "clean" to remove all local images'

if [[ $# -ne 2 ]]
then
    echo "build_images_core.sh <config file path> <build/clean>"
    exit 1
fi

declare -r config_file="${1}"

# shellcheck disable=1090
source "${config_file}"

script_path="$( dirname -- "$0"; )"

if [[ "${2}" == "clean" ]]
then
    for img in grpc-server envoy grpc-health-proxy
    do
        docker rmi -f "${img}":latest 2> /dev/null
    done
elif [[ "${2}" == "build" ]]
then
    pushd "${script_path}/../agent-dialogue-core"

    # couple of extra steps here:
    #   1. create a copy of search_api_config.json with the correct search API endpoint
    declare -r domain="${search[domain]}"
    sed < "../gcp_deployment/template_files/search/search_api_config-template.json" \
        -e "s/SEARCH_API_ENDPOINT/${domain}/g" > search_api_config.json
    #   2. override the default config file URL with the one defined in deploy_gcp_config
    docker build --build-arg config_url="${config_url}" -f Dockerfile -t grpc-server:latest .

    docker build -f grpc_health_proxy.Dockerfile -t grpc-health-proxy:latest .
    popd

    pushd "${script_path}/../config"
    docker build --build-arg envoy_config_file="${envoy_config_file}" --build-arg envoy_ssl_cert="${envoy_ssl_cert}" --build-arg envoy_ssl_privkey="${envoy_ssl_privkey}" -f envoy_updated.Dockerfile -t envoy:latest .
    popd

else
    echo "Unknown argument: ${2} (expected either 'build' or 'clean')"
    exit 1
fi
