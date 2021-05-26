#!/bin/bash

# App launcher

jps | grep Master > /dev/null
RES=$?

if [[ $RES != 0 ]]; then
	start-all.sh > /dev/null
fi

spark-submit "$1" "$2"

if [[ $RES != 0 ]]; then
	stop-all.sh > /dev/null
fi