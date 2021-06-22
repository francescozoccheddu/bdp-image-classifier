#!/bin/bash

set -e
cd

echo "[JOB] Installing required tools"
export PATH="$PATH:/home/hadoop/.local/bin"
python3 -m pip install -q --user --upgrade pip
python3 -m pip install -q --user "https://github.com/francescozoccheddu/bdp-image-classifier/releases/download/latest/bdp_image_classifier-0.1-py3-none-any.whl"

echo "[JOB] Retrieving app assembly"
ASSEMBLY_FILE="assembly.jar"
bdp-ic-app-download x64 -o "$ASSEMBLY_FILE" -q

echo "[JOB] Generating configuration"
CG_SCRIPT_FILE="%CG_SCRIPT_FILE%"
chmod +x "$CG_SCRIPT_FILE"
CG_DIR="config_generation"
mkdir -p "$CG_DIR"
CONFIG_FILE="config.json"
(cd "$CG_DIR" && "$CG_SCRIPT_FILE") > "$CONFIG_FILE"
OUTPUT_DIR_NAME="results"
HDFS_OUTPUT_DIR="hdfs:///$OUTPUT_DIR_NAME"
mkdir -p "$OUTPUT_DIR_NAME"
bdp-ic-datasets-reconfigure --data-save "" --data-temp-file "hdfs:///temp" --featurization-save "" --training-save "$HDFS_OUTPUT_DIR/model" --testing-save "$HDFS_OUTPUT_DIR/summary" --testing-print true

echo "[JOB] Running app"
spark-submit "$ASSEMBLY_FILE" "$CONFIG_FILE" 2>&1 | tee "$OUTPUT_DIR_NAME/log"

echo "[JOB] Collecting results"
hadoop fs -get "$HDFS_OUTPUT_DIR"
tar -czf "%RESULTS_FILE%" "$OUTPUT_DIR_NAME"