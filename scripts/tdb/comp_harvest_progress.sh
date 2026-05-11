#! /bin/bash
#
#Compare source/ftp AUids against the buckets that exist on ingest1
tpath="/home/$LOGNAME/tmp"
logfile="bucketlog"
lockss="/home/$LOGNAME/lockss-daemon"
tdbout="./scripts/tdb/tdbout"
YEAR=$(date +%Y)
LAST_YEAR=$((YEAR - 1))

echo "******" > $tpath/$logfile
now=$(date)
echo "Generated: $now" > $tpath/$logfile

echo
echo "***Processing CLOCKSS $YEAR content:"
$tdbout -Q "publisher:info[contract] is \"$YEAR\"" -t publisher,plugin,year,publisher:info[tester],publisher:info[contract],status,publisher:info[platform],file tdb/clockssingest/*.tdb | python3 ./scripts/tdb/statuses_clockss_harvest.py

#echo
#echo "***Processing CLOCKSS $YEAR and $LAST_YEAR content:"
#tdbout -Q "publisher:info[contract] is \"$YEAR\" or publisher:info[contract] is \"$LAST_YEAR\"" -t publisher,plugin,year,publisher:info[tester],publisher:info[contract],status,publisher:info[platform],file tdb/clockssingest/*.tdb | python3 ./scripts/tdb/statuses_clockss_harvest.py

echo
echo "***Need CLOCKSS Harvest Plugins:"
$tdbout -c title,publisher:info[contract],publisher:info[platform] -Q 'plugin ~ "needs"' tdb/clockssingest/*.tdb

# Find plugins listed in tdb files, that don't exist
echo "----------------------"
# These items should be run from lockss-daemon/plugins/src
( cd plugins/src && grep -rl --include "*.xml" "plugin_identifier" * | sed 's/\(.*\).xml/\1/' | sort -u ) > $tpath/ab.txt
scripts/tdb/tdbout -t plugin tdb/*/ | sort -u | sed 's/\./\//g' > $tpath/ac.txt
#plugins that have no AUs.
#diff $tpath/ab.txt $tpath/ac.txt | grep "^< "     
diff $tpath/ab.txt $tpath/ac.txt | grep "^> " | grep Clockss

# Find plugin names without "Clockss" in the clockss title database
echo "----------------------"
./scripts/tdb/tdbout -t publisher,title,plugin -Q 'plugin !~ "Clockss" and plugin !~ "needs"' tdb/clockssingest/{,*/}*.tdb | sort -u
echo " "

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
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. tdb files not assigned to content testing"
scripts/tdb/tdbout -t publisher:info[tester],publisher -Q 'publisher:info[tester] is not "8" and publisher:info[tester] is not "5"' tdb/clockssingest/*.tdb | sort | uniq -c

echo
echo "Generated: $now"
#echo "For current copy see: http://clockss-ingest.lockss.org/reports/mpetrich/source_bucket_report.txt"
exit 0
