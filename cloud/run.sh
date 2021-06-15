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

# AWS

export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID:-${ARGS[AWS_AK_ID]}}
export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY:-${ARGS[AWS_AK_SECRET]}}
export AWS_DEFAULT_REGION="us-east-1"
export AWS_DEFAULT_OUTPUT="text"

[ -n "$AWS_ACCESS_KEY_ID" ] || dieh "Missing AWS access key ID. Set the 'AWS_ACCESS_KEY_ID' environment variable or use the -k option."
[ -n "$AWS_SECRET_ACCESS_KEY" ] || dieh "Missing AWS secret access key. Set the 'AWS_SECRET_ACCESS_KEY' environment variable or use the -s option."

log "Setting up the AWS AWS"

AWS="$SDIR/.aws-cli"
if [ -f "$AWS" ]; then 
	echo "Reusing cached AWS AWS."
else
	curl -o "$AWS" -L "https://github.com/simnalamburt/awscliv2.appimage/releases/latest/download/aws-x86_64.AppImage" || die "Download failed."
	chmod +x "$AWS"
fi

UID=`"$AWS" sts get-caller-identity | cut -f1` || die "AWS authentication failed. Is the access key correct?"

CLUSTER_NAME="francescozoccheddu-big-data-project-image-classifier"

"$AWS" emr list-clusters --active | grep "^CLUSTERS" | cut -f4 | grep "^$CLUSTER_NAME$" && die "An active cluster already exists."

echo "S3 and EMR resources will be allocated in region '$AWS_DEFAULT_REGION'."

# Create output directory

mkdir -p "$OUTPUT_DIR" || die "Failed to create the output directory."

function cleardir {
	rmdir --ignore-fail-on-non-empty "$INSTALL_DIR"
}

function fin_cleanup {
	cleardir
}

# Create bucket

BUCKET_NAME="francescozoccheddu-big-data-project-image-classifier-$UID"
S3="s3://$BUCKET_NAME"

log "Creating S3 bucket '$BUCKET_NAME'"
"$AWS" s3 mb "$S3" || die "Failed to create the bucket."

function fin_cleanup {
	cleardir
	log "Deleting the bucket"
	"$AWS" s3 rb "$S3" --force || die "Failed to delete the bucket."
}

# Upload scripts

S3_CONF="$S3/configuration.json"
S3_SCRIPT="$S3/job.sh" 

log "Uploading scripts"
"$AWS" s3 cp "$SDIR/.job.sh" "$S3_SCRIPT" || die "Failed to upload scripts."

# Create cluster

log "Creating the cluster '$CLUSTER_NAME'"

RES_NAME="results"
LOGS_NAME="logs"
S3_RES="$S3/results"

"$AWS" emr create-default-roles || die "Failed to create EMR default roles." 
CLUSTER_ID=$("$AWS" create-cluster 
	--applications Name=Spark 
	--use-default-roles 
	--auto-terminate 
	--release-label emr-6.3.0 
	--log-uri "s3n://$BUCKET_NAME/$RES_NAME/$LOGS_NAME" 
	--name "$CLUSTER_NAME" 
	--configurations "file://$SDIR/.configuration.json" 
	--instance-type m4.large 
	--instance-count $INSTANCE_COUNT`
	--steps Type=CUSTOM_JAR,Name=Job,ActionOnFailure=TERMINATE_CLUSTER,Jar=s3://$AWS_DEFAULT_REGION.elasticmapreduce/libs/script-runner/script-runner.jar,Args=["$S3_SCRIPT","$DATASET"]
	) || die "Failed to create the cluster."
echo "Cluster created succesfully with ID '$CLUSTER_ID'"

function fail_cleanup {
	log "Terminating the cluster"
	"$AWS" emr terminate-clusters --cluster-ids "$CLUSTER_ID" || die "Failed to terminate the cluster."
}

# Wait for cluster termination

echo "Waiting for the cluster to terminate its job..."
echo "Type CTRL+C to abort."

"$AWS" emr wait cluster-terminated --cluster-id "$CLUSTER_ID" || die "Cluster timed out."

echo "The cluster has terminated its job."

# Collect results

log "Collecting results"

function fail_cleanup {
	rm -f "$OUTPUT_DIR/model" "$OUTPUT_DIR/summary"
	rm -rf "$OUTPUT_DIR/$LOGS_NAME"
}

"$AWS" s3 cp "$S3/$RES_NAME" "$OUTPUT_DIR" --recursive || die "Failed to download results."
