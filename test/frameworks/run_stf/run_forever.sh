#!/bin/bash

TEST_TYPE=$1
HOST=`hostname`
run=0
ADDRESS=troberts@stanford.edu

LOG_FILE=log.txt

trap "exit 1" 2 

#while [ `python testsuite.py $TEST_TYPE` ]
while [ true ]
do
    python testsuite.py $TEST_TYPE > $LOG_FILE 2>&1 
    if [ $? -ne 0 ]
    then
	mail $ADDRESS -s "Test failed on $HOST" < $LOG_FILE
	exit 1
    fi
  echo "run finished, removing log files"
  rm  $LOG_FILE
  ./clean.sh
done

echo "Done"
