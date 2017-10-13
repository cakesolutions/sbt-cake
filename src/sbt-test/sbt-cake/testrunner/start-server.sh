#!/usr/bin/env bash

pushd target/universal/stage
./bin/testrunner &
echo $! > server.pid
popd
