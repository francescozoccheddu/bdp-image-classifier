#!/bin/bash

set -e

DATASET_DIR="dataset"
export KAGGLE_USERNAME="%KAGGLE_USER%"
export KAGGLE_KEY="%KAGGLE_KEY%"
python3 -m bdp_image_classifier.datasets.download "%DATASET%" -o "$DATASET_DIR" -q
python3 -m bdp_image_classifier.datasets.reconfigure "$DATASET_DIR" --data-cwd "$(realpath \"$DATASET_DIR\")"
cat "$DATASET_DIR/config.json"