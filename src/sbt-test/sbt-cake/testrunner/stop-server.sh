#!/usr/bin/env bash

kill -9 $(ps aux | grep "MockServer" | grep -v 'grep' | awk '{print $2}')
