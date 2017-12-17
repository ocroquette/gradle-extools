#!/usr/bin/env bash

FILE1="${1?Please provide 2 files as parameters}"
FILE2="${2?Please provide 2 files as parameters}"

loops=0
while [ ! -e "$FILE1" ]
do
    loops=$((loops+1))
    if (( $((loops)) > 10 )); then
        echo Timeout
        exit 1
    fi
    echo "Waiting for $FILE1 to appear..."
    sleep 1
done

echo "Touching $FILE2"
touch "$FILE2"
