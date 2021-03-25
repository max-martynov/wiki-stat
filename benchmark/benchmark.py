#!/usr/bin/env python

from subprocess import Popen, PIPE
import shlex
from os import listdir
from os.path import isfile, join
import time
import plotly.graph_objs as go

MIN_THREADS = 1
MAX_THREADS = 4


def get_data():
    d_dir = "data"
    f_format = ".bz2"
    return [join(d_dir, f) for f in listdir(d_dir) if isfile(join(d_dir, f)) and f.endswith(f_format)]


def measure(data, threads):
    start = time.time()
    data_str = ",".join(data)
    args = './gradlew run --args="--inputs {} --threads {}"'.format(data_str, threads)
    print(args)
    process = Popen(shlex.split(args), stdout=PIPE, stderr=PIPE)
    _, stderr = process.communicate()
    process.wait()
    end = time.time()
    print(stderr)
    return end - start


def build_plot(x, y):
    fig = go.Figure()
    fig.add_trace(go.Scatter(x=x, y=y,
                             mode='lines'))
    fig.update_layout(
        xaxis_title="threads",
        yaxis_title="time"
    )
    fig.write_image("benchmark/fig1.png")


def main():
    data = get_data()
    x = []
    y = []
    for threads in range(MIN_THREADS, MAX_THREADS + 1):
        res = measure(data, threads)
        print("Ran {} threads in {} s".format(threads, res))
        x.append(threads)
        y.append(res)

    build_plot(x, y)


main()
