#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the search API component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = either "build" to build+push images, or "clean" to remove all local images'

if [[ $# -ne 2 ]]
then
    echo "build_images_searchapi.sh <config file path> <build/clean>"
    exit 1
fi

# declare -r config_file="${1}"

script_path="$( dirname -- "$0"; )"

if [[ "${2}" == "clean" ]]
then
    docker rmi -f search-api:latest 2> /dev/null
elif [[ "${2}" == "build" ]]
then
    # TODO: update when repos are merged
    # currently assumes TaskMAD and search API repos are in the same parent directory
    pushd "${script_path}/../../GroundedKnowledgeInterface/api"

    docker build -f Dockerfile -t search-api:latest .

    popd
else
    echo "Unknown argument: ${2} (expected either 'build' or 'clean')"
    exit 1
fi
