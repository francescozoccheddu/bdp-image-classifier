#!/bin/bash

# Environment uninstaller

HOME=`realpath ~/.ic_env`

echo "-- Uninstalling environment in $HOME"

echo "-- Removing $HOME"
rm -rf "$HOME"

echo "-- Restoring ~/.profile"
sed -i '/ic_env/d' ~/.profile 

echo "-- Done"