#!/bin/bash


usage() {
  echo "usage: $0 <test type> <email address>"
  echo "  Runs the <test type> test once, then emails <email address>"
  exit 2
}

[ "$#" -ne "2" ] && usage

host=`hostname`


test_type=$1
address=$2

log_file=log.txt

trap "exit 1" 2 

python testsuite.py $test_type > $log_file 2>&1 
mail -s "Test finished on $host" $address < $log_file
echo "run finished, removing log files"
rm  $log_file
./clean.sh

echo "Done"
