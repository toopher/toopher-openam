#!/bin/sh
rm -fr toopher-openam

./build.sh

mkdir toopher-openam
mkdir -p toopher-openam/openam/js
mkdir -p toopher-openam/openam/config/auth/default
mkdir -p toopher-openam/openam/WEB-INF/lib
mkdir -p toopher-openam/openam/WEB-INF/classes



cp README.md toopher-openam
python2.7 -m markdown README.md > toopher-openam/README.html
cp lib/* toopher-openam/openam/WEB-INF/lib/
cp toopher-openam.jar toopher-openam/openam/WEB-INF/lib/
rm toopher-openam/openam/WEB-INF/lib/amserver.jar
rm toopher-openam/openam/WEB-INF/lib/opensso-sharedlib.jar

cp ToopherSecondFactor/amAuthToopherSecondFactor.* toopher-openam/openam/WEB-INF/classes/
cp ToopherSecondFactor/ToopherSecondFactor.xml toopher-openam/openam/config/auth/default/
cp ToopherSecondFactor/toopher-openam.js toopher-openam/openam/js/

mkdir -p toopher-openam/openam/tools
cp tools/* toopher-openam/openam/tools

tar czvf toopher-openam.tgz toopher-openam

