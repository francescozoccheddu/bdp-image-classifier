#!/bin/bash

# Data downloader

OUTPUT_DIR=`realpath "${1:-dataset}"`
THIS_FILE=`realpath "$0"`
THIS_DIR=`dirname "$THIS_FILE"`

echo "-- Downloading dataset into '$OUTPUT_DIR'"

mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR"

wget "https://iplab.dmi.unict.it/MLC2018/dataset.zip" -O .dataset.zip -nv --show-progress

echo "-- Extracting dataset (this may take some minutes)"

unzip -q -o .dataset.zip
rm -f .dataset.zip

function move {
	declare -A OUTPUTS
	LINES=`tr -d "\r" < "$1"`
	read -a LABELS <<< `(cut -d "," -f6 | xargs) <<< "$LINES"`
	read -a INPUTS <<< `(cut -d "," -f1 | xargs) <<< "$LINES"`
	for I in "${!LABELS[@]}"; 
	do
		LABEL=${LABELS[$I]}
		INPUT=${INPUTS[$I]}
		OUTPUTS[$LABEL]="${OUTPUTS[$LABEL]} .images/${INPUT}"
	done
	for LABEL in "${!OUTPUTS[@]}"
	do
		INPUT=${OUTPUTS[$LABEL]}
		mkdir -p "images/$2/$LABEL"
		#mv -t "images/$2/$LABEL" $INPUT
		mv -t images/$2/$LABEL $INPUT
	done
}

mv "images" ".images"
move "training_list.csv" "train"
move "validation_list.csv" "test"
rm -rf ".images"
rm -f "training_list.csv" "validation_list.csv" "testing_list_blind.csv" "README.txt"
cp "$THIS_DIR/config.json.template" "config.json"

echo "-- Done"
echo "Use the following configuration file:"
echo `realpath "config.json"`
