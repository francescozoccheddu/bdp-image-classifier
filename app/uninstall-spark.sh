#!/bin/bash

# Spark uninstaller

# Uninstall OpenJDK 8
echo "-- Uninstalling OpenJDK 8..."
sudo apt -qq purge openjdk-8-jdk -y
sed -i '/JAVA_HOME/d' ~/.profile  

# Uninstall Scala
echo "-- Uninstalling Scala..."
sudo apt -qq purge scala -y

# Uninstall Hadoop
echo "-- Uninstalling Hadoop..."
sudo rm -rf $HADOOP_HOME
sed -i '/HADOOP_HOME/d' ~/.profile  

# Uninstall Spark
echo "-- Uninstalling Spark..."
sudo rm -rf $SPARK_HOME
sed -i '/SPARK_HOME/d' ~/.profile  

# Cleanup
echo "-- Cleaning up..."
sudo apt -qq autoremove -y

# Update shell
source ~/.profile

echo "-- Done."