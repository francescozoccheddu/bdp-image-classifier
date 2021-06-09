#!/bin/bash

# 'land' dataset downloader

OUTPUT_DIR="${1:-dataset}"
mkdir -p "$OUTPUT_DIR"
THIS_FILE=`realpath "$0"`
THIS_DIR=`dirname "$THIS_FILE"`

echo "-- Downloading 'land' dataset into '`realpath "$OUTPUT_DIR"`'"

cd "$OUTPUT_DIR"

curl -o .dataset.zip "https://storage.googleapis.com/kaggle-data-sets/915557/1552478/upload/images.zip?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=gcp-kaggle-com%40kaggle-161607.iam.gserviceaccount.com%2F20210609%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20210609T111531Z&X-Goog-Expires=259199&X-Goog-SignedHeaders=host&X-Goog-Signature=885f7293246b9389a61c0156f8253a3e262d79577c6321759a0792d65f28fa1b6adb721817532a6868f5f3aa6a669c33808caf3a0b0f4bc61363e794f8a1a56cc19336ac355fcd068f61bb44bdd1a632af424bd62706c85b16d0c425b95d98dc77a8d98927701a950ee35b2d06fca6c4913db2ca8e4614656effc910db6ffd25c20d879f80f8830055ee3d44a628d0af19bb4297d95b1b990cb4c7ef50a572e572040a7fb6b26782ea3151caae046d8b751c5add51f2c74f002d9e5313be52adcb37a49ca0262a2d5bc5f79aec4b6cead0c00d969847206e77f0dca8a1c26fd7af58b19df3b9a19661970df25aaa1e0d3373fabb6371486555399e300395df94"

echo "-- Extracting dataset"

unzip -q -o .dataset.zip -d "images"
rm -f .dataset.zip

cp "$THIS_DIR/config.json.template" "config.json"

echo "-- Done"
echo "Use the following configuration file:"
echo `realpath "config.json"`
