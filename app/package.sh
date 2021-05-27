#!/bin/bash

# App packager

echo "-- Packaging $OUTPUT"

set -e
OUTPUT=`realpath "${1:-assembly.jar}"`
THIS_FILE=`realpath "$0"`
APP_DIR=`dirname "$THIS_FILE"`/image-classifier
INTERM="$APP_DIR/.intermediate_assembly.jar"
cd "$APP_DIR"
sbt --error "set assembly / assemblyOutputPath := file(\"$INTERM\")" assembly > /dev/null || { echo "-- Failed"; exit 1; }
mv -f "$INTERM" "$OUTPUT"

echo "-- Done"