#!/bin/sh

echo "========= clean up =========\n\n"
rm -r target/default/doc/*
rm -r target/coverage/*
rm -r docs/api/*
rm -r docs/coverage/*

echo "======== cp ref mpd=========\n\n"
cp resources/mpd-ref.edn docs/

echo "========= coverage =========\n\n"
lein cloverage
cp -r target/coverage/* docs/coverage

echo "=========  codox   =========\n\n"
lein codox
cp -r target/default/doc/* docs/api

echo "========= ns-graph =========\n\n"
lein ns-graph
mv graph.png docs/graph.png 
