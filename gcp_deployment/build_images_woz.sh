#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the wizard component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = remote docker repo to tag images for
#   $3 = either "build" to build+push images, or "clean" to remove all local images'

if [[ $# -ne 3 ]]
then
    echo "build_images_woz.sh <config file path> <remote-repo> <build/clean>"
    exit 1
fi

# declare -r config_file="${1}"
declare -r remote="${2}"

script_path="$( dirname -- "$0"; )"

if [[ "${3}" == "clean" ]]
then
    docker rmi -f woz:latest 2> /dev/null
    docker rmi -f "${remote}"/woz:latest 2> /dev/null
elif [[ "${3}" == "build" ]]
then
    # TODO: update when repos are merged
    # currently assumes TaskMAD and WoZStudy repos are in the same parent directory
    pushd "${script_path}/../../WoZStudy/"

    docker build -f Dockerfile -t woz:latest .
    docker tag woz:latest "${remote}"/woz:latest

    popd

    docker push "${remote}"/woz:latest
else
    echo "Unknown argument: ${3} (expected either 'build' or 'clean')"
    exit 1
fi
