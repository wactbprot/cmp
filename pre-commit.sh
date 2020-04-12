#!/bin/sh

echo "\n========= coverage =========\n" 
rm docs/coverage/cmp/worker/*.html
rm docs/coverage/cmp/*.html
rm docs/coverage/*.html
lein cloverage

echo "\n=========   codox   =========\n" 
rm docs/*.html
lein codox

echo "\n=========  ns-graph  =========\n" 
lein ns-graph
mv graph.png docs/graph.png 