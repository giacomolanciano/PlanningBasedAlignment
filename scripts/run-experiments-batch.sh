#!/usr/bin/env bash

#######################################################
# author:	Giacomo Lanciano
# date:		26/09/2017
#
# descr:	Run a batch of planning-based alignment
#           experiments taking the PDDL files from the
#           given directory and applying the given
#           search strategy.
#######################################################


FD_OPT="fd-opt"
FD_SUB_OPT="fd-sub-opt"
SYMBA2="symba2"


function usage {
	printf "\nusage: $ run-experiments-batch /path/to/dir [{${FD_OPT}, ${FD_SUB_OPT}, ${SYMBA2}}]\n\n" 1>&2
}


# check if the mandatory arguments are passed
if [[ $# < 1 || $# > 2 ]]; then
	usage
    exit 1
fi

# check if the first argument is an actual dir
if [[ ! -d $1 ]]; then
	usage
    exit 1
fi

# set the search strategy depending on the second argument
search=${SYMBA2}
if [[ $# == 2 ]]; then
    if [[ $2 != ${FD_OPT} && $2 != ${FD_SUB_OPT} && $2 != ${SYMBA2} ]]; then
        usage
        exit 1
    fi
    search=$2
fi

# iterate over the dirs contained in the given dir
# NOTE: typing Ctrl-C during the loop only stops the current python sub-process (and start the next).
# NOTE: typing Ctrl-Z during the loop stops the entire process without printing the stats of the
# current python sub-process.
# FIXME (doesn't work if any of the dir names contains a space)
for filename in `find $1 -maxdepth 1 -mindepth 1 -type d | sort`; do
    # get dir absolute path
    complete_file_path=`readlink -e ${filename}`

    # launch planner manager script
    python fd_manager.py ${complete_file_path} -s ${search}
done

exit 0
