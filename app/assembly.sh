#!/bin/bash

# App assembler

OUTPUT=`realpath "${1:-assembly.jar}"`

echo "-- Assembling '$OUTPUT'"

if ! command -v sbt &> /dev/null
then
    echo "SBT is required. See 'https://www.scala-sbt.org/download.html' for more information."
    echo "-- Failed"
    exit 1
fi

THIS_FILE=`realpath "$0"`
APP_DIR=`dirname "$THIS_FILE"`/image-classifier
INTERM="$APP_DIR/.intermediate_assembly.jar"
cd "$APP_DIR"
sbt --error "set assembly / assemblyOutputPath := file(\"$INTERM\")" assembly || { echo "-- Failed"; exit 1; }
mv -f "$INTERM" "$OUTPUT"

echo "-- Done"