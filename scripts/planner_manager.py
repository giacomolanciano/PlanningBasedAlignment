#!/usr/bin/env python

from __future__ import absolute_import, division, print_function  # to run both on Python 2 and 3

import glob
import re
import shutil
import subprocess
import sys
from os import path
from os import remove

DEV = False
PLANNER_MANAGER_ARGS_NUM = 4
PLANNER_INPUT_EXT = '.pddl'
DOMAIN_FILE_PATTERN = 'domain*'
PROBLEM_FILE_PATTERN = 'problem'
INTEGER_NUMBER_REGEX = '\d+'
DECIMAL_NUMBER_REGEX = '\-?\d+(,\d{3})*(\.\d+)*'
INPUT_SEARCH_TIME_ENTRY_PREFIX = 'Total time: ' + DECIMAL_NUMBER_REGEX
INPUT_EXPANDED_STATES_ENTRY_PREFIX = 'Expanded ' + DECIMAL_NUMBER_REGEX + ' state'
INPUT_GENERATED_STATES_ENTRY_PREFIX = 'Generated ' + DECIMAL_NUMBER_REGEX + ' state'
OUTPUT_SEARCH_TIME_ENTRY_PREFIX = '; searchtime = '
OUTPUT_EXPANDED_STATES_ENTRY_PREFIX = '; expandedstates = '
OUTPUT_GENERATED_STATES_ENTRY_PREFIX = '; generatedstates = '
MILLISECS_PER_SECOND = 1000

# positions expressed with respect to the ORIGINAL sys.argv
ROOT_DIR_POS = 1
SRC_DIR_POS = ROOT_DIR_POS + 1
DEST_DIR_POS = SRC_DIR_POS + 1

# positions expressed with respect to the TRUNCATED sys.argv
ALIGNMENT_FILE_POS = 5
DOMAIN_FILE_POS = 6
PROBLEM_FILE_POS = DOMAIN_FILE_POS + 1

if __name__ == '__main__':

    # the argument list has to match the following structure:
    #
    # ['< planner_manager_path >',  # to be discarded
    # '< root_dir_path >',
    # '< input_dir_path >',
    # '< output_dir_path >',
    # 'python',
    # '.../fast-downward/fast-downward.py',
    # '--build',
    # '<build_id>',
    # '--plan-file',
    # '< alignment_file_placeholder >',
    # '< domain_file_placeholder >',
    # '< problem_file_placeholder >',
    # '--heuristic',
    # '< chosen_heuristic >',
    # '--search',
    # '< chosen_strategy >']

    # to be kept equal to equivalent constants in ResultPerspective.java
    ROOT_DIR = sys.argv[ROOT_DIR_POS]
    SRC_DIR = sys.argv[SRC_DIR_POS]
    DEST_DIR = sys.argv[DEST_DIR_POS]
    DEST_FILE = path.join(DEST_DIR, 'alignment_')

    planner_args = sys.argv[PLANNER_MANAGER_ARGS_NUM:]

    for domain in glob.glob(path.join(SRC_DIR, DOMAIN_FILE_PATTERN)):
        # extract trace number
        basename = path.basename(domain)
        trace_number = re.search(INTEGER_NUMBER_REGEX, basename).group(0)

        print('processing trace #{0:s}...'.format(trace_number))

        # get correct file names for problem and output
        problem = path.join(SRC_DIR, PROBLEM_FILE_PATTERN + trace_number + PLANNER_INPUT_EXT)
        alignment = DEST_FILE + trace_number

        # exec planner (redirecting output)
        planner_args[ALIGNMENT_FILE_POS] = alignment
        planner_args[DOMAIN_FILE_POS] = domain
        planner_args[PROBLEM_FILE_POS] = problem
        process = subprocess.Popen(planner_args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        # get planner search time from process std out
        out, err = process.communicate()

        # log errors to stderr if needed
        if err:
            sys.stderr.write(err + '\n')

        # decode process output to prevent errors when running with Python 3
        out = out.decode('utf-8')

        # parse alignment time
        trace_alignment_time = re.search(INPUT_SEARCH_TIME_ENTRY_PREFIX, out).group(0)
        trace_alignment_time = re.search(DECIMAL_NUMBER_REGEX, trace_alignment_time).group(0)
        trace_alignment_time_ms = float(trace_alignment_time) * MILLISECS_PER_SECOND

        # parse alignment expanded states
        trace_alignment_expanded_states = re.search(INPUT_EXPANDED_STATES_ENTRY_PREFIX, out).group(0)
        trace_alignment_expanded_states = re.search(DECIMAL_NUMBER_REGEX, trace_alignment_expanded_states).group(0)

        # parse alignment generated states
        trace_alignment_generated_states = re.search(INPUT_GENERATED_STATES_ENTRY_PREFIX, out).group(0)
        trace_alignment_generated_states = re.search(DECIMAL_NUMBER_REGEX, trace_alignment_generated_states).group(0)

        # append stats to output file
        with open(alignment, 'a') as alignment_file:
            alignment_file.write(OUTPUT_SEARCH_TIME_ENTRY_PREFIX + str(trace_alignment_time_ms) + 'ms\n')
            alignment_file.write(OUTPUT_EXPANDED_STATES_ENTRY_PREFIX + str(trace_alignment_expanded_states) + '\n')
            alignment_file.write(OUTPUT_GENERATED_STATES_ENTRY_PREFIX + str(trace_alignment_generated_states))

        try:
            # remove domain and problem files
            if not DEV:
                remove(domain)
                remove(problem)
        except OSError:
            pass

    try:
        # cleanup
        remove(path.join(ROOT_DIR, 'output.sas'))

        # remove input directory
        if not DEV:
            shutil.rmtree(SRC_DIR)
    except OSError:
        print('Cannot properly remove temporary files.')
