#!/bin/bash

set -e

DATASET_DIR="dataset"
export KAGGLE_USERNAME="%KAGGLE_USER%"
export KAGGLE_KEY="%KAGGLE_KEY%"
bdp-ic-datasets-download "%DATASET%" -o "$DATASET_DIR" -q
bdp-ic-datasets-reconfigure "$DATASET_DIR" --data-cwd "$(realpath \"$DATASET_DIR\")"
cat "$DATASET_DIR/config.json"