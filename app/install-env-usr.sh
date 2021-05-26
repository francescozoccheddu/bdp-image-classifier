#!/bin/bash

# Environment installer for ic_env user

USR="ic_env"
PASSWD="$USR"
echo "-- Installing environment as user $USR"

if id "$USR" &>/dev/null; then
    echo "User $USR already exists. Run uninstall-env-usr.sh and try again."
	echo "-- Failed."
	exit 1
fi

echo "-- Creating user $USR"
sudo adduser --disabled-password --gecos "Environment for image classifier" $USR > /dev/null
echo "$USR:$PASSWD" | sudo chpasswd >& /dev/null

THIS_FILE=`realpath "$0"`
ICHOME=`eval echo "~$USR"`
SCRIPT_SRC=`dirname "$THIS_FILE"`/install-env.sh
SCRIPT_DST_NAME=".install-env.sh"
SCRIPT_DST="$ICHOME/$SCRIPT_DST_NAME"
sudo cp "$SCRIPT_SRC" "$SCRIPT_DST"
sudo chmod a+rwx "$SCRIPT_DST"
echo "$PASSWD" | sudo -S -u "$USR" bash -c "\"~/$SCRIPT_DST_NAME\" && rm -f \"~/$SCRIPT_DST_NAME\""

echo "-- Done."