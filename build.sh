#!/usr/bin/env bash

set -e
#set -x

GRAAL=~/soft/graalvm-ce-19.2.0.1
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

if [[ ! -f $GRAAL/bin/native-image ]]
then
    echo "Installing native-image..."
    bash -c "cd $GRAAL/bin/ ; ./gu install native-image"
fi

$GRAAL/bin/native-image -cp "$CP" com.cmlteam.serv.Serv
ls -lh ./com.cmlteam.serv.serv

echo
echo Upx
echo

rm -f ./serv
$UPX/upx ./com.cmlteam.serv.serv -oserv
ls -lh ./serv

