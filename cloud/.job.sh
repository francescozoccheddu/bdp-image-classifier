#!/bin/bash

# Exit on error
set -e -v -x

# Check EMR
USR="hadoop"
[ "`whoami`" = "$USR" ] || { echo "Expected '$USR' user"; exit 1; }
. /etc/os-release
[ "$ID" = "amzn" ] || { echo "Expected an Amazon Linux distribution"; exit 1; }
[ "$#" -eq "1" ] || { echo "Expected 1 argument but got $#"; exit 1; }

# Retrieve repository
DIR="/home/$USR/image-classifier"
mkdir -p "$DIR"
cd "$DIR"
TMP_ZIP=".repo.zip"
curl -o "$TMP_ZIP" -L "https://github.com/francescozoccheddu/big-data-project/archive/refs/heads/main.zip"
unzip -o "$TMP_ZIP"
rm -f "$TMP_ZIP"

# Start app
UID=`aws sts get-caller-identity | cut -f1`
CONFIG_WOS="francescozoccheddu-big-data-project-image-classifier-$UID/config-emr.json"
aws s3 cp "dataset/config-emr.json" "s3://$CONFIG_WOS"
"big-data-project-main/app/download-x64.sh" "assembly-x64.jar"
"big-data-project-main/datasets/$1/download.sh" "dataset"
spark-submit "assembly-x64.jar" "s3:///$CONFIG_WOS"