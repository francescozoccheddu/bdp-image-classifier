#!/bin/bash

# Data downloader
set -e
echo "-- Downloading dataset"

pip install --user -qq --no-input kaggle
echo "Kaggle username:"
read USERNAME
echo "Kaggle password:"
read -s PASSWORD
export KAGGLE_USERNAME=$USERNAME
export KAGGLE_KEY=$PASSWORD
kaggle datasets download -d puneet6060/intel-image-classification --unzip -f ciao
unset KAGGLE_USERNAME
unset KAGGLE_KEY

echo "-- Done"