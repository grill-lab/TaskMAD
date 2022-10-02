#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Build Docker images for the chat UI component
#
# Arguments expected:
#   $1 = path to full config file
#   $2 = remote docker repo to tag images for

if [[ $# -ne 2 ]]
then
    echo "build_images_chatui.sh <config file path> <remote-repo>"
    exit 1
fi

# declare -r config_file="${1}"
declare -r remote="${2}"

script_path="$( dirname -- "$0"; )"

docker rmi -f chat:latest 2> /dev/null
docker rmi -f "${remote}"/chat:latest 2> /dev/null

pushd "${script_path}/../agent-dialogue-ui"

docker build -f Dockerfile -t chat:latest .
docker tag chat:latest "${remote}"/chat:latest

popd

docker push "${remote}"/chat:latest
