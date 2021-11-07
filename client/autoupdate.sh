#!/bin/bash

while true; do find src | entr -d ./release.sh ;  done
