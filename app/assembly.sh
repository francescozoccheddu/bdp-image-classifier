#!/bin/bash

##############################
######## Assembly app ########
##############################

# Commons

HELP_DESC="Assembly the classifier app into an executable JAR package"
ARGS_NAME=(OUTPUT_FILE)
ARGS_HELP=("the output JAR")
ARGS_DEF=("assembly.jar")

. `dirname "$0"`/../.commons.sh

OUTPUT_FILE=${ARGS[OUTPUT_FILE]}

req sbt "See 'https://www.scala-sbt.org/download.html' for more information."

# Paths

APP_DIR="$SDIR/image-classifier"
OUTPUT_FILE_TMP="$APP_DIR/.intermediate_assembly.jar"
reqd "$APP_DIR" "Did you move the script?"

# Assembly 

log "Assembling '$OUTPUT_FILE'"
cd "$APP_DIR"
sbt --error "set assembly / assemblyOutputPath := file(\"$OUTPUT_FILE_TMP\")" assembly || die "Compilation failed."
mv -f "$OUTPUT_FILE_TMP" "$OUTPUT_FILE"