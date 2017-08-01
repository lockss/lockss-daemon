#! /bin/bash
#
# Read in two reports of AUids and status. Generate a table
# of category crossings.
#
# To create a report, comparing clockss status1 and status2, right now.
# ./scripts/tdb/tdbout -t auid,status tdb/clockssingest/*.tdb | sort -u > ../SageEdits/file1.txt
# ./scripts/tdb/tdbout -t auid,status2 tdb/clockssingest/*.tdb | sort -u > ../SageEdits/file2.txt
# ./scripts/tdb/report_buckets.pl ../SageEdits/file1.txt ../SageEdits/file2.txt > ../SageEdits/buckets_today.tsv
#

# This active script is for creating a table for clockss, for all harvest content, from Oct 1 of the previous year to the present.

#option to get info by year.
#req_year=$1
#cur_year=`date +%Y`
#if $req_year > 1900 and $req_year < $cur_year then use the year, otherwise use all content.
#if [ $req_year > 1900 && $req_year < $cur_year ]
#then
#    req = "-t auid,status -Q 'year ~ "'$req_year'"' tdb/clockssingest/*.tdb"
#else
#    req = "-t auid,status tdb/clockssingest/*.tdb"
#fi

cd ~/lockss-daemon
git checkout master
git checkout `git rev-list -n 1 --before="2017-07-03 00:00" master`
ant jar-lockss
./scripts/tdb/tdbout -t auid,status tdb/clockssingest/*.tdb | grep -v SourcePlugin | grep -v warcfiles | sort -u > ~/tmp/file1.txt
git checkout master
git pull origin master
ant jar-lockss
./scripts/tdb/tdbout -t auid,status tdb/clockssingest/*.tdb | grep -v SourcePlugin | grep -v warcfiles | sort -u > ~/tmp/file2.txt
./scripts/tdb/report_buckets.pl ~/tmp/file1.txt ~/tmp/file2.txt
exit 0
