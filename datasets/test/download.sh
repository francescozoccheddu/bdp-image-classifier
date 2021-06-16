#!/bin/bash

##############################
##### Download test data #####
##############################

# Commons

DATASET_URL="https://iplab.dmi.unict.it/MLC2018/dataset.zip"

function cleanup {
	rm -r -f ".images"
	rm -f "training_list.csv" "validation_list.csv" "testing_list_blind.csv" "README.txt"
}

. `dirname "$0"`/../.commons.sh

# Prepare

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
	for I in {2..15}; do
		unset OUTPUTS[$I]
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
