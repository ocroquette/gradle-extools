#!/bin/bash

DUMPFILE="${1?Please provide as parameter the file to dump to}"

env > "$DUMPFILE"
