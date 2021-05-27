#!/bin/bash

# SBT uninstaller

echo "-- Uninstalling SBT"

sudo apt-get -qqq purge sbt -y > /dev/null
sudo rm -f /etc/apt/sources.list.d/sbt.list
sudo rm -f /etc/apt/sources.list.d/sbt_old.list

echo "--- Done"