#!/bin/bash

rm -rf ../server/src/main/resources/assets/*
cp -r __sapper__/export/* ../server/src/main/resources/assets
cp -r __sapper__/export/* ../server/target/classes/assets
echo "Deployed."
