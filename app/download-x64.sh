#!/bin/bash

#############################
### Download x64 assembly ###
#############################

# Commons

HELP_DESC="Download the latest prebuilt JAR for x64 machines"
ARGS_NAME=(OUTPUT_FILE)
ARGS_HELP=("the output JAR")
ARGS_DEF=("assembly.jar")

. `dirname "$0"`/../.commons.sh
req curl

OUTPUT_FILE=${ARGS[OUTPUT_FILE]}

# Paths

#TODO