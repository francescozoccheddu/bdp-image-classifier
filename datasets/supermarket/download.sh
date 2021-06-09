#!/bin/bash

# 'supermarket' dataset downloader

OUTPUT_DIR="${1:-dataset}"
mkdir -p "$OUTPUT_DIR"
THIS_FILE=`realpath "$0"`
THIS_DIR=`dirname "$THIS_FILE"`

echo "-- Downloading 'supermarket' dataset into '`realpath "$OUTPUT_DIR"`'"

cd "$OUTPUT_DIR"

curl -o .dataset.zip "https://iplab.dmi.unict.it/MLC2018/dataset.zip"

echo "-- Extracting dataset"

unzip -q -o .dataset.zip
rm -f .dataset.zip

mv "images" ".images"

function move {
	declare -A OUTPUTS
	SOURCE=`tr -d "\r" < "$1"`
	read -a LABELS <<< `(cut -d "," -f6 | xargs) <<< "$SOURCE"`
	read -a INPUTS <<< `(cut -d "," -f1 | xargs) <<< "$SOURCE"`
	for I in "${!LABELS[@]}"; 
	do
		LABEL=${LABELS[$I]}
		INPUT=${INPUTS[$I]}
		OUTPUTS[$LABEL]="${OUTPUTS[$LABEL]} .images/${INPUT}"
	done
	for LABEL in "${!OUTPUTS[@]}"
	do
		INPUT=${OUTPUTS[$LABEL]}
		mkdir -p "images/$LABEL"
		mv -t "images/$LABEL" $INPUT
	done
}

move "training_list.csv"
move "validation_list.csv"
rm -r -f ".images"
rm -f "training_list.csv" "validation_list.csv" "testing_list_blind.csv" "README.txt"
cp "$THIS_DIR/config.json.template" "config.json"

echo "-- Done"
echo "Use the following configuration file:"
echo `realpath "config.json"`
