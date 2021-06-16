#!/bin/bash

##############################
### Run the app in AWS EMR ###
##############################

# Commons

HELP_DESC="Create an EMR cluster, run the app, then destroy the cluster (create a temporary S3 bucket to communicate)"
. `dirname "$0"`/.commons.sh

# Job

create_bucket

log "Uploading script"
bucket_ul "$JOB_FILE" "job.sh"

EMR_STEP_1=Type=CUSTOM_JAR,Name=Job,ActionOnFailure=TERMINATE_CLUSTER,Jar=s3://$AWS_DEFAULT_REGION.elasticmapreduce/libs/script-runner/script-runner.jar,Args=[$BUCKET/job.sh,$DATASET]
EMR_STEP_2=Type=CUSTOM_JAR,Name=UploadResultsToS3,ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar,Args=[aws,s3,cp,$RES_REMOTE,$BUCKET/results.tgz]
EMR_STEPS="$EMR_STEP_1 $EMR_STEP_2"
create_cluster

wait_cluster_terminated

log "Collecting results"
bucket_dl "results.tgz" "$RES_TEMP_TGZ"
extract_results
