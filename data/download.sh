#!/bin/bash

# Data downloader

echo "-- Downloading dataset"

OUTPUT="${1:-dataset}"
THIS_DIR=`dirname "$0"`/
kaggle datasets download --unzip -o -p "$OUTPUT" -d puneet6060/intel-image-classification
rm -f $OUTPUT/intel-image-classification.zip
cp -f `dirname "$0"`"/config.json.template" "$OUTPUT/config.json"

echo "-- Done"
echo "Use the following configuration file:"
echo `realpath "$OUTPUT/config.json"`
