#!/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

# Copy local files to an existing GCP persistent disk. If the disk is
# newly created it will be formatted as ext4 first. 
# 
# Files are simply copied recursively using SCP from local to remote.
#
# Arguments expected:
#   $1 = disk name
#   $2 = VM name
#   $3 = GCP zone
#   $4 = path to local directory containing files to copy 
#   $5 = path to remote destination directory (relative to root of the disk)
#   $6 = true if newly created disk (requires formatting), false otherwise

if [[ $# -ne 6 ]]
then
    echo "copy_files_to_gcp_disk.sh <VM name> <disk name> <zone> <local source dir> <remote dest dir> <true|false>"
    exit 1
fi
    
script_path="$( dirname -- "$0"; )"

declare -r vm_name="${1}"
declare -r disk_name="${2}"
declare -r zone="${3}"
declare -r src="${4}"
declare -r dest="${5}"
# not readonly as it might be updated below
declare new_disk="${6}"

check_source_exists() {
    # Check if the local source directory actually exists
    # 
    # Arguments:
    #   $1 = path to local source directory
    #
    # Return value: ignored, should exit if not found
    if [[ ! -d "${src}" ]]
    then
        echo "Source folder ${src} does not exist!"
        exit 1
    fi
}

is_response_not_empty() {
    # Check if a gcloud response is empty or not
    # 
    # Arguments:
        # $1 = response text
    #
    # Return value: 0 if argument is NOT empty, 1 if it IS empty
    [[ -n "${1}" ]]
}

does_disk_exist() {
    # Check if a disk with the given label already exists
    #
    # Arguments:
    #   $1 = label of the disk
    #   $2 = zone to filter on
    #
    # Return value: 0 if disk exists, 1 if not

    resp=$(gcloud compute disks list --zones="${2}" --filter=name="${1}" 2> /dev/null)
    is_response_not_empty "${resp}"
}

does_vm_exist() {
    # Check if a VM with the given name already exists
    #
    # Arguments:
    #   $1 = name of the VM
    #   $2 = zone to filter on
    #
    # Return value: 0 if VM exists, 1 if not
    resp=$(gcloud compute instances list --zones="${2}" --filter=name="${1}" 2> /dev/null)
    is_response_not_empty "${resp}"
}

configure_ssh() {
    # check SSH access works for a newly created VM, (and generate a key 
    # silently with --quiet) this might fail if the VM is still booting, 
    # try a few times with a delay between attempts
    #
    # Arguments:
    #   $1 = VM name
    #
    # Return value: 0 if successful, 1 if not
    declare num_retries=4
    declare retry_delay=10
    declare return_code=1

    for (( i=1; i<=num_retries; i++ )) 
    do
        echo "> Generating SSH keys and logging into VM..."
        if ! gcloud compute config-ssh 2>&1
        then
            echo "    (retry #${i}/${num_retries})"
            sleep "${retry_delay}"
        else
            return_code=0
            break
        fi
    done

    return ${return_code}
}

run_ssh_command() {
    # runs a command in a VM over SSH, with auto-retry if it fails since this
    # happens occasionally for no apparent reason
    #
    # Arguments:
    #   $1 = VM name
    #   $2 = SSH command string
    #   $3 = message to display describing command
    #
    # Return value: 0 if successful, 1 if not

    declare num_retries=4
    declare retry_delay=5
    declare return_code=1

    for (( i=1; i<=num_retries; i++ )) 
    do
        echo "> ${3}..."
        if ! gcloud compute ssh "${1}" --command "${2}"
        then
            echo "    (retry #${i}/${num_retries})"
            sleep "${retry_delay}"
        else
            return_code=0
            break
        fi
    done

    return ${return_code}
}

format_disk() {
    # Format a newly created disk using ext4
    # 
    # Arguments:
    #   $1 = VM name
    #   $2 = disk name 
    #   
    #   Return value: ignored (should exit on error)

    run_ssh_command "${1}" "sudo /sbin/mkfs.ext4 -q /dev/disk/by-id/google-${2}" "Formatting the disk"
    # echo "> Formatting the disk..."
    # gcloud compute ssh "${1}" --command "sudo /sbin/mkfs.ext4 -q /dev/disk/by-id/google-${2}"
}

scp_files_to_disk() {
    # Use scp to mirror the local files to remote path
    # 
    # Arguments:
    #   $1 = VM name
    #   $2 = disk name
    #   $3 = local source directory
    #   $4 = remote dest directory
    #   
    # Return value: ignored (should exit on error)

    declare -r mountpoint="/mnt/temp"
    # Mount the drive at a fixed location
    run_ssh_command "${1}" "sudo mkdir -p ${mountpoint} && sudo mount /dev/disk/by-id/google-${2} ${mountpoint}" "Mounting drive at ${mountpoint} in the VM"

    # Create the path structure on the remote disk
    run_ssh_command "${1}" "sudo mkdir -p ${mountpoint}/${4} && sudo chmod -R a+rwx ${mountpoint}" "Creating remote dir structure ${mountpoint}/${4}"

    # Copy the files 
    declare local_sz
    local_sz=$(du -sm "${3}" | awk '{ print $1; }')
    echo "> Copying a total of ${local_sz}MB of files to remote..."
    gcloud compute scp --recurse "${3}"/* "${1}:${mountpoint}/${4}"
    echo "> Finished copying files!"
    run_ssh_command "${1}" "sudo sync && sudo umount ${mountpoint}" "Unmounting disk"
    echo "> Disk unmounted"
}

if ! does_vm_exist "${vm_name}" "${zone}"
then
    echo "> Creating a VM instance called ${vm_name}..."
    gcloud compute instances create "${vm_name}" --image-family=debian-11 --image-project=debian-cloud --machine-type=f1-micro --network-tier=STANDARD
else
    echo "> Using existing VM ${vm_name}"
fi

if ! configure_ssh "${vm_name}"
then
    echo "> Failed to configure SSH credentials for the VM!"
    exit 1
fi

echo "> Checking disk ${disk_name} exists..."
does_disk_exist "${disk_name}" "${zone}"

# using device-name here allows the disk to be easily accessed under that for mounting/formatting
echo "> Attaching disk ${disk_name}..."
if ! gcloud compute instances attach-disk "${vm_name}" --disk "${disk_name}" --device-name="${disk_name}"
then
    # probably means already attached from a previous attempt"
    echo "> Failed to attach disk, attempting to detach it..."
    gcloud compute instances detach-disk "${vm_name}" --disk "${disk_name}"
    # assume it wasn't formatted on the previous attempt
    new_disk="true"
    echo "> Re-attaching disk ${disk_name}..."
    gcloud compute instances attach-disk "${vm_name}" --disk "${disk_name}" --device-name="${disk_name}"
fi

if [[ "${new_disk}" = "true" ]]
then
    echo "> Disk is unformatted, will format and then copy files"
    format_disk "${vm_name}" "${disk_name}"
else
    echo "> Disk already formatted, only syncing files"
fi

scp_files_to_disk "${vm_name}" "${disk_name}" "${src}" "${dest}"

# detach the disk from the VM
echo "> Detaching disk..."
gcloud compute instances detach-disk "${vm_name}" --disk "${disk_name}"

# dispose of VM
echo "> Deleting VM instance ${vm_name}..."
gcloud compute instances delete "${vm_name}" --quiet 

echo "> Completed successfully!"
