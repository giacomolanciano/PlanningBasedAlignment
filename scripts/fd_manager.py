#!/usr/bin/env python

from __future__ import absolute_import, division, print_function  # to run both on Python 2 and 3

import argparse
import glob
import re
import subprocess
import sys
import time
from os import mkdir
from os import path
from os import remove

from runstats import Statistics

FD_OPT = 'fd-opt'
FD_SUB_OPT = 'fd-sub-opt'
SYMBA2 = 'symba2'
PLANNER_INPUT_EXT = '.pddl'
DOMAIN_FILE_PATTERN = 'domain*'
PROBLEM_FILE_PATTERN = 'problem'
INTEGER_NUMBER_REGEX = '\d+'
DECIMAL_NUMBER_REGEX = '\d+(,\d{3})*(\.\d+)*'
INPUT_SEARCH_TIME_ENTRY_PREFIX = 'Total time: ' + DECIMAL_NUMBER_REGEX
OUTPUT_SEARCH_TIME_ENTRY_PREFIX = '; searchtime = '
OUTPUT_PLAN_COST_ENTRY_PREFIX = '; cost = '
MILLISECS_PER_SECOND = 1000
DEFAULT_TIME_UNIT = ' ms'
INCOMPLETE_COMPUTATION_MSG = 'The computation has been interrupted before proper completion.'
REPORT_FILE_NAME = 'report.txt'


# planner script arguments
PLANNER_ARGS = [
    'python',
    'fast-downward.py',
    '--build',
    'release64',
    '--plan-file',
    '< alignment_file_placeholder >',
    '< domain_file_placeholder >',
    '< problem_file_placeholder >',
]
ALIGNMENT_FILE_POS = 5
DOMAIN_FILE_POS = ALIGNMENT_FILE_POS + 1
PROBLEM_FILE_POS = DOMAIN_FILE_POS + 1


def current_millisecs_time():
    return int(round(time.time() * 1000))


def init_arg_parser():
    parser = argparse.ArgumentParser(description='Launch the planner on a set of PDDL domain/problem pairs.')
    parser.add_argument(
        'src_dir', metavar='directory', type=str,
        help='The directory where PDDL input files are stored.')
    parser.add_argument(
        '-s', '--search', dest='search', choices=[FD_OPT, FD_SUB_OPT, SYMBA2], default=SYMBA2, type=str,
        help='The search strategy to be used by the planner.')
    return parser


def format_src_dir(src_dir):
    if not path.exists(src_dir):
        arg_parser.print_usage()
        print()
        raise OSError('The given directory does not exists.')

    # remove trailing path separators from input dir
    last_char = src_dir[-1]
    while last_char == '/' or last_char == '\\':
        src_dir = src_dir[:-1]
        last_char = src_dir[-1]

    return src_dir


def get_planner_args_list(search):
    if search == FD_OPT:
        return PLANNER_ARGS + ['--heuristic', 'hcea=cea()', '--search', 'astar(blind())']
    elif search == FD_SUB_OPT:
        return PLANNER_ARGS + ['--heuristic', 'hhmax=hmax()', '--search', 'lazy_greedy([hhmax], preferred=[hhmax])']
    elif search == SYMBA2:
        return PLANNER_ARGS + ['--search', 'sbd()']


