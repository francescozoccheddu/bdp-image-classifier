#!/bin/bash

# Data downloader

echo "-- Downloading dataset"

kaggle datasets download -d puneet6060/intel-image-classification --unzip -f ciao

echo "-- Done"