#!/bin/bash

# Spark setup

# Create .profile
touch ~/.profile

# Create temporary directory
mkdir .install_temp
cd .install_temp

# Install OpenJDK 8
echo "-- Installing OpenJDK 8..."
sudo apt -qq update
sudo apt -qq install openjdk-8-jdk -y
echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64" >> ~/.profile

# Install Scala 2.12.13
echo "-- Installing Scala 2.12.13..."
wget https://downloads.lightbend.com/scala/2.12.13/scala-2.12.13.deb -O .scala.deb -nv --show-progress
sudo dpkg -i .scala.deb
rm -f .scala.deb

# Install Hadoop 3.3.0
echo "--- Installing Hadoop 3.3.0..."
wget https://downloads.apache.org/hadoop/common/hadoop-3.3.0/hadoop-3.3.0-aarch64.tar.gz -O .hadoop.tgz -nv --show-progress
mkdir .hadoop
tar xf .hadoop.tgz -C .hadoop
rm -f .hadoop.tgz
sudo mv .hadoop/* /opt/hadoop
rm -rf .hadoop
echo "export HADOOP_HOME=/opt/hadoop" >> ~/.profile
echo "export PATH=\$PATH:\$HADOOP_HOME/bin" >> ~/.profile

# Install Spark 3.1.1 for Hadoop 3.2
echo "--- Installing Spark 3.1.1..."
wget https://downloads.apache.org/spark/spark-3.1.1/spark-3.1.1-bin-hadoop3.2.tgz -O .spark.tgz -nv --show-progress
mkdir .spark
tar xf .spark.tgz -C .spark
rm -f .spark.tgz
sudo mv .spark/* /opt/spark
rm -rf .spark
echo "export SPARK_HOME=/opt/spark" >> ~/.profile
echo "export PATH=\$PATH:\$SPARK_HOME/bin:\$SPARK_HOME/sbin" >> ~/.profile

# Remove temporary directory
echo "-- Cleaning up..."
cd ..
rm -rf .install_temp

# Update shell
source ~/.profile

echo "-- Done."