#!/bin/bash

##############################
### Run the app in AWS EMR ###
##############################

# Commons

HELP_DESC="Run the classifier app on an AWS EMR cluster"
OPTS_NAME=(DATASET AWS_AK_ID AWS_AK_SECRET INSTANCE_COUNT OUTPUT_DIR)
OPTS_FLAG=(d k s c o)
DATASETS=("supermarket" "land" "indoor")
DATASETS_LISTING=""
for I in ${!DATASETS[@]}; do 
	DATASETS_COUNT="${#DATASETS[@]}"
	LAST_DATASET_I=$((DATASETS_COUNT-1))
	if [ "$I" -eq "$LAST_DATASET_I" ]; then
		DATASETS_LISTING="$DATASETS_LISTING and "
	elif [ "$I" -ne "0" ]; then
		DATASETS_LISTING="$DATASETS_LISTING, "
	fi
	DATASETS_LISTING="$DATASETS_LISTING'${DATASETS[$I]}'"
done
OPTS_DEF=("${DATASETS[0]}" "" "" 3 "result")
OPTS_HELP=("the dataset to use, between $DATASETS_LISTING" "the AWS access key ID (defaults to AWS_ACCESS_KEY_ID)" "the AWS secret access key (defaults to AWS_SECRET_ACCESS_KEY)" "the number of instances, between 2 and 10" "the output directory")

. `dirname "$0"`/../.commons.sh
reqs curl

DATASET=${ARGS[DATASET]}
INSTANCE_COUNT=${ARGS[INSTANCE_COUNT]}
OUTPUT_DIR=${ARGS[OUTPUT_DIR]}

DATASET_OK=false
for VALID_DATASET in ${DATASETS[@]}; do 
	[ "$VALID_DATASET" != "$DATASET" ] || DATASET_OK=true
done
[ "$DATASET_OK" = true ] || dieh "Unknown dataset '$DATASET'."
( [ "$INSTANCE_COUNT" -ge 2 ] && [ "$INSTANCE_COUNT" -le 10 ] ) || dieh "INSTANCE_COUNT must fall in range [2, 10]."

# CLI

export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID:-${ARGS[AWS_AK_ID]}}
export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY:-${ARGS[AWS_AK_SECRET]}}
export AWS_DEFAULT_REGION="us-east-1"
export AWS_DEFAULT_OUTPUT="text"

[ -n "$AWS_ACCESS_KEY_ID" ] || dieh "Missing AWS access key ID. Set the 'AWS_ACCESS_KEY_ID' environment variable or use the -k option."
[ -n "$AWS_SECRET_ACCESS_KEY" ] || dieh "Missing AWS secret access key. Set the 'AWS_SECRET_ACCESS_KEY' environment variable or use the -s option."

log "Setting up the AWS CLI"

CLI="$SDIR/.aws-cli"
if [ -f "$CLI" ]; then 
	echo "Reusing cached AWS CLI."
else
	curl -o "$CLI" -L "https://github.com/simnalamburt/awscliv2.appimage/releases/latest/download/aws-x86_64.AppImage" || die "Download failed."
	chmod +x "$CLI"
fi

"$CLI" sts get-caller-identity >& /dev/null
[ $? -eq "0" ] || die "AWS authentication failed. Is the access key correct?"

CLUSTER_NAME="francescozoccheddu-big-data-project-image-classifier"

"$CLI" emr list-clusters --active | grep "^CLUSTERS" | cut -f4 | grep "^$CLUSTER_NAME$" && die "An active cluster already exists."

# Create output directory

mkdir -p "$OUTPUT_DIR" || die "Failed to create the output directory."

function cleandir {
	rmdir --ignore-fail-on-non-empty "$INSTALL_DIR"
}

function fail_cleanup {
	cleandir
}

# Create bucket

S3="s3://francescozoccheddu-big-data-project-image-classifier"

log "Creating S3 bucket '$S3'"
"$CLI" s3 mb "$S3" || die "Failed to create the bucket."

function fin_cleanup {
	cleandir
	log "Deleting the bucket"
	"$CLI" s3 rb "$S3" --force || die "Failed to delete the bucket."
}

# Upload scripts

S3_CONF="$S3/configuration.json"
S3_SCRIPT="$S3/job.sh" 

log "Uploading scripts"
"$CLI" s3 cp "$SDIR/.job.sh" "$S3_SCRIPT" || die "Failed to upload scripts."
"$CLI" s3 cp "$SDIR/.configuration.json" "$S3_CONF" || die "Failed to upload scripts."

# Create cluster

log "Creating the cluster"

S3_LOGS="$S3/logs" 

CLUSTER_ID=`"$CLI" create-cluster` || die "Failed to create the cluster."
echo "Cluster created succesfully with ID '$CLUSTER_ID'"

function fail_cleanup {
	cleandir
	log "Terminating the cluster"
	"$CLI" emr terminate-clusters --cluster-ids "$CLUSTER_ID" || die "Failed to terminate the cluster."
}

# Wait for cluster termination

echo "Waiting for the cluster to terminate its job..."
echo "Type CTRL+C to abort."

"$CLI" emr wait cluster-terminated --cluster-id "$CLUSTER_ID" || die "Cluster timed out."

echo "The cluster has terminated its job."

# Collect results

log "Collecting results"

OUT_MODEL="$OUTPUT_DIR/model"
OUT_SUMMARY="$OUTPUT_DIR/summary"
OUT_LOGS="$OUTPUT_DIR/logs"

function fail_cleanup {
	rm -f "$OUT_MODEL" "$OUT_SUMMARY" "$OUT_LOGS"
	cleandir
}

"$CLI" s3 cp "$S3/model" "$OUT_MODEL" || die "Failed to download results."
"$CLI" s3 cp "$S3/summary" "$OUT_SUMMARY" || die "Failed to download results."
"$CLI" s3 cp "$S3_LOGS" "$OUT_LOGS" || die "Failed to download results."
