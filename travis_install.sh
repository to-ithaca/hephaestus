#!/bin/bash

GIT_URL=$0
CLONE_DIR=$1
VERSION=$2

CURRENT_DIR=$(pwd)

mkdir -p $HOME/.external
cd $HOME/.external

if [ ! -d "$CLONE_DIR" ]; then
 git clone $GIT_URL $CLONE_DIR
 cd $CLONE_DIR
 git checkout -qf $VERSION
else
 cd $CLONE_DIR
fi

cmake -DBUILD_SHARED_LIBS=ON && make && sudo make install

cd $CURRENT_DIR
