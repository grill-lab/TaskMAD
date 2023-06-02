#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the wizard component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = either "build" to build+push images, or "clean" to remove all local images'

if [[ $# -ne 2 ]]
then
    echo "build_images_woz.sh <config file path> <build/clean>"
    exit 1
fi

declare -r config_file="${1}"
source "${config_file}"

script_path="$( dirname -- "$0"; )"

if [[ "${2}" == "clean" ]]
then
    docker rmi -f woz:latest 2> /dev/null
elif [[ "${2}" == "build" ]]
then
    # TODO: update when repos are merged
    # currently assumes TaskMAD and WoZStudy repos are in the same parent directory
    pushd "${script_path}/../../WoZStudy/"
    docker build --build-arg spreadsheet_url="${spreadsheet_url}" --build-arg recipe_url="${recipe_url}" --build-arg data_url="${data_url}" -f Dockerfile -t woz:latest .
    popd
else
    echo "Unknown argument: ${2} (expected either 'build' or 'clean')"
    exit 1
fi
