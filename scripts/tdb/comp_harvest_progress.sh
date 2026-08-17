#! /bin/bash
#
#Compare source/ftp AUids against the buckets that exist on ingest1
tpath="/home/$LOGNAME/tmp"
logfile="bucketlog"
lockss="/home/$LOGNAME/lockss-daemon"
tdbout="./scripts/tdb/tdbout"
YEAR=$(date +%Y)
LAST_YEAR=$((YEAR - 1))

#Document time start for log
echo "******" > $tpath/$logfile
now=$(date)
echo "Generated: $now" > $tpath/$logfile

#Report on processing the current year's content
echo
echo "***Processing CLOCKSS $YEAR content:"
$tdbout -Q "publisher:info[contract] is \"$YEAR\"" -t publisher,plugin,year,publisher:info[tester],publisher:info[contract],status,publisher:info[platform],file tdb/clockssingest/*.tdb | python3 ./scripts/tdb/statuses_clockss_harvest.py

#Update above to this in 2027
#echo
#echo "***Processing CLOCKSS $YEAR and $LAST_YEAR content:"
#tdbout -Q "publisher:info[contract] is \"$YEAR\" or publisher:info[contract] is \"$LAST_YEAR\"" -t publisher,plugin,year,publisher:info[tester],publisher:info[contract],status,publisher:info[platform],file tdb/clockssingest/*.tdb | python3 ./scripts/tdb/statuses_clockss_harvest.py

#List titles for harvest plugins in development pending
echo
echo "***CLOCKSS harvest plugins in development (pending)"
$tdbout -t publisher:info[tester],publisher,publisher:info[contract],publisher:info[platform] -Q 'plugin ~ "needs"' tdb/clockssingest/*.tdb | sort | uniq -c

#List publishers for harvest plugins in development existing
echo
echo "***CLOCKSS harvest plugins in development (existing)"
$tdbout -t publisher:info[tester],publisher,publisher:info[contract],publisher:info[platform] -Q 'plugin !~ "needs" and publisher:info[tester] is not "8" and publisher:info[tester] is not "5"' tdb/clockssingest/*.tdb | sort | uniq -c


# Find plugins listed in tdb files, that don't exist. Usually empty
echo
echo "---------------------"
# These items should be run from lockss-daemon/plugins/src
( cd plugins/src && grep -rl --include "*.xml" "plugin_identifier" * | sed 's/\(.*\).xml/\1/' | sort -u ) > $tpath/ab.txt
$tdbout -t plugin tdb/*/ | sort -u | sed 's/\./\//g' > $tpath/ac.txt
#plugins that exist that have no AUs
#comm -23 $tpath/ab.txt $tpath/ac.txt
#plugins in the tdb files, that don't exist
comm -13 $tpath/ab.txt $tpath/ac.txt | grep Clockss

# Find plugin names without "Clockss" in the clockss title database. Usually empty
echo
echo "---------------------"
$tdbout -t publisher,title,plugin -Q 'plugin !~ "Clockss" and plugin !~ "needs"' tdb/clockssingest/{,*/}*.tdb | sort -u

#
# Find tdb files possibly ready to be moved to retirement or needing first processing.
#echo "---------------------"
#echo "---------------------"
#echo "CLOCKSS. tdb files ready to retire?"
#grep -L -e expected -e exists -e crawling -e manifest -e testing -e ready tdb/clockssingest/*.tdb
#echo "---------------------"
#echo "---------------------"
#echo "CLOCKSS. tdb files need first processing."
#grep -L -e ready -e crawling -e frozen -e deepCrawl -e finished tdb/clockssingest/*.tdb | xargs grep -l "manifest"

echo
echo "***Generated: $now"
#echo "For current copy see: http://clockss-ingest.lockss.org/reports/mpetrich/source_bucket_report.txt"
exit 0
