#!/bin/bash

###############################
######## Cloud commons ########
###############################

# Commons

OPTS_NAME=(DATASET AWS_AK_ID AWS_AK_SECRET INSTANCE_COUNT OUTPUT_DIR)
OPTS_FLAG=(d k s c o)
_ICC_COMMONS_DATASETS=("test" "supermarket" "land" "indoor")
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
OPTS_DEF=("${_ICC_COMMONS_DATASETS[0]}" "" "" 3 "results")
OPTS_HELP=("the dataset to use, between $_ICC_COMMONS_DATASETS_LISTING" "the AWS access key ID (defaults to AWS_ACCESS_KEY_ID)" "the AWS secret access key (defaults to AWS_SECRET_ACCESS_KEY)" "the number of instances, between 2 and 10" "the output directory")

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

EMR_LOG_NAME="emr-logs"
JOB_FILE="$SDIR/.job.sh.template"
RES_TEMP_TGZ="$OUTPUT_DIR/.results.tgz"

mkdir -p "$OUTPUT_DIR" || die "Failed to create output directory '$OUTPUT_DIR'."

function fail_cleanup {
	rm -rf "$OUTPUT_DIR/model" "$OUTPUT_DIR/summary" "$OUTPUT_DIR/log" "$OUTPUT_DIR/summary.crc" "$OUTPUT_DIR/model.crc" "$RES_TEMP_TGZ"
	rmdir --ignore-fail-on-non-empty "$OUTPUT_DIR"
}

# AWS CLI

export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID:-${ARGS[AWS_AK_ID]}}
export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY:-${ARGS[AWS_AK_SECRET]}}
export AWS_DEFAULT_REGION="us-east-1"
export AWS_DEFAULT_OUTPUT="text"

[ -n "$AWS_ACCESS_KEY_ID" ] || dieh "Missing AWS access key ID. Set the 'AWS_ACCESS_KEY_ID' environment variable or use the -k option."
[ -n "$AWS_SECRET_ACCESS_KEY" ] || dieh "Missing AWS secret access key. Set the 'AWS_SECRET_ACCESS_KEY' environment variable or use the -s option."

log "Setting up the AWS CLI"

_ICC_COMMONS_AWS="$SDIR/.aws-cli"
if [ -f "$_ICC_COMMONS_AWS" ]; then 
	echo "Reusing cached AWS CLI."
else
	curl -o "$_ICC_COMMONS_AWS" -L "https://github.com/simnalamburt/awscliv2.appimage/releases/latest/download/aws-x86_64.AppImage" || die "Download failed."
	chmod +x "$_ICC_COMMONS_AWS"
fi

_ICC_COMMONS_AWS_UID=`"$_ICC_COMMONS_AWS" sts get-caller-identity --query UserId` || die "AWS authentication failed. Is the access key correct?"

_ICC_COMMONS_NAME="francescozoccheddu-big-data-project-image-classifier"
_ICC_COMMONS_CLUSTER_NAME="$_ICC_COMMONS_NAME"
_ICC_COMMONS_KEY_NAME="$_ICC_COMMONS_NAME"
_ICC_COMMONS_KEY_FILE="$SDIR/.key.pem"
_ICC_COMMONS_BUCKET_NAME="$_ICC_COMMONS_NAME-$_ICC_COMMONS_AWS_UID"
BUCKET="s3://$_ICC_COMMONS_BUCKET_NAME"
RES_REMOTE="/home/hadoop/image-classifier-results.tgz"
_ICC_COMMONS_REMOTE_USR="hadoop"
HOME_REMOTE="/home/$_ICC_COMMONS_REMOTE_USR"

_ICC_COMMONS_ACTIVE_CLUSTERS=`"$_ICC_COMMONS_AWS" emr list-clusters --active --query Clusters[?Name=="'$_ICC_COMMONS_CLUSTER_NAME'"]`
[ -n "$_ICC_COMMONS_ACTIVE_CLUSTERS" ] && die "An active cluster already exists."

echo "All AWS resources will be allocated in region '$AWS_DEFAULT_REGION'."

