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
# This example is for creating a table for the GLN over time.
#git checkout master
#git checkout `git rev-list -n 1 --before="2016-04-01 00:00" master`
#ant jar-lockss
#./scripts/tdb/tdbout -t auid,status tdb/prod/*.tdb | sort -u > ../tmp/file1.txt
#git checkout master
#git checkout `git rev-list -n 1 --before="2017-04-01 00:00" master`
#ant jar-lockss
#./scripts/tdb/tdbout -t auid,status tdb/prod/*.tdb | sort -u > ../tmp/file2.txt
#git checkout master
#git pull origin master
#ant jar-lockss
#./scripts/tdb/report_buckets.pl ../tmp/file1.txt ../tmp/file2.txt
#exit 0
#
# This example is for creating a table for clockss at one specific time.
#git checkout master
#git checkout `git rev-list -n 1 --before="2017-05-01 00:00" master`
#ant jar-lockss
#./scripts/tdb/tdbout -t auid,status tdb/clockssingest/*.tdb | sort -u > ../tmp/file1.txt
#./scripts/tdb/tdbout -t auid,status2 tdb/clockssingest/*.tdb | sort -u > ../tmp/file2.txt
#git checkout master
#git pull origin master
#ant jar-lockss
#./scripts/tdb/report_buckets.pl ../tmp/file1.txt ../tmp/file2.txt
#exit 0

#CLOCKSS: To create a report, comparing two points in time.
#Copy this file and report_buckets_publplug.pl to SageEdits.
git checkout master
git checkout `git rev-list -n 1 --before="2021-05-01 00:00" master`
ant jar-lockss
./scripts/tdb/tdbout -t auid,status tdb/clockssingest/{,_retired/}*.tdb | sort -u > ../SageEdits/file_05.txt
git checkout master
git checkout `git rev-list -n 1 --before="2021-06-01 00:00" master`
ant jar-lockss
./scripts/tdb/tdbout -t auid,status,publisher,plugin tdb/clockssingest/{,_retired/}*.tdb | sort -u > ../SageEdits/file_06.txt
git checkout master
git pull
ant jar-lockss
./../SageEdits/report_buckets_publplug.pl ../SageEdits/file_05.txt ../SageEdits/file_06.txt > ../SageEdits/buckets__05.tsv

exit 0
