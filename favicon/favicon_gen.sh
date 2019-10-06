#!/usr/bin/env bash

set -e
#set -x

GRAAL=~/soft/graalvm-ce-19.2.0.1

NODE=$GRAAL/bin/node

mydir=$(dirname "$0")

cd $mydir

$NODE favicon_gen.js
