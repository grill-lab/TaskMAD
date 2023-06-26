#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# TaskMAD deployment script for Docker on local machine.

# define some friendly names for ANSII colour escape codes
RED="\e[31m"
GREEN="\e[32m"
YELLOW="\e[33m"
BLUE="\e[34m"
PURPLE="\e[35m"
CYAN="\e[36m"
NC="\e[0m"

script_path="$( dirname -- "$0"; )"

declare deployment_ok=true

exit_handler() {
    # Handler for the bash EXIT signal. The script is set up to exit
    # if a command returns an unsuccessful status code, but depending
    # on when/how that happens it might not be immediately obvious to 
    # the user that something went wrong. 
    #
    # This handler just uses the exit_ok variable to check if the
    # exit event happened prematurely due to an error, and reports
    # that to the user (currently only for the deployment phase, 
    # as that's by far the most complex).
    # 
    # TODO: some of the steps redirect stderr to /dev/null, should
    # probably make sure it's visible instead to help debug problems
    if [[ "${deployment_ok}" == false ]]
    then
        echo -e "\n\n*** The selected TaskMAD deployment steps were NOT successfully completed!"
        echo "*** Check the output above for error messages"
    else
        echo -e "\n\nThe selected TaskMAD deployment steps were successfully completed!"
    fi
}

echo_color() {
    # Simple wrapper for the echo command to print coloured messages
    # 
    # $1 = message to print
    # $2 = color sequence (optional, defaults to green)
    #
    # Return value: ignored

    # this syntax sets "color" to ${GREEN} if a 2nd argument is not passed
    # to the function
    local color="${2:-${GREEN}}"
    echo -en "${color}${1}${NC}"
}

build_images() {
    # Builds all the TaskMAD Docker images locally
    #
    # Return value: ignored (should exit on error)

    echo_color "> Building images\n"
    for d in "${deployments[@]}"
    do
        build_cmd="${d}[docker_build_script]"
        if [[ -n "${!build_cmd}" ]]
        then
            echo_color "> Running build script for ${d}...\n"
            eval "${!build_cmd}" "${script_path}/deploy_config" "build"
        else
            echo_color "> Skipping Docker build for ${d}...\n" "${YELLOW}"
        fi
    done

    echo_color "> All images built!\n"
}

create_containers() {
    # Create all containers with necessary networking etc
    
    # run envoy to handle the incoming external connections and route them
    # to the appropriate containers. the envoy Docker image is using host
    # networking because we need to redirect connections to the separate
    # deployment for the cast searcher

    # TODO build this even if core not selected
    docker run --rm -d --name envoy --network host envoy:latest
    if [[ "${deployments[*]}" =~ "core" ]]
    then
        # Core listens on port 8070, and needs a volume mounted with config files/keys 
        docker run --rm -d --name grpc-server -p 8070:8070 --mount type=bind,src=./core_files,dst=/code/keys grpc-server:latest
    fi	 
    
    if [[ "${deployments[*]}" =~ "chat" ]]
    then
        # Chat listens on port 80
        docker run --rm -d --name chat -p 8080:80 chat:latest
    fi

    if [[ "${deployments[*]}" =~ "woz" ]]
    then
        # Woz listens on port 80
        docker run --rm -d --name woz -p 8090:80 woz:latest
    fi

    if [[ "${deployments[*]}" =~ "search" ]]
    then
        # Search listens on port 5000
        docker run --rm -d --name search -p 8100:5000 search-api:latest
    fi
}

stop_containers() {
    for c in grpc-server chat woz search envoy
    do
        if [[ "${deployments[*]}" =~ "${c}" ]]
        then
            docker stop ${c}
        fi
    done
    # TODO again see above
    docker stop envoy
}

clean_docker_images() {
    for d in "${deployments[@]}"
    do
        build_cmd="${d}[docker_build_script]"
        echo_color "> Removing local images for ${d}\n"
        eval "${!build_cmd}" "${script_path}/deploy_config" "clean"
    done
}

###

# source user-defined variables (VSCode: if this generates a shellcheck warning 
# add "-x" as a custom argument in the extension settings)
# shellcheck source=docker_deployment/deploy_config
source "${script_path}/deploy_config"

# require a parameter to be passed to perform any actions
if [[ $# -lt 1 ]]
then
    echo "Usage: deploy.sh <build|start|stop|cleanup>"
    echo ""
    echo "Available commands:"
    echo -e "   ${GREEN}build${NC}: build all Docker images"
    echo -e "   ${GREEN}start${NC}: create all Docker containers"
    echo -e "   ${GREEN}stop${NC}: stop all Docker containers"
    echo -e "   ${GREEN}cleanup${NC}: delete all local Docker images for the deployments."
    exit 0
fi

pushd "${script_path}" > /dev/null

if [[ "${1}" == "cleanup" ]]
then
    clean_docker_images
elif [[ "${1}" == "build" ]]
then
    # hook up the exit_handler function above to the bash "EXIT" signal, 
    # so it will be called when the script exits (for any reason)
    trap exit_handler EXIT

    # see exit_handler above
    deployment_ok=false

    build_images 

    # for exit_handler, see above
    deployment_ok=true
elif [[ "${1}" == "start" ]]
then
    create_containers
elif [[ "${1}" == "stop" ]]
then 
    stop_containers
elif [[ "${1}" == "cleanup" ]]
then
    clean_docker_images
else
    echo_color "Unrecognised parameter \"${1}\"\n"
    exit 1
fi

popd > /dev/null
exit 0
