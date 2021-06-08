#!/bin/bash

# SSH setup

echo "-- Setting up SSH"

if ! command -v ssh-keygen &> /dev/null
then
    echo "SSH is required. Install it and try again."
    echo "-- Failed"
    exit 1
fi

ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa -q
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
chmod 0600 ~/.ssh/authorized_keys

echo "-- Done"