#!/bin/bash

# SBT setup

echo "-- Installing SBT"

echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list > /dev/null
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list > /dev/null
wget -qO- "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add >& /dev/null
sudo apt-get -qq update >& /dev/null
sudo apt-get -qq install sbt -y > /dev/null || { echo "-- Failed"; exit 1; }

echo "-- Done"
