#!/usr/bin/env bash

set -e
#set -x

GRAAL=~/soft/graalvm-ce-19.2.0
UPX=~/soft/upx-3.95-amd64_linux

mvn clean compile
CP=$(mvn -q exec:exec -Dexec.executable=echo -Dexec.args="%classpath")

if [[ ! -d ./build ]]
then
    mkdir ./build
fi

cd ./build/

echo
echo Graal
echo

$GRAAL/bin/native-image -cp "$CP" com.cmlteam.serv.Serv
ls -lh ./com.cmlteam.serv.serv

echo
echo Upx
echo

rm -f ./serv
$UPX/upx ./com.cmlteam.serv.serv -oserv
ls -lh ./serv

