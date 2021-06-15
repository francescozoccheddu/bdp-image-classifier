#!/bin/bash

###############################
######## Cloud commons ########
###############################

# Commons

OPTS_NAME=(DATASET AWS_AK_ID AWS_AK_SECRET INSTANCE_COUNT OUTPUT_DIR)
OPTS_FLAG=(d k s c o)
_ICC_COMMONS_DATASETS=("supermarket" "land" "indoor")
_ICC_COMMONS_DATASETS_LISTING=""
for I in ${!_ICC_COMMONS_DATASETS[@]}; do 
	_ICC_COMMONS_DATASETS_COUNT="${#_ICC_COMMONS_DATASETS[@]}"
	_ICC_COMMONS_LAST_DATASET_I=$((_ICC_COMMONS_DATASETS_COUNT-1))
	if [ "$I" -eq "$_ICC_COMMONS_LAST_DATASET_I" ]; then
		_ICC_COMMONS_DATASETS_LISTING="$_ICC_COMMONS_DATASETS_LISTING and "
	elif [ "$I" -ne "0" ]; then
		_ICC_COMMONS_DATASETS_LISTING="$_ICC_COMMONS_DATASETS_LISTING, "
	fi
	_ICC_COMMONS_DATASETS_LISTING="$_ICC_COMMONS_DATASETS_LISTING'${_ICC_COMMONS_DATASETS[$I]}'"
done
OPTS_DEF=("${_ICC_COMMONS_DATASETS[0]}" "" "" 3 "the output directory")
OPTS_HELP=("the dataset to use, between $_ICC_COMMONS_DATASETS_LISTING" "the AWS access key ID (defaults to AWS_ACCESS_KEY_ID)" "the AWS secret access key (defaults to AWS_SECRET_ACCESS_KEY)" "the number of instances, between 2 and 10" "results")

. `dirname "$0"`/../.commons.sh
reqs curl

DATASET=${ARGS[DATASET]}
INSTANCE_COUNT=${ARGS[INSTANCE_COUNT]}
OUTPUT_DIR=${ARGS[OUTPUT_DIR]}

_ICC_COMMONS_DATASET_OK=false
for _ICC_COMMONS_VALID_DATASET in ${_ICC_COMMONS_DATASETS[@]}; do 
	[ "$_ICC_COMMONS_VALID_DATASET" != "$DATASET" ] || _ICC_COMMONS_DATASET_OK=true
done
[ "$_ICC_COMMONS_DATASET_OK" = true ] || dieh "Unknown dataset '$DATASET'."
( [ "$INSTANCE_COUNT" -ge 2 ] && [ "$INSTANCE_COUNT" -le 10 ] ) || dieh "INSTANCE_COUNT must fall in range [2, 10]."

# Paths

MODEL_NAME="model"
SUMMARY_NAME="summary"
LOG_NAME="log"
EMR_LOG_NAME="emr-logs"
RES_NAME="results"
JOB_FILE="$SDIR/.job.sh.template"
EMR_CONFIG_FILE="$SDIR/.configuration.json"

mkdir -p "$OUTPUT_DIR" || die "Failed to create output directory '$OUTPUT_DIR'."

function fail_cleanup {
	rm -f "$OUTPUT_DIR/$MODEL_NAME" "$OUTPUT_DIR/$SUMMARY_NAME" "$OUTPUT_DIR/$LOG_NAME"
	rm -rf "$OUTPUT_DIR/$EMR_LOG_NAME"
	rmdir --ignore-fail-on-non-empty "$OUTPUT_DIR"
}

# AWS

export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID:-${ARGS[AWS_AK_ID]}}
export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY:-${ARGS[AWS_AK_SECRET]}}
export AWS_DEFAULT_REGION="us-east-1"
export AWS_DEFAULT_OUTPUT="text"

[ -n "$AWS_ACCESS_KEY_ID" ] || dieh "Missing AWS access key ID. Set the 'AWS_ACCESS_KEY_ID' environment variable or use the -k option."
[ -n "$AWS_SECRET_ACCESS_KEY" ] || dieh "Missing AWS secret access key. Set the 'AWS_SECRET_ACCESS_KEY' environment variable or use the -s option."


