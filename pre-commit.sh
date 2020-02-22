#!/bin/sh

rm docs/*.html
lein codox
lein ns-graph
mv graph.png docs