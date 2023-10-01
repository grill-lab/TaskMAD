#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the chat UI component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = either "build" to build+push images, or "clean" to remove all local images'

if [[ $# -ne 2 ]]
then
    echo "build_images_chatui.sh <config file path> <build/clean>"
    exit 1
fi

declare -r config_file="${1}"

# shellcheck disable=1090
source "${config_file}"

script_path="$( dirname -- "$0"; )"

if [[ "${2}" == "clean" ]]
then
    docker rmi -f chat:latest 2> /dev/null
elif [[ "${2}" == "build" ]]
then
    pushd "${script_path}/../agent-dialogue-ui"

    # set the recipe URL and default backend endpoint from the config file
    declare -r backend_url="https://${core[domain]}"
    docker build --build-arg topic_url="${topic_url}" --build-arg backend_url="${backend_url}" --build-arg data_url="${data_url}" -f Dockerfile -t chat:latest .

    popd
else
    echo "Unknown argument: ${2} (expected either 'build' or 'clean')"
    exit 1
fi
