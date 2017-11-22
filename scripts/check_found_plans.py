#!/usr/bin/env python

from __future__ import absolute_import, division, print_function  # to run both on Python 2 and 3

import re
import sys
import time
from os import listdir
from os import path

INTEGER_NUMBER_REGEX = '\d+'
DECIMAL_NUMBER_REGEX = '\d+(,\d{3})*(\.\d+)*'
OUTPUT_SEARCH_TIME_ENTRY_PREFIX = '; searchtime = '
OUTPUT_PLAN_COST_ENTRY_PREFIX = '; cost = '
MILLISECS_PER_SECOND = 1000
DEFAULT_TIME_UNIT = ' ms'
INCOMPLETE_COMPUTATION_MSG = 'The computation has been interrupted before proper completion.'


def current_millisecs_time():
    return int(round(time.time() * 1000))


def format_src_dir(src_dir):
    if not path.exists(src_dir):
        raise OSError('The given directory does not exists.')

    # remove trailing path separators from input dir
    last_char = src_dir[-1]
    while last_char == '/' or last_char == '\\':
        src_dir = src_dir[:-1]
        last_char = src_dir[-1]

    return src_dir


if __name__ == '__main__':
    # parse arguments
    SRC_DIR = sys.argv[1]

    # prepare arguments
    SRC_DIR = format_src_dir(SRC_DIR)

    # prepare log files
    current_time = str(current_millisecs_time())
    SOLUTIONS_NOT_FOUND_FILE_NAME = 'sol_not_found_' + current_time + '.txt'

    # iterate over alignments files
    try:
        for alignment_file in [f for f in listdir(SRC_DIR) if path.isfile(path.join(SRC_DIR, f))]:
            # extract trace number
            basename = path.basename(alignment_file)
            trace_number = re.search(INTEGER_NUMBER_REGEX, basename).group(0)

            print('processing trace #{0:s}...'.format(trace_number))

            # parse alignment cost from file to check whether solution was found
            with open(path.join(SRC_DIR, alignment_file), 'r') as a_file:
                a_file_text = a_file.read()
                if not re.search(OUTPUT_PLAN_COST_ENTRY_PREFIX, a_file_text):
                    print('Solution not found for trace #{0:s}.'.format(trace_number))
                    with open(SOLUTIONS_NOT_FOUND_FILE_NAME, 'a') as sol_not_found_file:
                        sol_not_found_file.write(trace_number + '\n')

    except KeyboardInterrupt:
        pass
