#!/bin/bash

# Assembly downloader

OUTPUT=`realpath "${1:-assembly.jar}"`

echo "-- Downloading '$OUTPUT'"

wget "https://github.com/francescozoccheddu/Big-Data-Project/releases/download/0.0.1/assembly.jar" -O $OUTPUT -nv --show-progress || { echo "-- Failed"; exit 1; }

echo "-- Done"

