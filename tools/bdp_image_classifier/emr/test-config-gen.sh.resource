#!/bin/bash

set -e

DATASET_DIR="dataset"
bdp-ic-datasets-download "%DATASET%" -o "$DATASET_DIR" >&2
bdp-ic-datasets-reconfigure "$DATASET_DIR" --data-cwd "$(realpath "$DATASET_DIR")"
cat "$DATASET_DIR/config.json"