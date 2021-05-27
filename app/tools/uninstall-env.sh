#!/bin/bash

# Environment uninstaller

IC_HOME=`realpath ~/.ic_env`

echo "-- Uninstalling environment in $IC_HOME"

echo "-- Removing $IC_HOME"
rm -rf "$IC_HOME"

echo "-- Restoring ~/.profile"
sed -i '/# ic_env profile/d' ~/.profile 

echo "-- Done"