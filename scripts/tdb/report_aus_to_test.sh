###!/bin/bash
###
# Read in tester number and output auids to test.
#
tester=$1
current_year=2015
published=2013
contract=2008 #includes all possible contract years

until [ $published -gt $current_year ] ; do
    echo   The published year is $published

    contract=2008 #includes all possible contract years
    until [ $contract -gt $published ]; do
        echo The contract is $contract
#set -x
            ./scripts/tdb/tdbout -a -M -Q 'year ~ "'$published'$" and publisher:info[tester] is "'$tester'" and publisher:info[contract] is "'$contract'"' tdb/clockssingest/*.tdb
#set +x
        let contract=contract+1
    done
    
    let published=published+1
done


exit 0

