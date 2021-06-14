#!/bin/bash

# Check EMR
USR="hadoop"
[ "`whoami`" = "$USR" ] || { echo "Expected '$USR' user"; exit 1; }
. /etc/os-release
[ "$ID" = "amzn" ] || { echo "Expected an Amazon Linux distribution"; exit 1; }
[ "$#" -eq "1" ] || { echo "Expected 1 argument but got $#"; exit 1; }

# Exit on error
set -e -v -x

# Retrieve repository
DIR="/home/$USR/image-classifier"
mkdir -p "$DIR"
cd "$DIR"
TMP_ZIP=".repo.zip"
curl -o "$TMP_ZIP" "https://github.com/francescozoccheddu/big-data-project/archive/refs/heads/main.zip"
unzip -o "$TMP_ZIP"
rm -f "$TMP_ZIP"

# Start app
ASSEMBLY="assembly-x64.jar"
DATASET="dataset"
"big-data-project/app/download-x64.sh" "$ASSEMBLY"
"big-data-project/dataset/$1/download.sh" "$DATASET"
spark-submit "$ASSEMBLY" "$DATASET/config-emr.json"