EMR_KEY=false
EMR_LOG=false
EMR_STEPS=""
unset _ICC_COMMONS_CLUSTER_ID

_ICC_COMMONS_CLUSTER_CREATED=false
_ICC_COMMONS_KEY_CREATED=false
_ICC_COMMONS_BUCKET_CREATED=false
_ICC_COMMONS_SSH_AUTHORIZED=false

function create_cluster {
	log "Creating cluster '$_ICC_COMMONS_CLUSTER_NAME'"
	_ICC_COMMONS_CLUSTER_CREATED=true
	"$_ICC_COMMONS_AWS" emr create-default-roles || die "Failed to create EMR default roles." 
	local OPTS=""
	[ "$EMR_KEY" = true ] && OPTS="$OPTS --ec2-attributes KeyName=\"$_ICC_COMMONS_KEY_NAME\""  
	[ -n "$EMR_STEPS" ] && OPTS="$OPTS --auto-terminate --steps $EMR_STEPS"  
	[ -n "$EMR_STEPS" ] || OPTS="$OPTS --no-auto-terminate"  
	[ "$EMR_LOG" = true ] && OPTS="$OPTS --log-uri \"s3n://$_ICC_COMMONS_BUCKET_NAME/$RES_NAME/$EMR_LOG_NAME\"" 
	_ICC_COMMONS_CLUSTER_ID=$("$_ICC_COMMONS_AWS" emr create-cluster \
		--query ClusterId \
		--applications Name=Spark \
		--use-default-roles \
		--release-label emr-6.3.0 \
		--name "$_ICC_COMMONS_CLUSTER_NAME" \
		--configurations "file://$SDIR/.configuration.json" \
		--instance-type m4.large \
		--instance-count $INSTANCE_COUNT \
		$OPTS ) || die "Failed to create the cluster."
	echo "Cluster created succesfully with ID '$_ICC_COMMONS_CLUSTER_ID'."
}

function authorize_ssh {
	log "Authorizing SSH inbound traffic"
	_ICC_COMMONS_MY_IP=`curl -s http://checkip.amazonaws.com/` || die "Failed to get local machine IP."
	_ICC_COMMONS_SECURITY_GROUP_ID=`"$_ICC_COMMONS_AWS" emr describe-cluster --cluster-id "$_ICC_COMMONS_CLUSTER_ID" --query Cluster.Ec2InstanceAttributes.EmrManagedMasterSecurityGroup` || die "Failed to get the security group."
	local AUTH_ERR=`"$_ICC_COMMONS_AWS" ec2 authorize-security-group-ingress --group-id "$_ICC_COMMONS_SECURITY_GROUP_ID" --protocol tcp --port 22 --cidr "$_ICC_COMMONS_MY_IP/32" 2>&1`
	if [ "$?" -ne 0 ]; then
		[ `grep "(InvalidPermission.Duplicate) <<< $AUTH_ERR"` ] || die "Failed to authorize SSH inbound traffic."
	else
		_ICC_COMMONS_SSH_AUTHORIZED=true
	fi
}

function revoke_ssh {
	log "Revoking SSH inbound traffic authorization"
	[ -n "$_ICC_COMMONS_MY_IP" ] || return
	[ -n "$_ICC_COMMONS_SECURITY_GROUP_ID" ] || return
	"$_ICC_COMMONS_AWS" ec2 revoke-security-group-ingress --group-id "$_ICC_COMMONS_SECURITY_GROUP_ID" --protocol tcp --port 22 --cidr "$_ICC_COMMONS_MY_IP/32" || die "Failed to revoke the SSH inbound traffic authorization."
}

function terminate_cluster {
	[ -n "$_ICC_COMMONS_CLUSTER_ID" ] || return
	log "Terminating the cluster"
	"$_ICC_COMMONS_AWS" emr terminate-clusters --cluster-ids "$_ICC_COMMONS_CLUSTER_ID" || die "Failed to terminate the cluster."
}

