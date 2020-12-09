#!/bin/sh

echo "========= clean up: ========\n"
rm -r target/default/doc/*
echo "rm -r target/default/doc/*\n"
rm -r target/coverage/*
echo "rm -r target/coverage/*\n"
rm -r docs/api/*
echo "rm -r docs/api/*\n"
rm -r docs/coverage/*
echo "rm -r docs/coverage/*\n"

echo "======== cp ref mpd=========\n\n"
cp resources/mpd-ref.edn docs/

echo "========= coverage =========\n\n"
lein cloverage
cp -r target/coverage/* docs/coverage

echo "=========  codox   =========\n\n"
lein codox
cp -r target/default/doc/* docs/api
