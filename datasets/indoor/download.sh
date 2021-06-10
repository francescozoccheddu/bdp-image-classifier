#!/bin/bash

################################
##### Download indoor data #####
################################

# Commons

if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then echo "Bash version 4 or later is required."; exit 1; fi

CANCELED=false

function log {
	echo "-- $1"
}

function die {
	echo "$1"
	uninstall
	exit 1
}

function fail {
	echo "$1"
	uninstall
	log "Failed"
	exit 1
}

function req {
	command -v $1 &> /dev/null || die "$1 is required. $2"
}

function uninstall {

}

function cancel {
	if [ "$CANCELED" = false ]; then
		CANCELED=true
		fail "Canceled"
	fi
}

trap cancel SIGHUP SIGINT SIGTERM

# Input

ARGDEF_OUTPUT_DIR="dataset"

function help {
	echo "Download the 'indoor' dataset. Usage:"
	echo "$0 [-h|<OUTPUT_DIR>]"
	echo "Options:"
	echo "    -h            prints this help message"
	echo "    OUTPUT_DIR    the output directory (defaults to '$ARGDEF_OUTPUT_DIR')"
	exit
}

while getopts ":h" OPT; do
   case $OPT in
      h) help;;
   esac
done

[ "$#" -le 1 ] || die "Expected 0 or 1 arguments but got $#. Run $0 -h for help."

ARG_OUTPUT_DIR=${1:-$ARGDEF_OUTPUT_DIR}

# Paths

req realpath

mkdir -p "$ARG_OUTPUT_DIR"
THIS_FILE=`realpath "$0"`
THIS_DIR=`dirname "$THIS_FILE"`
cd "$OUTPUT_DIR"

# Download

req curl

log "Downloading 'indoor' dataset into '`realpath "$OUTPUT_DIR"`'"
curl -o .dataset.zip "https://storage.googleapis.com/kaggle-data-sets/358221/702372/bundle/archive.zip?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=gcp-kaggle-com%40kaggle-161607.iam.gserviceaccount.com%2F20210608%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20210608T160944Z&X-Goog-Expires=259199&X-Goog-SignedHeaders=host&X-Goog-Signature=383deb42dc6185e03b48fb466c06617a581ebd21593211c297c297fcf29280669ab640472b0b2e7505b759e2b057274829b1b9a3f100530db3cabe588d4fd448d6a4d67a86ba7a47d1b0ef02301db98bb970c57ade93811172b2f403b65cb91fed453e005a3446a136dd082d3ad3ccb3fc2a8715281b243fb1c2756eeecee5669a172c6a5efd41f563f3920c02f18dda8083cbe17db4487fd3f0dee0e76731cd7596690eb63541c71c95504c3e130e8de6e8981dac09bd5c51c84765ad0819d6152d3d676684b8702a7c93feac3d182aaec9d1411cd306295f28b01ad255a1fd9dd4be842260f56a2112a6aedcf3ad5547fbf7cf3dd3043b81b291b008ceaad9"

# Extract

echo "-- Extracting dataset"
unzip -q -o .dataset.zip
rm -f .dataset.zip

mv -f "indoorCVPR_09/Images" "images"
rm -rf "indoorCVPR_09" "indoorCVPR_09annotations"
rm -f "TestImages.txt" "TrainImages.txt"
cp "$THIS_DIR/config.json.template" "config.json"

echo "-- Done"
echo "Use the following configuration file:"
echo `realpath "config.json"`
