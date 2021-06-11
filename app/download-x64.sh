#!/bin/bash

#############################
### Download x64 assembly ###
#############################

# Commons

HELP_DESC="Download the latest prebuilt JAR for x64 machines"
ARGS_NAME=(OUTPUT_FILE)
ARGS_HELP=("the output JAR")
ARGS_DEF=("assembly-x64.jar")

. `dirname "$0"`/../.commons.sh
req curl

OUTPUT_FILE=${ARGS[OUTPUT_FILE]}

function fail_cleanup {
	rm -f "$OUTPUT_FILE"
}

# Download

log "Downloading prebuilt x64 JAR into '$OUTPUT_FILE'"
curl -o "$OUTPUT_FILE" -L "https://github.com/francescozoccheddu/big-data-project/releases/download/v0.1-alpha/assembly-x64.jar" || die "Download failed."