#!/bin/bash

# App launcher for ic_env user

USR="ic_env"

ICHOME=`eval echo "~$USR"`
ASSEMBLY_DST_NAME=".assembly.jar"
ASSEMBLY_DST="$ICHOME/$ASSEMBLY_DST_NAME"
CONFIG=`realpath "$2"`
sudo cp "$1" "$ASSEMBLY_DST"
sudo chmod a+rwx "$ASSEMBLY_DST"
echo "$PASSWD" | sudo -S -u "$USR" bash -c "start-all.sh && spark submit \"~/ASSEMBLY_DST_NAME\" \"$CONFIG\" && rm -f \"~/$ASSEMBLY_DST_NAME\" && stop-all.sh"