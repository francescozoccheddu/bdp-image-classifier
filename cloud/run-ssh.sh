#!/bin/bash

##############################
### Run the app in AWS EMR ###
##############################

# Commons

HELP_DESC="Create an EMR cluster, run the app, then destroy the cluster (create a temporary EC2 key-pair and use SSH to communicate)"
. `dirname "$0"`/.commons.sh

# Create key

create_key

# Run job

EMR_KEY=true
create_cluster
wait_cluster_running
authorize_ssh
JOB_REMOTE="$HOME_REMOTE/job.sh"
cluster_ul "$JOB_FILE" "$JOB_REMOTE"
cluster_ssh "chmod +x \"$JOB_REMOTE\" && \"$JOB_REMOTE\" \"$DATASET\""

# Download results

cluster_dl "$RES_REMOTE" "$RES_TEMP_TGZ"
extract_results