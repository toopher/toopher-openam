#!/bin/sh
[ -d toopher-openam ] && echo "build directory already exists! Please remove before continuing" && exit

./build.sh

mkdir toopher-openam
mkdir -p toopher-openam/openam

#cp -r schema toopher-openam

cp README.md toopher-openam
python -m markdown README.md > toopher-openam/README.html
cp -r lib toopher-openam/openam
cp toopher-openam.jar toopher-openam/openam/lib
rm toopher-openam/openam/lib/amserver.jar
rm toopher-openam/openam/lib/opensso-sharedlib.jar

mkdir -p toopher-openam/openam/config
cp ToopherSecondFactor/* toopher-openam/openam/config

mkdir -p toopher-openam/openam/tools
cp tools/* toopher-openam/openam/tools

tar czvf toopher-openam.tgz toopher-openam
