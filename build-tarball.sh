#!/bin/sh
[ -d toopher-openam ] && echo "build directory already exists! Please remove before continuing" && exit

./build.sh

mkdir toopher-openam
mkdir -p toopher-openam/openam
cp -r schema toopher-openam
cp README.md toopher-openam
cp -r lib toopher-openam/openam
cp toopher-openam.jar toopher-openam/openam/lib
mkdir -p toopher-openam/openam/config
cp ToopherSecondFactor/* toopher-openam/openam/config

tar czvf toopher-openam.tgz toopher-openam
