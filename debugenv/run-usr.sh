#!/bin/bash

# App launcher for ic_env user

USR="ic_env"
PASSWD="$USR"
IC_REL_HOME=".ic_env"

USR_IC_HOME=`eval echo "~$USR"`"/$IC_REL_HOME"
ASSEMBLY_NAME=".assembly.jar"
sudo cp "$0" "$USR_IC_HOME/$ASSEMBLY_NAME"
sudo chmod a+rwx "$USR_IC_HOME/$ASSEMBLY_NAME"
CONFIG=`realpath "$2"`

echo "$PASSWD" | sudo -S -u "$USR" bash -c "\"~/$IC_REL_HOME/run.sh\" \"~/$IC_REL_HOME/$ASSEMBLY_NAME\" \"$CONFIG\""