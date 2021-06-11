#!/bin/bash

#############################
########## Commons ##########
#############################

if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then echo "Bash version 4 or later is required."; exit 1; fi

_IC_COMMONS_CANCELED=false
_IC_COMMONS_CLEANEDUP=false

function log {
	echo "-- $1"
}

function fin_cleanup { :; }
function fail_cleanup { :; }
function fail_nocanc_cleanup { :; }
function succ_cleanup { :; }
function fin_nocanc_cleanup { :; }

function _ic_commons_exit {
	local RES=$?
	if [ "$RES" -eq "0" ]; then
		succ_cleanup
		log "Done"
	else
		if [ "$_IC_COMMONS_CLEANEDUP" = false ]; then
			fail_nocanc_cleanup
		fi
		fail_cleanup
		log "Failed"
	fi
	if [ "$_IC_COMMONS_CLEANEDUP" = false ]; then
		fin_nocanc_cleanup
	fi
	fin_cleanup
	exit $?
}

trap _ic_commons_exit EXIT

function canc_cleanup { :; }

function _ic_commons_cleanup {
	if [ "$_IC_COMMONS_CLEANEDUP" = false ]; then
		_IC_COMMONS_CLEANEDUP=true
		canc_cleanup
	fi	
}

function die {
	echo "$1" 1>&2
	_ic_commons_cleanup
	exit 1
}

function _ic_commons_cancel {
	if [ "$_IC_COMMONS_CANCELED" = false ]; then
		_IC_COMMONS_CANCELED=true
		printf "\n"
		die "Canceled."
	fi
}

trap _ic_commons_cancel SIGHUP SIGINT SIGTERM

function req {
	command -v $1 &> /dev/null || die "$1 is required. $2"
}

function reqs {
	for CMD in "$@"
	do
		req $CMD
	done
}

function reqf {
	[ -f "$1" ] || die "File '$1' does not exist. $2"
}

function reqd {
	[ -d "$1" ] || die "Directory '$1' does not exist. $2"
}

req realpath

_IC_COMMONS_THIS_FILE=`realpath "$0"`
SDIR=`dirname "$_IC_COMMONS_THIS_FILE"`
SDIR_REL=`dirname "$0"`

function _ic_commons_help {
	echo "$HELP_DESC. Usage:"
	local OPTS=""
	local MAXLEN="20"
	local OPT
	for OPT in ${ARGS_NAME[@]}; do
		if [ -n "$OPTS" ]; then
			OPTS="$OPTS "
		fi
		OPTS="$OPTS<$OPT>"
		local LEN=${#OPT}
		if [ "$LEN" -gt "$MAXLEN" ]; then
			MAXLEN="$LEN"
		fi
	done
	if [ -z "${ARGS_DEF+x}" ]; then
		if [ -z "$OPTS" ]; then
			echo "$0 -h"
		else
			echo "$0 (-h|$OPTS)"
		fi
	else
		echo "$0 [-h|$OPTS]"
	fi
	echo "Options:"
	printf "%${MAXLEN}s  %s\n" "-h" "print this help message"
	local I
	for I in ${!ARGS_NAME[@]}; do
		printf "%${MAXLEN}s  %s" "${ARGS_NAME[$I]}" "${ARGS_HELP[$I]}"
		if [ -n "${ARGS_DEF[$I]}" ]; then
			printf " (defaults to '${ARGS_DEF[$I]}')"
		fi
		printf "\n"
	done
	trap '' EXIT
	exit
}

declare -A ARGS

while getopts ":h" OPT; do
	case $OPT in
		h) _ic_commons_help;;
	esac
done

_IC_COMMONS_I=0
for _IC_COMMONS_I in ${!ARGS_NAME[@]}; do
	_IC_COMMONS_ARGI=$((_IC_COMMONS_I+1))
	ARGS[${ARGS_NAME[$_IC_COMMONS_I]}]=${!_IC_COMMONS_ARGI:-${ARGS_DEF[$_IC_COMMONS_I]}}
done

if [ -z "${ARGS_DEF+x}" ]; then
	[ "$#" -eq "${#ARGS_NAME[@]}" ] || die "Expected ${#ARGS_NAME[@]} arguments but got $#. Run $0 -h for help."
else
	[ "$#" -eq "0" ] || [ "$#" -le "${#ARGS_NAME[@]}" ] || die "Expected 0 or ${#ARGS_NAME[@]} arguments but got $#. Run $0 -h for help."
fi