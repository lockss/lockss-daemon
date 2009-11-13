#!/bin/sh

if [ $# -eq 0 -o $# -gt 2 ]
then
    echo "usage: $0 <test name/suite> [<email address>]"
    echo "Runs the specified test/suite until failure, then emails <email address> if supplied"
    exit 2
fi

if [ $# -eq 2 ]
then
    which mail > /dev/null
    if [ $? -ne 0 ]
    then
        echo Mail agent not found
        exit 2
    fi
fi

log=log.txt
while [ true ]
do
    ./testsuite.py $1 > $log 2>&1
    if [ $? -ne 0 ]
    then
        if [ $# -eq 2 ]
        then
            mail $2 -s "STF $1 failed on $host" < $log
        else
            cat $log
        fi
        exit 1
    fi
    echo "`date`	Run finished, cleaning up"
    rm $log
    ./clean.sh
done
