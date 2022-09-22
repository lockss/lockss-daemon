#!/bin/bash
#
# Script to provide alerts to problems in the title database
tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath

date
# Find incorrect status
echo "---------------------"
echo "---------------------"
echo "*Status typos gln: "
scripts/tdb/tdbout -t status tdb/prod/ | sort | uniq -c | grep -vw manifest | grep -vw released | grep -vw expected | grep -vw exists | grep -vw testing | grep -vw wanted | grep -vw ready | grep -vw down | grep -vw superseded | grep -vw doNotProcess | grep -vw notReady | grep -vw doesNotExist
echo "*Status typos usdocs: "
scripts/tdb/tdbout -t status tdb/usdocspln/ | sort | uniq -c | grep -vw manifest | grep -vw released | grep -vw expected | grep -vw exists | grep -vw testing | grep -vw wanted | grep -vw down | grep -vw superseded | grep -vw doNotProcess | grep -vw notReady | grep -vw doesNotExist
echo "*Status variations clockssingest: "
scripts/tdb/tdbout -c status,status2 tdb/clockssingest/ | sort | uniq -c | sort -n | grep -vw "manifest,exists" | grep -vw "crawling,exists" | grep -vw "finished,crawling" | grep -vw "exists,exists" | grep -vw "down,crawling" | grep -vw "doNotProcess,doNotProcess" | grep -vw "expected,exists" | grep -vw "testing,exists" | grep -vw "notReady,exists" | grep -vw "ingNotReady,exists" | grep -vw "zapped,finished" | grep -vw "doesNotExist,doesNotExist"
#
# Find plugins listed in tdb files, that don't exist
echo "---------------------"
echo "---------------------"
# Script is run from lockss-daemon/scripts/tdb
# These items should be run from lockss-daemon/plugins/src
echo "*Plugins that don't exist, but are listed in tdb files: "
( cd plugins/src && grep -rl --include "*.xml" "plugin_identifier" * | sed 's/\(.*\).xml/\1/' | sort -u ) > $tpath/ab.txt
scripts/tdb/tdbout -t plugin tdb/*/ | sort -u | sed 's/\./\//g' > $tpath/ac.txt
#plugins that have no AUs.
#diff $tpath/ab.txt $tpath/ac.txt | grep "^< "     
#plugins that don't exist, but are listed in tdb files
diff $tpath/ab.txt $tpath/ac.txt | grep "^> "
echo " "
#
# Find all reingest
echo "---------------------"
echo "---------------------"
echo "Clockss AUs with status2=manifest. Ready to release to production machines."
echo "reingest: 1:8082, 2:8085, 3:8083, 4:8082, 5:8082"
scripts/tdb/tdbout -F -t "au:hidden[proxy]" -Q 'status2 is "manifest"' tdb/clockssingest/ | sort | uniq -c
echo "No reingest set."
scripts/tdb/tdbout -F -t "publisher,title" -Q 'status2 is "manifest" and au:hidden[proxy] is ""' tdb/clockssingest/ | sort | uniq -c
echo " "
#
# Find duplicate auids in the gln title database
echo "---------------------"
echo "---------------------"
scripts/tdb/tdbout -AXEa tdb/prod/ | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "GLN. All AUids = $allAUs"
echo "GLN. AUids without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate name/plugin pairs in the gln title database
echo "---------------------"
scripts/tdb/tdbout -AXE -c plugin,name tdb/prod/ | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "GLN. All plugin/names = $allAUs"
echo "GLN. Plugin/names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate released names in the gln title database
echo "---------------------"
echo "GLN. Duplicate Released Names. Commented out."
#scripts/tdb/tdbout -P -c name tdb/prod/ | sort > $tpath/allAUs
#uniq $tpath/allAUs > $tpath/dedupedAUs
#allAUs=`cat $tpath/allAUs | wc -l`
#uniqAUs=`cat $tpath/dedupedAUs | wc -l`
#echo "GLN. All Released names = $allAUs"
#echo "GLN. Released names without duplicates = $uniqAUs"
#diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find number of AUs ready for release in the prod title database
echo "----------------------"
./scripts/tdb/tdbout -Y -t status tdb/prod/ | sort | uniq -c
echo " "
#
# Find plugin names with "Clockss" in the prod title database
echo "----------------------"
./scripts/tdb/tdbout -t publisher,name,plugin -Q 'plugin ~ "Clockss" and plugin !~ "needs"' tdb/prod/{,*/}*.tdb
echo " "
#
# Find duplicate auids in the clockss title database
echo "---------------------"
echo "---------------------"
scripts/tdb/tdbout -AXEa tdb/clockssingest/ | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "CLOCKSS. All AUids = $allAUs"
echo "CLOCKSS. AUids without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate name/plugin pairs in the clockss title database
echo "---------------------"
scripts/tdb/tdbout -AXE -c plugin,name tdb/clockssingest/ | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "CLOCKSS. All plugin/names = $allAUs"
echo "CLOCKSS. Plugin/names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate released names in the clockss title database
echo "---------------------"
echo "Clockss. Duplicate Released Names. Commented out."
#scripts/tdb/tdbout -PCZI -c name tdb/clockssingest/ | sort > $tpath/allAUs
#uniq $tpath/allAUs > $tpath/dedupedAUs
#allAUs=`cat $tpath/allAUs | wc -l`
#uniqAUs=`cat $tpath/dedupedAUs | wc -l`
#echo "CLOCKSS. All Released names = $allAUs"
#echo "CLOCKSS. Released names without duplicates = $uniqAUs"
#diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find number of AUs ready for release in the clockssingest title database
echo "----------------------"
./scripts/tdb/tdbout -Y -t status tdb/clockssingest/ | sort | uniq -c
echo " "
#
# Find plugin names with "Clockss" in the prod title database
echo "----------------------"
./scripts/tdb/tdbout -t publisher,name,plugin -Q 'plugin !~ "Clockss" and plugin !~ "needs"' tdb/clockssingest/{,*/}*.tdb
echo " "
# Find duplicate auids in the whole database. Not exists or expected
#echo "---------------------"
#echo "---------------------"
#scripts/tdb/tdbout -Aa tdb/*/ | sort > $tpath/allAUs
#uniq $tpath/allAUs > $tpath/dedupedAUs
#allAUs=`cat $tpath/allAUs | wc -l`
#uniqAUs=`cat $tpath/dedupedAUs | wc -l`
#echo "All AUids = $allAUs"
#echo "AUids without duplicates = $uniqAUs"
#diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#echo " "
#
#
# Find HighWire plugin dupes
#echo "---------------------"
#echo "---------------------"
#echo "GLN. HighWire Dupe AUs across plugins"
#scripts/tdb/tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher tdb/prod/ | perl -pe 's/\t+/ /g' | sort -k 1,2 > $tpath/HW_g_all
#scripts/tdb/tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher tdb/prod/ | perl -pe 's/\t+/ /g' | sort -k 1,2 -u > $tpath/HW_g_dedupe
#cat $tpath/HW_g_all | wc -l
#cat $tpath/HW_g_dedupe | wc -l
#diff $tpath/HW_g_all $tpath/HW_g_dedupe
#diff $tpath/HW_g_all $tpath/HW_g_dedupe | grep "< " | wc -l
#echo "expect 19"
#echo " "
#echo "---------------------"
#echo "---------------------"
#echo "CLOCKSS. HighWire Dupe AUs across plugins"
#scripts/tdb/tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher tdb/clockssingest/ | perl -pe 's/\t+/ /g' | sort -k 1,2 > $tpath/HW_c_all
#scripts/tdb/tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher tdb/clockssingest/ | perl -pe 's/\t+/ /g' | sort -k 1,2 -u > $tpath/HW_c_dedupe
#diff $tpath/HW_c_all $tpath/HW_c_dedupe | grep "< " | sort
#diff $tpath/HW_c_all $tpath/HW_c_dedupe | grep "< " | wc -l
#echo "expect 89"
#echo " "
#
# Find tdb files possibly ready to be moved to retirement.
echo "---------------------"
echo "---------------------"
echo "GLN. tdb files ready to retire?"
grep -L -e exists -e released -e manifest -e testing -e expected tdb/prod/*.tdb
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. tdb files ready to retire?"
grep -L -e exists -e crawling -e manifest -e testing -e expected tdb/clockssingest/*.tdb
# Find issn problems in gln title database
echo "---------------------"
echo "---------------------"
echo "GLN. ISSN issues"
#Use tdb out to generate a list of publisher, title, issn, eissn. Replace all amp with and. Remove all starting The. Ignore sub-titles.
scripts/tdb/tdbout -t publisher,title,issn,eissn tdb/prod/ | sed 's/\t\(.*\) & /\t\1 and /' | sed 's/\tThe /\t/' | sed 's/: .*\(\t.*\t\)/\1/' | sort -u > $tpath/issn
scripts/tdb/scrub_table.pl $tpath/issn
#
# Find issn problems in clockss title database
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. ISSN issues"
scripts/tdb/tdbout -t publisher,title,issn,eissn tdb/clockssingest/ | sed 's/\t\(.*\) & /\t\1 and /' | sed 's/\tThe /\t/' | sort -u > $tpath/issn
scripts/tdb/scrub_table.pl $tpath/issn
#
echo "---------------------"
echo "Missing Slashes"
grep "param\[base_url\]" tdb/*/*.tdb tdb/*/*/*.tdb | grep "http.*://" | grep -v "/\s*$" | grep -v ":\s*#" | grep -v "\/\s*#"
echo "---------------------"
echo "---------------------"