log "Setting up the AWS CLI"

AWS="$SDIR/.aws-cli"
if [ -f "$AWS" ]; then 
	echo "Reusing cached AWS AWS."
else
	curl -o "$AWS" -L "https://github.com/simnalamburt/awscliv2.appimage/releases/latest/download/aws-x86_64.AppImage" || die "Download failed."
	chmod +x "$AWS"
fi

AWS_UID=`"$AWS" sts get-caller-identity | cut -f1` || die "AWS authentication failed. Is the access key correct?"

NAME="francescozoccheddu-big-data-project-image-classifier"
CLUSTER_NAME="$NAME"
KEY_NAME="$NAME"
KEY_FILE="$SDIR/.key"
BUCKET_NAME="$NAME-$AWS_UID"
BUCKET_AWS="s3://$BUCKET_NAME"
BUCKET_HADOOP="s3:///$BUCKET_NAME"
RES_REMOTE="/home/hadoop/image-classifier-results"

"$AWS" emr list-clusters --active | grep "^CLUSTERS" | cut -f4 | grep "^$CLUSTER_NAME$" && die "An active cluster already exists."

echo "All AWS resources will be allocated in region '$AWS_DEFAULT_REGION'."

function create_cluster {
	log "Creating cluster '$CLUSTER_NAME'"
	"$AWS" emr create-default-roles || die "Failed to create EMR default roles." 
	local OPTS=""
	[ "$EMR_KEY" = true ] && OPTS="$OPTS --ec2-attributes KeyName=\"$KEY_NAME\""  
	[ "$EMR_STEPS" = true ] && OPTS="$OPTS --auto-terminate --steps $EMR_STEPS"  
	[ "$EMR_STEPS" = true ] || OPTS="$OPTS --no-auto-terminate"  
	[ "$EMR_LOG" = true ] && OPTS="$OPTS --log-uri \"s3n://$BUCKET_NAME/$RES_NAME/$EMR_LOG_NAME\"" 
	CLUSTER_ID=$("$AWS" create-cluster 
		--applications Name=Spark 
		--use-default-roles 
		--release-label emr-6.3.0 
		--name "$CLUSTER_NAME" 
		--configurations "file://$SDIR/.configuration.json" 
		--instance-type m4.large 
		--instance-count $INSTANCE_COUNT
		$OPTS
		) || die "Failed to create the cluster."
	echo "Cluster created succesfully with ID '$CLUSTER_ID'."
}

function terminate_cluster {
	[ -n "$CLUSTER_ID" ] || return
	log "Terminating the cluster"
	"$AWS" emr terminate-clusters --cluster-ids "$CLUSTER_ID" || die "Failed to terminate the cluster."
}

function wait_cluster {
	[ -n "$CLUSTER_ID" ] || return
	echo "Waiting for the cluster to terminate its job..."
	echo "Type CTRL+C to abort."
	"$AWS" emr wait cluster-terminated --cluster-id "$CLUSTER_ID" || die "Cluster timed out."
	echo "The cluster has terminated its job."
}

function _icc_commons_delete_key {
	"$AWS" ec2 delete-key-pair --key-name "$KEY_NAME" || die "Failed to delete EC2 key pair."
	rm -f "$KEY_FILE"
}

function create_key {
	log "Creating EC2 key pair '$KEY_NAME'"
	_icc_commons_delete_key
	"$AWS" ec2 create-key-pair --key-name "$KEY_NAME" > "$KEY_FILE" || die "Failed to create EC2 key pair."
}

function delete_key {
	log "Deleting EC2 key pair"
	_icc_commons_delete_key
}

function _icc_commons_delete_bucket {
	"$AWS" s3 rb "$BUCKET_AWS" --force || die "Failed to delete S3 bucket."
}

function create_bucket {
	log "Creating S3 bucket '$BUCKET_NAME'"
	_icc_commons_delete_bucket
	"$AWS" s3 mb "$BUCKET_AWS" || die "Failed to create S3 bucket."
}

function delete_bucket {
	log "Deleting S3 bucket"
	_icc_commons_delete_bucket
}

function fin_cleanup {
	terminate_cluster
	_icc_commons_delete_bucket
	_icc_commons_delete_key
}