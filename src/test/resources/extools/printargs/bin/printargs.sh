#!/usr/bin/env bash

n=0
while test $# -gt 0
do
    n=$((n+1))
    echo "ARG$n=$1"
    shift
done
