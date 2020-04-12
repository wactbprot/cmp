#!/bin/sh

echo "\nrm -r docs/*\n" 
rm -r docs/*
echo "============================" 
echo "========= coverage ========="
echo "============================" 
lein cloverage
cp -r target/coverage docs/coverage
echo "============================" 
echo "=========  codox   ========="
echo "============================" 
lein codox
cp -r target/default/doc docs/api
echo "============================" 
echo "========= ns-graph ========="
echo "============================" 
lein ns-graph
mv graph.png docs/graph.png 