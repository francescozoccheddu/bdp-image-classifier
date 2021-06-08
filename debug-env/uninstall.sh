#!/bin/bash

# Environment uninstaller

IC_HOME=`realpath ~/.image-classifier-debug-env`

echo "-- Uninstalling environment in $IC_HOME"

echo "-- Removing $IC_HOME"
rm -rf "$IC_HOME"

echo "-- Restoring ~/.profile"
sed -i '/# image-classifier-debug-env profile/d' ~/.profile 

echo "-- Done"