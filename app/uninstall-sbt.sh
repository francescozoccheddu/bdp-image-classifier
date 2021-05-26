#!/bin/bash

# SBT uninstaller

# Uninstall SBT
echo "-- Uninstalling SBT"
sudo apt -qq purge sbt -y
sudo rm -f /etc/apt/sources.list.d/sbt.list
sudo rm -f /etc/apt/sources.list.d/sbt_old.list

echo "--- Done"