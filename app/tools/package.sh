#!/bin/bash

# App packager

OUTPUT=`realpath "${1:-assembly.jar}"`
SCRIPT_FILE=`realpath "$0"`
SCRIPT_DIR=`dirname "$SCRIPT_FILE"`
APP_DIR=`dirname "$SCRIPT_DIR"`/image-classifier
INTERM="$APP_DIR/.intermediate_assembly.jar"

echo "-- Packaging $OUTPUT"
cd "$APP_DIR"
sbt --warn "set assembly / assemblyOutputPath := file(\"$INTERM\")" assembly
mv -f "$INTERM" "$OUTPUT"

echo "-- Done"