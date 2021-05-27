#!/bin/bash

# SSH setup

echo "-- Setting up SSH"

sudo apt-get -qq update >& /dev/null
sudo apt-get -qq install ssh sshd > /dev/null
ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa -q
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
chmod 0600 ~/.ssh/authorized_keys

echo "-- Done"