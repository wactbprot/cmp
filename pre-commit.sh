#!/bin/sh

lein codox
lein ns-graph
mv graph.png docs