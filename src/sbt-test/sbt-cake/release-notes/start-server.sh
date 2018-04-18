#!/usr/bin/env bash

pushd target/universal/stage
./bin/release-notes &
echo $! > server.pid
popd
