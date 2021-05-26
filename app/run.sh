#!/bin/bash

# App launcher

start-all.sh > /dev/null
spark-submit "$1" "$2"
stop-all.sh > /dev/null