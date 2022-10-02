#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the search API component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = remote docker repo to tag images for

if [[ $# -ne 1 ]]
then
    echo "build_images_searchapi.sh <remote-repo>"
    exit 1
fi

# declare -r config_file="${1}"
declare -r remote="${2}"

script_path="$( dirname -- "$0"; )"

docker rmi -f search-api:latest 2> /dev/null
docker rmi -f "${remote}"/search-api:latest 2> /dev/null

# TODO: update when repos are merged
# currently assumes TaskMAD and search API repos are in the same parent directory
pushd "${script_path}/../../GroundedKnowledgeInterface/api"

docker build -f Dockerfile -t search-api:latest .
docker tag search-api:latest "${remote}"/search-api:latest

popd

docker push "${remote}"/search-api:latest
