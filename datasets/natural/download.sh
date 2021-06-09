#!/bin/bash

# 'natural' dataset downloader

OUTPUT_DIR="${1:-dataset}"
mkdir -p "$OUTPUT_DIR"
THIS_FILE=`realpath "$0"`
THIS_DIR=`dirname "$THIS_FILE"`

echo "-- Downloading 'natural' dataset into '`realpath "$OUTPUT_DIR"`'"

cd "$OUTPUT_DIR"

curl -o .dataset.zip "https://storage.googleapis.com/kaggle-data-sets/111880/269359/bundle/archive.zip?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=gcp-kaggle-com%40kaggle-161607.iam.gserviceaccount.com%2F20210609%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20210609T084906Z&X-Goog-Expires=259199&X-Goog-SignedHeaders=host&X-Goog-Signature=a23f86d74d20099ddb3916263ef867112be25ae5f85393c266ffba064eac67fa18848bebabfd90c581a80f8f1e4b3335f2edbe3d863b61f6a3731df29d65af782965d5b284609192806106fddfd93e23262623ea3bab267c1eb4ef8729cc9990cbd509027a2faeb4b0a7a2aca3d8753cd290cad0456bc69a45ca6aa33b2dcf7e85253e97d9e4235a058c4ce1d8a1bb05e9484ff56f66d33c0fa83142996d846ff8749f6c3bd6f62965d321dac290fb0cd0845483dc1de452240992d0318597416d438e8177f81ddf9a909e2ee83509b960eec23dd3862953acb7fd8b4bdc0001968b1ebb37ffe861b4efb6a0d9dbc0a066aaa9c1c7089281cceca85b08b050a1"

echo "-- Extracting dataset"

unzip -q -o .dataset.zip
rm -f .dataset.zip

mkdir -p "images"
rsync -a --remove-source-files "seg_test/seg_test/" "seg_train/seg_train/" "images"
rm -rf "seg_train" "seg_test" "seg_pred"
cp "$THIS_DIR/config.json.template" "config.json"

echo "-- Done"
echo "Use the following configuration file:"
echo `realpath "config.json"`
