#!/bin/bash


usage() {
  echo "usage: $0 <test type> <email address>"
  echo "  Runs the <test type> test until it fails, then emails <email address>"
  exit 2
}

[ "$#" -ne "2" ] && usage

host=`hostname`


test_type=$1
address=$2

log_file=log.txt

trap "exit 1" 2 

while [ true ]
do
    python testsuite.py $test_type > $log_file 2>&1 
    if [ $? -ne 0 ]
    then
	mail $address -s "Test failed on $host" < $log_file
	exit 1
    fi
  echo "run finished, removing log files"
  rm  $log_file
  ./clean.sh
done

echo "Done"
