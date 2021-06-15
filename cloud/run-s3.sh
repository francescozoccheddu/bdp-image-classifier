#!/bin/bash

##############################
### Run the app in AWS EMR ###
##############################

. `dirname "$0"`/.commons.sh

RES_AWS="$BUCKET_AWS/$RES_NAME"

# Upload script

create_bucket
"$AWS" s3 cp "$JOB_FILE" "$BUCKET_AWS/job.sh" || die "Failed to upload script."

# Run job

EMR_LOG=true
EMR_KEY=false
EMR_STEP_1=Type=CUSTOM_JAR,Name=Job,ActionOnFailure=TERMINATE_CLUSTER,Jar=s3://$AWS_DEFAULT_REGION.elasticmapreduce/libs/script-runner/script-runner.jar,Args=["$BUCKET_AWS/job.sh","$DATASET"]
EMR_STEP_2=Type=CUSTOM_JAR,Name=UploadResultsToS3,ActionOnFailure=TERMINATE_CLUSTER,Jar=command-runner.jar,Args=["aws s3 cp \"$RES_REMOTE\" \"$RES_AWS\" --recursive"]
EMR_STEPS="$EMR_STEP_1 $EMR_STEP_2"
create_cluster
wait_cluster

# Download results

"$AWS" s3 cp "$RES_AWS" "$OUTPUT_DIR" --recursive || die "Failed to download results."