def print_stats(stats, complete):
    try:
        sample_size = '\tSample size:   {0:d}'.format(len(stats))
        avg_time = '\tAverage time:  {0:.2f}'.format(stats.mean()) + DEFAULT_TIME_UNIT
        max_time = '\tMaximum time:  {0:.2f}'.format(stats.maximum()) + DEFAULT_TIME_UNIT
        min_time = '\tMinimum time:  {0:.2f}'.format(stats.minimum()) + DEFAULT_TIME_UNIT
        std_dev = '\tStd deviation: {0:.2f}'.format(stats.stddev())

    except:
        print(SRC_DIR)
        error_msg = 'Statistics could not be computed due to unexpected error.'
        print(error_msg)
        with open(REPORT_FILE_NAME, 'a') as report:
            report.write(SRC_DIR + '\n')
            report.write('\t' + error_msg + '\n')

    else:
        print(SRC_DIR)

        if not complete:
            print(INCOMPLETE_COMPUTATION_MSG)

        print(sample_size)
        print(avg_time)
        print(max_time)
        print(min_time)
        print(std_dev)

        # append to report file
        with open(REPORT_FILE_NAME, 'a') as report:
            report.write(SRC_DIR + '\n')

            if not complete:
                report.write(INCOMPLETE_COMPUTATION_MSG + '\n')

            report.write(sample_size + '\n')
            report.write(avg_time + '\n')
            report.write(max_time + '\n')
            report.write(min_time + '\n')
            report.write(std_dev + '\n')


if __name__ == '__main__':
    # parse arguments
    arg_parser = init_arg_parser()
    args = arg_parser.parse_args()
    SRC_DIR = args.src_dir
    SEARCH = args.search

    # set planner inputs
    SRC_DIR = format_src_dir(SRC_DIR)
    PLANNER_ARGS = get_planner_args_list(SEARCH)

    # prepare output dir
    current_time = str(current_millisecs_time())
    DEST_DIR = 'alignments_' + current_time
    mkdir(DEST_DIR)
    DEST_FILE = path.join(DEST_DIR, 'alignment_')

    # prepare log files
    SOLUTIONS_NOT_FOUND_FILE_NAME = 'sol_not_found_' + current_time + '.txt'

    # init stats
    time_stats = Statistics()

    # iterate over PDDL domain/problem pairs
    complete_computation = True
    try:
        for domain in glob.glob(path.join(SRC_DIR, DOMAIN_FILE_PATTERN)):
            # extract trace number
            basename = path.basename(domain)
            trace_number = re.search(INTEGER_NUMBER_REGEX, basename).group(0)

            print('processing trace #{0:s}...'.format(trace_number))

            # get correct file names for problem and output
            problem = path.join(SRC_DIR, PROBLEM_FILE_PATTERN + trace_number + PLANNER_INPUT_EXT)
            alignment = DEST_FILE + trace_number

            # exec planner (redirecting output)
            PLANNER_ARGS[ALIGNMENT_FILE_POS] = alignment
            PLANNER_ARGS[DOMAIN_FILE_POS] = domain
            PLANNER_ARGS[PROBLEM_FILE_POS] = problem
            process = subprocess.Popen(PLANNER_ARGS, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

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

            # update stats
            if trace_number != 0:
                time_stats.push(trace_alignment_time_ms)

            # append stats to output file
            with open(alignment, 'a') as alignment_file:
                alignment_file.write(OUTPUT_SEARCH_TIME_ENTRY_PREFIX + str(trace_alignment_time_ms) + 'ms\n')

            # parse alignment cost from file to check whether solution was found
            with open(alignment, 'r') as alignment_file:
                file_text = alignment_file.read()
                if not re.search(OUTPUT_PLAN_COST_ENTRY_PREFIX, file_text):
                    print('Solution not found for trace #{0:s}.'.format(trace_number))
                    with open(SOLUTIONS_NOT_FOUND_FILE_NAME, 'a') as sol_not_found_file:
                        sol_not_found_file.write(trace_number + '\n')

    except:
        complete_computation = False

    print_stats(time_stats, complete_computation)

    if path.isfile(SOLUTIONS_NOT_FOUND_FILE_NAME):
        error_msg_ = 'Some plans could not be found, check log file.'
        print(error_msg_)
        with open(REPORT_FILE_NAME, 'a') as report_:
            report_.write('\t' + error_msg_ + '\n')

    try:
        # cleanup
        remove('output.sas')
        remove('output')
    except OSError:
        pass
