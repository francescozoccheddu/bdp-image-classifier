#!/bin/bash

# SSH setup

echo "-- Setting up SSH"
sudo apt -qq install ssh sshd
ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa -q
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
chmod 0600 ~/.ssh/authorized_keys

echo "-- Done"