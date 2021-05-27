#!/bin/bash

# Environment installer for ic_env user

USR="ic_env"
PASSWD="$USR"
IC_REL_HOME=".ic_env"

echo "-- Installing environment as user $USR"

if id "$USR" &>/dev/null; then
    echo "User $USR already exists. Run uninstall-env-usr.sh and try again."
	echo "-- Failed"
	exit 1
fi

echo "-- Creating user $USR"
sudo adduser --disabled-password --gecos "Environment for image classifier" $USR > /dev/null
echo "$USR:$PASSWD" | sudo chpasswd >& /dev/null

function send {
	THIS_FILE=`realpath "$0"`
	THIS_DIR=`dirname "$THIS_FILE"`
	USR_IC_HOME=`eval echo "~$USR"`"/$IC_REL_HOME"
	sudo cp "$THIS_DIR/$0" "$USR_IC_HOME/$0"
	sudo chmod a+rwx "$USR_IC_HOME/$0"
}

send "install-env.sh"
send "run.sh"
send "uninstall-env.sh"
echo "$PASSWD" | sudo -S -u "$USR" bash -c "~/\"$IC_REL_HOME\"/install-env.sh"