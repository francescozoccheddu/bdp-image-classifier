#!/bin/bash

# App packager
set -e
OUTPUT=`realpath "${1:-assembly.jar}"`
THIS_FILE=`realpath "$0"`
APP_DIR=`dirname "$THIS_FILE"`/image-classifier
INTERM="$APP_DIR/.intermediate_assembly.jar"

echo "-- Packaging $OUTPUT"
cd "$APP_DIR"
sbt --warn "set assembly / assemblyOutputPath := file(\"$INTERM\")" assembly
mv -f "$INTERM" "$OUTPUT"

echo "-- Done"