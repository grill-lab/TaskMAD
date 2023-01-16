#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the chat UI component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = remote docker repo to tag images for
#   $3 = either "build" to build+push images, or "clean" to remove all local images'

if [[ $# -ne 3 ]]
then
    echo "build_images_chatui.sh <config file path> <remote-repo> <build/clean>"
    exit 1
fi

declare -r config_file="${1}"
declare -r remote="${2}"

# shellcheck disable=1090
source "${config_file}"

script_path="$( dirname -- "$0"; )"

if [[ "${3}" == "clean" ]]
then
    docker rmi -f chat:latest 2> /dev/null
    docker rmi -f "${remote}"/chat:latest 2> /dev/null
elif [[ "${3}" == "build" ]]
then
    pushd "${script_path}/../agent-dialogue-ui"

    # set the recipe URL and default backend endpoint from the config file
    declare -r backend_url="https://${core[domain]}"
    docker build --build-arg recipe_url="${recipe_url}" --build-arg backend_url="${backend_url}" -f Dockerfile -t chat:latest .
    docker tag chat:latest "${remote}"/chat:latest

    popd

    docker push "${remote}"/chat:latest
else
    echo "Unknown argument: ${3} (expected either 'build' or 'clean')"
    exit 1
fi