function wait_cluster_running {
	[ -n "$_ICC_COMMONS_CLUSTER_ID" ] || return
	log "Waiting for the cluster to start"
	echo "Type CTRL+C to abort."
	"$_ICC_COMMONS_AWS" emr wait cluster-running --cluster-id "$_ICC_COMMONS_CLUSTER_ID" || die "Cluster timed out."
	echo "The cluster has started."
}

function wait_cluster_terminated {
	[ -n "$_ICC_COMMONS_CLUSTER_ID" ] || return
	log "Waiting for the cluster to terminate its job"
	echo "Type CTRL+C to abort."
	"$_ICC_COMMONS_AWS" emr wait cluster-terminated --cluster-id "$_ICC_COMMONS_CLUSTER_ID" || die "Cluster timed out."
	echo "The cluster has terminated its job."
}

function create_key {
	log "Creating EC2 key pair '$_ICC_COMMONS_KEY_NAME'"
	"$_ICC_COMMONS_AWS" ec2 delete-key-pair --key-name "$_ICC_COMMONS_KEY_NAME" >& /dev/null
	rm -f "$_ICC_COMMONS_KEY_FILE"
	_ICC_COMMONS_KEY_CREATED=true
	"$_ICC_COMMONS_AWS" ec2 create-key-pair --key-name "$_ICC_COMMONS_KEY_NAME" --query "KeyMaterial" > "$_ICC_COMMONS_KEY_FILE" || die "Failed to create EC2 key pair."
	chmod 400 "$_ICC_COMMONS_KEY_FILE"
}

function delete_key {
	log "Deleting EC2 key pair"
	"$_ICC_COMMONS_AWS" ec2 delete-key-pair --key-name "$_ICC_COMMONS_KEY_NAME" || die "Failed to delete EC2 key pair."
	rm -f "$_ICC_COMMONS_KEY_FILE"
}

function create_bucket {
	log "Creating S3 bucket '$_ICC_COMMONS_BUCKET_NAME'"
	"$_ICC_COMMONS_AWS" s3 rb "$BUCKET" --force >& /dev/null
	_ICC_COMMONS_BUCKET_CREATED=true
	"$_ICC_COMMONS_AWS" s3 mb "$BUCKET" || die "Failed to create S3 bucket."
}

function delete_bucket {
	log "Deleting S3 bucket"
	"$_ICC_COMMONS_AWS" s3 rb "$BUCKET" --force || die "Failed to delete S3 bucket."
}

function fin_cleanup {
	[ "$_ICC_COMMONS_CLUSTER_CREATED" = true ] && terminate_cluster
	[ "$_ICC_COMMONS_KEY_CREATED" = true ] && delete_key
	[ "$_ICC_COMMONS_BUCKET_CREATED" = true ] && delete_bucket
	[ "$_ICC_COMMONS_SSH_AUTHORIZED" = true ] && revoke_ssh
}

function cluster_ssh {
	"$_ICC_COMMONS_AWS" emr ssh --cluster-id "$_ICC_COMMONS_CLUSTER_ID" --key-pair-file "$_ICC_COMMONS_KEY_FILE" --command "$1" | tail -n +2 || die "Failed to run command on remote machine."
}

function cluster_dl {
	"$_ICC_COMMONS_AWS" emr get --cluster-id "$_ICC_COMMONS_CLUSTER_ID" --key-pair-file "$_ICC_COMMONS_KEY_FILE" --src "$1" --dest "$2" > /dev/null || die "Download from remote machine failed."
}

function cluster_ul {
	"$_ICC_COMMONS_AWS" emr put --cluster-id "$_ICC_COMMONS_CLUSTER_ID" --key-pair-file "$_ICC_COMMONS_KEY_FILE" --src "$1" --dest "$2" > /dev/null || die "Upload to remote machine failed."
}

function bucket_ul {
	"$_ICC_COMMONS_AWS" s3 cp "$1" "$BUCKET/$2" || die "Upload to S3 bucket failed."
}

function bucket_dl {
	"$_ICC_COMMONS_AWS" s3 cp "$BUCKET/$1" "$2" || die "Download from S3 bucket failed."
}

function extract_results {
	tar xf "$RES_TEMP_TGZ" -C "$OUTPUT_DIR" --overwrite || die "Extraction failed."
}
