#!/bin/bash

##############################
###### Datasets commons ######
##############################

# Commons

if [ "`basename \"$0\"`" = ".commons.sh" ]; then echo "You should not invoke this script directly. See README.md for help."; exit 1; fi

DATASET_NAME=$(basename $(dirname "$0"))

HELP_DESC="Download the '$DATASET_NAME' dataset"
ARGS_NAME=(OUTPUT_DIR)
ARGS_HELP=("the output directory")
ARGS_DEF=("dataset")

. `dirname "$0"`/../../.commons.sh
reqs curl unzip

reqf "$SDIR/config.json.template" "Did you move the script?"
reqf "$SDIR/config-emr.json.template" "Did you move the script?"

# Paths

OUTPUT_DIR_REL=${ARGS[OUTPUT_DIR]}
mkdir -p "$OUTPUT_DIR_REL"
cd "$OUTPUT_DIR_REL"
TMP=".dataset_$DATASET_NAME.zip"

function fin_cleanup {
	rmdir --ignore-fail-on-non-empty "`realpath .`"
}

# Download

log "Downloading dataset '$DATASET_NAME' into '$OUTPUT_DIR_REL'"

function fail_cleanup {
	rm -f "$TMP"
}

curl -o "$TMP" "$DATASET_URL" || die "Download failed."

# Extract

log "Extracting dataset"

unzip -q -o "$TMP" || die "Extraction failed."
rm -f "$TMP"

function fail_cleanup {
	cleanup
}

# Finalize

function succ_cleanup {
	cleanup
	cp "$SDIR/config.json.template" "config.json"
	cp "$SDIR/config-emr.json.template" "config-emr.json"

	echo "Use the following configuration file:"
	echo `realpath "config.json"`
}