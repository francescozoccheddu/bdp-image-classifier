#!/bin/bash

# Environment uninstaller for ic_env user

USR=ic_env

echo "-- Removing $USR user.."
sudo userdel -f -r "$USR" >& /dev/null

echo "-- Done."