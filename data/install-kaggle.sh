#!/bin/bash

# Kaggle CLI uninstaller

echo "-- Installing kaggle CLI"

if ! command -v pip &> /dev/null
then
    echo "Kaggle requires pip. Run 'sudo apt install pip' and try again."
    echo "-- Failed"
    exit 1
fi

pip install --user -qq --no-input kaggle

CREDFILE="~/.kaggle/kaggle.json"

if [ ! -f "$CREDFILE" ]; then
    echo "'$CREDFILE' file is needed for kaggle to work. You can generate it by selecting 'Generate new API Token' from your Kaggle.com account settings."
fi

echo "-- Done"