#!/usr/bin/env python

from subprocess import Popen, PIPE
import shlex
from os import listdir, system
from os.path import isfile, join
import time
import plotly.graph_objs as go
import argparse

DEFAULT_MIN_THREADS = 1
DEFAULT_MAX_THREADS = 4
DEFAULT_DATA_DIR = "data"
DEFAULT_OUTPUT_FILE = "benchmark/fig1.png"


def get_data(d_dir):
    f_format = ".bz2"
    return [join(d_dir, f) for f in listdir(d_dir) if isfile(join(d_dir, f)) and f.endswith(f_format)]


def measure(data, threads, extra_memory, xml_parser):
    start = time.time()
    data_str = ",".join(data)
    system("export GRADLE_OPTS=\"{}\"".format("-Xms12g -Xmx12g" if extra_memory else ""))
    args = './gradlew run --args="--inputs {} --threads {} --parser {}"'\
        .format(data_str, threads, xml_parser)
    print(args)
    process = Popen(shlex.split(args), stdout=PIPE, stderr=PIPE)
    _, stderr = process.communicate()
    process.wait()
    end = time.time()
    print(stderr)
    return end - start


def build_plot(x, y, output):
    fig = go.Figure()
    fig.add_trace(go.Scatter(x=x, y=y,
                             mode='lines'))
    fig.update_layout(
        xaxis_title="threads",
        yaxis_title="time"
    )
    fig.write_image(output)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--min", help="min threads number", default=DEFAULT_MIN_THREADS, type=int)
    parser.add_argument("--max", help="max threads number", default=DEFAULT_MAX_THREADS, type=int)
    parser.add_argument("--data", help="data directory", default=DEFAULT_DATA_DIR, type=str)
    parser.add_argument("--out", help="output file", default=DEFAULT_OUTPUT_FILE, type=str)
    parser.add_argument("--mem", help="allocate extra memory", action="store_true")
    parser.add_argument("--parser", help="choose parser", type=str, default="SAX")
    args = parser.parse_args()

    min_threads = args.min
    max_threads = args.max
    d_dir = args.data
    output = args.out
    extra_memory = args.mem
    xml_parser = args.parser

    data = get_data(d_dir)
    x = []
    y = []
    for threads in range(min_threads, max_threads + 1):
        res = measure(data, threads, extra_memory, xml_parser)
        print("Ran {} threads in {} s".format(threads, res))
        x.append(threads)
        y.append(res)

    build_plot(x, y, output)


main()
