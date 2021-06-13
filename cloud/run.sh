#!/bin/bash

##############################
### Run the app in AWS EMR ###
##############################

# Commons

HELP_DESC="Run the classifier app on an AWS EMR cluster"
OPTS_NAME=(DATASET AWS_AK_ID AWS_AK_SECRET INSTANCE_COUNT)
OPTS_FLAG=(d k s c)
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
OPTS_DEF=("${DATASETS[0]}" "" "" 3)
OPTS_HELP=("the dataset to use, between $DATASETS_LISTING" "the AWS access key ID" "the AWS secret access key" "the number of instances, between 2 and 10")

. `dirname "$0"`/../.commons.sh
reqs curl

DATASET=${ARGS[DATASET]}
INSTANCE_COUNT=${ARGS[INSTANCE_COUNT]}

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

[ -n "$AWS_ACCESS_KEY_ID" ] || dieh "Missing AWS access key ID. Set the 'AWS_ACCESS_KEY_ID' environment variable or use the -k option."
[ -n "$AWS_SECRET_ACCESS_KEY" ] || dieh "Missing AWS secret access key. Set the 'AWS_SECRET_ACCESS_KEY' environment variable or use the -s option."

log "Setting up the AWS CLI"

CLI="$SDIR/.aws-cli"
if [ -f "$CLI" ]; then 
	echo "Reusing cached AWS CLI."
else
	curl -o "$CLI" -L "https://github.com/simnalamburt/awscliv2.appimage/releases/latest/download/aws-x86_64.AppImage" || die "Download failed."
	chmod +cx"$CLI"
fi

"$AWS" sts get-caller-identity >& /dev/null
[ $? -eq "0" ] || die "AWS authentication failed. Is the access key correct?"

# Create bucket

UUID=`< /proc/sys/kernel/random/uuid`
BUCKET="image-classifier-$UUID"

log "Creating S3 bucket '$BUCKET'"

function fin_cleanup {
	log "Deleting the bucket"
	"$CLI" s3api delete-bucket --bucket "$BUCKET" || die "Failed to delete the bucket."
}

# Create cluster

log "Creating the cluster"
CLUSTER=`"$CLI" create-cluster` || die "Failed to create the cluster."
echo "Cluster '$CLUSTER' created succesfully"

function fail_cleanup {
	log "Terminating the cluster"
	"$CLI" emr terminate-clusters --cluster-ids "$CLUSTER" || die "Failed to terminate the cluster."
}

echo "Waiting for the cluster to terminate its job..."
echo "Type CTRL+C to abort."

echo "The cluster has terminated its job."

# Collecting results

log "Collecting results"