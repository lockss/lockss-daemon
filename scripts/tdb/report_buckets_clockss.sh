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

# This active script is for creating a table for clockss, for a specific publication year, at the current time.
year=$1
./scripts/tdb/tdbout -t auid,status -Q 'year ~ "'$year'"' tdb/clockssingest/{,*/}*.tdb | sort -u > ~/tmp/rb1_${year}.txt
#./scripts/tdb/tdbout -t auid,status -Q 'year ~ "'$year'"' tdb/clockssingest/*.tdb | grep -v ClockssTaylorAndFrancisPlugin | sort -u > ~/tmp/rb1.txt
./scripts/tdb/tdbout -t auid,status2 -Q 'year ~ "'$year'"' tdb/clockssingest/{,*/}*.tdb | sort -u > ~/tmp/rb2_${year}.txt
#./scripts/tdb/tdbout -t auid,status2 -Q 'year ~ "'$year'"' tdb/clockssingest/*.tdb | grep -v ClockssTaylorAndFrancisPlugin | sort -u > ~/tmp/rb2.txt
./scripts/tdb/report_buckets.pl ~/tmp/rb1_${year}.txt ~/tmp/rb2_${year}.txt | grep -v "^notPresent" | grep -v "^wanted" | grep -v "^released" | grep -v "^doesNotExist" | grep -v "^other" | grep -v "^deleted" | awk '{$2=$3=$6=$7=$9=$11=$13=$15=$18=$20=$21=$22=""; print $0}'
exit 0
