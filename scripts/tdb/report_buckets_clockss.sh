#! /bin/bash
#
# Read in two reports of AUids and status. Generate a table
# of category crossings.
#
# To create a report, comparing clockss status1 and status2
# ./scripts/tdb/tdbout -t auid,status tdb/clockssingest/*.tdb | sort -u > ../SageEdits/file1.txt
# ./scripts/tdb/tdbout -t auid,status2 tdb/clockssingest/*.tdb | sort -u > ../SageEdits/file2.txt
# ./scripts/tdb/report_buckets.pl ../SageEdits/file1.txt ../SageEdits/file2.txt > ../SageEdits/buckets_today.tsv

year=$1
./scripts/tdb/tdbout -t auid,status -Q 'year ~ "'$year'"' tdb/clockssingest/*.tdb | sort -u > ~/tmp/rb1.txt
./scripts/tdb/tdbout -t auid,status2 -Q 'year ~ "'$year'"' tdb/clockssingest/*.tdb | sort -u > ~/tmp/rb2.txt
./scripts/tdb/report_buckets.pl ~/tmp/rb1.txt ~/tmp/rb2.txt | grep -v "^notPresent" | grep -v "^wanted" | grep -v "^released" | grep -v "^doesNotExist" | grep -v "^other" | grep -v "^deleted" | awk '{$2=$3=$6=$7=$9=$11=$13=$15=$18=$20=$21=$22=""; print $0}'
exit 0
