#!/bin/bash

set -e

echo "[EMR] Retrieving app assembly"
ASSEMBLY_FILE="assembly.jar"
# pip install -q [...] # TODO
bdp-ic-app-download x64 -o "$ASSEMBLY_FILE"

echo "[EMR] Generating configuration"
OUTPUT_DIR_NAME="results"
HDFS_OUTPUT_DIR="hdfs:///$OUTPUT_DIR_NAME"
SCRIPT_FILE="%SCRIPT_FILE%"
mkdir -p "$OUTPUT_DIR_NAME"
chmod +x "$SCRIPT_FILE"
CONFIG_FILE="config.json"
"$SCRIPT_FILE" > "$CONFIG_FILE"
bdp-ic-datasets-reconfigure --data-save-file "" --data-temp-file "hdfs:///temp" --featurization-save-file "" --model-save-file "$HDFS_OUTPUT_DIR/model" --testing-save-file "$HDFS_OUTPUT_DIR/summary" --testing-print true

echo "[EMR] Running app"
spark-submit "$ASSEMBLY_FILE" "$CONFIG_FILE" | tee "$OUTPUT_DIR_NAME/log"

echo "[EMR] Collecting results"
hadoop fs -get "$HDFS_OUTPUT_DIR"
tar -czf "%RESULTS_FILE%" "$OUTPUT_DIR_NAME"