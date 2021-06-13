#!/bin/bash

#############################
########## Commons ##########
#############################

if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then echo "Bash version 4 or later is required."; exit 1; fi
_IC_COMMONS_THIS_FILE_REL="$0"
if [ "`basename \"$_IC_COMMONS_THIS_FILE_REL\"`" = ".commons.sh" ]; then echo "You should not invoke this script directly. See README.md for help."; exit 1; fi

_IC_COMMONS_CANCELED=false
_IC_EXITED=false

function log {
	echo "-- $1"
}

function canc_cleanup { :; }
function fin_cleanup { :; }
function fail_cleanup { :; }
function fail_nocanc_cleanup { :; }
function succ_cleanup { :; }
function fin_nocanc_cleanup { :; }

function _ic_commons_exit {
	local RES=$?
	_IC_EXITED=true
	if [ "$RES" -eq "0" ]; then
		succ_cleanup
		log "Done"
	else
		if [ "$_IC_COMMONS_CANCELED" = false ]; then
			fail_nocanc_cleanup
		fi
		fail_cleanup
		log "Failed"
	fi
	if [ "$_IC_COMMONS_CANCELED" = false ]; then
		fin_nocanc_cleanup
	fi
	fin_cleanup
	exit $?
}

trap _ic_commons_exit EXIT

function die {
	echo "$1" 1>&2
	if [ "$_IC_EXITED" = false ]; then
		exit 1
	fi
}

function dieh {
	die "$1 Run $_IC_COMMONS_THIS_FILE_REL -h for help."
}

function _ic_commons_cancel {
	trap '' SIGHUP SIGINT SIGTERM
	if [ "$_IC_COMMONS_CANCELED" = false ]; then
		_IC_COMMONS_CANCELED=true
		printf "\n"
		canc_cleanup
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

_IC_COMMONS_THIS_FILE=`realpath "$_IC_COMMONS_THIS_FILE_REL"`
SDIR=`dirname "$_IC_COMMONS_THIS_FILE"`
SDIR_REL=`dirname "$_IC_COMMONS_THIS_FILE_REL"`

declare -A ARGS

function _ic_commons_parse_args {
	[ -n "$HELP_DESC" ] || die "Empty HELP_DESC."
	[ "${#ARGS_NAME[@]}" -eq "${#ARGS_DEF[@]}" ] || die "#ARGS_NAME != #ARGS_DEF."
	[ "${#ARGS_NAME[@]}" -eq "${#ARGS_HELP[@]}" ] || die "#ARGS_NAME != #ARGS_HELP."
	[ "${#OPTS_NAME[@]}" -eq "${#OPTS_DEF[@]}" ] || die "#OPTS_NAME != #OPTS_DEF." 
	[ "${#OPTS_NAME[@]}" -eq "${#OPTS_HELP[@]}" ] || die "#OPTS_NAME != #OPTS_HELP." 
	[ "${#OPTS_NAME[@]}" -eq "${#OPTS_FLAG[@]}" ] || die "#OPTS_NAME != #OPTS_FLAG." 
	local I
	local NAME
	local FLAG
	local DEF
	local GO_FORM="h"
	local MAXLEN=6
	local MANDATORY=0
	declare -A FLAGMAP
	for I in ${!OPTS_NAME[@]}; do
		NAME=${OPTS_NAME[$I]}
		[ -n "$NAME" ] || die "Empty argument name."
		[ ${ARGS[$NAME]+x} ] && die "Duplicate '$NAME' option name."
		[ "${#NAME}" -lt "$MAXLEN" ] || MAXLEN="${#NAME}"
		FLAG=${OPTS_FLAG[$I]}
		[ "${#FLAG}" -eq "1" ] || die "Invalid '$FLAG' option flag."
		[[ "$FLAG" =~ [a-zA-Z] ]] || die "Not a letter option flag."
		[ "$FLAG" != "h" ] || die "Reserved 'h' option flag."
		FLAGMAP[$FLAG]=$NAME
		DEF=${OPTS_DEF[$I]}
		ARGS[$NAME]=$DEF
		GO_FORM="$GO_FORM$FLAG:"
		[ -z "$DEF" ] || GO_FORM="$GO_FORM:"
	done
	local HAS_DEF=false
	for I in ${!ARGS_NAME[@]}; do
		NAME=${ARGS_NAME[$I]}
		[ -n "$NAME" ] || "Empty argument name"
		[ ${ARGS[$NAME]+x} ] && die "Duplicate '$NAME' argument."
		[ "${#NAME}" -lt "$MAXLEN" ] || MAXLEN="${#NAME}"
		DEF=${ARGS_DEF[$I]}
		if [ -n "$DEF" ]; then 
			HAS_DEF=true
		else
			if [ "$HAS_DEF" = true ]; then die "Mandatory argument '$NAME' after optional arguments."; fi
			MANDATORY=$((MANDATORY+1))
		fi
		ARGS[$NAME]=$DEF
	done
	while getopts ":$GO_FORM" FLAG; do
		case $FLAG in
			h) _ic_commons_help;;
			"?") dieh "Unknown option -$OPTARG.";;
			*) ARGS[${FLAGMAP[$FLAG]}]=$OPTARG;;
		esac
	done
	shift $((OPTIND - 1))
	if [ "$MANDATORY" -eq "${#ARGS_NAME[@]}" ]; then
		local ERR="Expected $MANDATORY arguments but got $#."
	else
		local ERR="Expected $MANDATORY to ${#ARGS_NAME[@]} arguments but got $#."
	fi
	( [ "$#" -ge "$MANDATORY" ] && [ "$#" -le "${#ARGS_NAME[@]}" ] ) || dieh "$ERR"
	for (( I=0; I < $#; I++ )); do
		local ARGI=$((I+1))
		ARGS[${ARGS_NAME[$I]}]=${!ARGI}
	done
}

function _ic_commons_help {
	echo "$HELP_DESC. Usage:"
	printf "%s [-h]" "$_IC_COMMONS_THIS_FILE_REL"
	local NAME
	local DEF
	local I
	local DEFS=0
	for I in ${!OPTS_NAME[@]}; do
		printf " [-%s <%s>]" "${OPTS_FLAG[$I]}" "${OPTS_NAME[$I]}"
	done
	for I in ${!ARGS_NAME[@]}; do
		NAME=${ARGS_NAME[$I]}
		if [ -n "${ARGS_DEF[$I]}" ]; then
			DEFS=$((DEFS+1))
			printf " [<$NAME>" 
		else
			printf " <$NAME>"
		fi
	done
	for ((I=0;I<DEFS;I++)); do
		printf "]"
	done
	printf "\n"
	echo "Arguments:"
	local SPACING=$((MAXLEN+5))
	printf "%${SPACING}s  %s\n" "-h" "prints this help message"
	for I in ${!OPTS_NAME[@]}; do
		printf "%${SPACING}s  %s" "-${OPTS_FLAG[$I]} <${OPTS_NAME[$I]}>" "${OPTS_HELP[$I]}"
		if [ -n "${OPTS_DEF[$I]}" ]; then
			printf " (defaults to '${OPTS_DEF[$I]}')"
		fi
		printf "\n"
	done
	for I in ${!ARGS_NAME[@]}; do
		printf "%${SPACING}s  %s" "<${ARGS_NAME[$I]}>" "${ARGS_HELP[$I]}"
		if [ -n "${ARGS_DEF[$I]}" ]; then
			printf " (defaults to '${ARGS_DEF[$I]}')"
		fi
		printf "\n"
	done
	trap '' EXIT
	exit
}

_ic_commons_parse_args $@