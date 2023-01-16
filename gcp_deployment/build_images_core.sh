#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the core component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = remote docker repo to tag images for
#   $3 = either "build" to build+push images, or "clean" to remove all local images'

if [[ $# -ne 3 ]]
then
    echo "build_images_core.sh <config file path> <remote-repo> <build/clean>"
    exit 1
fi

declare -r config_file="${1}"
declare -r remote="${2}"

# shellcheck disable=1090
source "${config_file}"

script_path="$( dirname -- "$0"; )"

if [[ "${3}" == "clean" ]]
then
    for img in grpc-server envoy grpc-health-proxy
    do
        docker rmi -f "${img}":latest 2> /dev/null
        docker rmi -f "${remote}"/"${img}":latest 2> /dev/null
    done
elif [[ "${3}" == "build" ]]
then
    pushd "${script_path}/../agent-dialogue-core"

    # couple of extra steps here:
    #   1. create a copy of search_api_config.json with the correct search API endpoint
    declare -r domain="${search[domain]}"
    sed < "../gcp_deployment/template_files/search/search_api_config-template.json" \
        -e "s/SEARCH_API_ENDPOINT/${domain}/g" > search_api_config.json
    #   2. override the default config file URL with the one defined in deploy_gcp_config
    docker build --build-arg config_url="${config_url}" -f Dockerfile -t grpc-server:latest .
    docker tag grpc-server:latest "${remote}"/grpc-server:latest

    docker build -f grpc_health_proxy.Dockerfile -t grpc-health-proxy:latest .
    docker tag grpc-health-proxy:latest "${remote}"/grpc-health-proxy:latest
    popd

    pushd "${script_path}/../config"
    docker build -f envoy_updated.Dockerfile -t envoy:latest .
    docker tag envoy:latest "${remote}"/envoy:latest
    popd

    docker push "${remote}"/grpc-server:latest
    docker push "${remote}"/envoy:latest
    docker push "${remote}"/grpc-health-proxy:latest
else
    echo "Unknown argument: ${3} (expected either 'build' or 'clean')"
    exit 1
fi
