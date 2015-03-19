#!/bin/bash
#
# Script to provide alerts to problems in the title database
tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath

# Find incorrect status
echo "---------------------"
echo "---------------------"
echo "*Status typos gln: "
scripts/tdb/tdbout -t status tdb/prod/*.tdb | sort | uniq -c | grep -vw manifest | grep -vw released | grep -vw expected | grep -vw exists | grep -vw testing | grep -vw wanted | grep -vw ready | grep -vw down | grep -vw superseded | grep -vw doNotProcess | grep -vw notReady | grep -vw doesNotExist
echo "*Status variations clockssingest: "
scripts/tdb/tdbout -c status,status2 tdb/clockssingest/*.tdb | sort | uniq -c | sort -n | grep -vw "manifest,exists" | grep -vw "crawling,exists" | grep -vw "finished,crawling" | grep -vw "exists,exists" | grep -vw "down,crawling" | grep -vw "doNotProcess,doNotProcess" | grep -vw "expected,exists" | grep -vw "testing,exists" | grep -vw "notReady,exists" | grep -vw "ingNotReady,exists" | grep -vw "zapped,finished" | grep -vw "doesNotExist,doesNotExist"
echo "*Status typos ibictpln: "
scripts/tdb/tdbout -t status tdb/ibictpln/*.tdb | grep -vx manifest | grep -vx released | grep -vx expected | grep -vx exists | grep -vx testing | grep -vx wanted | grep -vx ready | grep -vx down | grep -vx superseded | grep -vx doNotProcess | grep -vx notReady | grep -vx doesNotExist
#
# Find plugins listed in tdb files, that don't exist
echo "---------------------"
echo "---------------------"
# Script is run from lockss-daemon/scripts/tdb
# These items should be run from lockss-daemon/plugins/src
echo "*Plugins that don't exist, but are listed in tdb files: "
( cd plugins/src && grep -rl --include "*.xml" "plugin_identifier" * | sed 's/\(.*\).xml/\1/' | sort -u ) > $tpath/ab.txt
scripts/tdb/tdbout -t plugin tdb/*/*.tdb | sort -u | sed 's/\./\//g' > $tpath/ac.txt
#plugins that have no AUs.
#diff $tpath/ab.txt $tpath/ac.txt | grep "^< "     
#plugins that don't exist, but are listed in tdb files
diff $tpath/ab.txt $tpath/ac.txt | grep "^> "
echo " "
#
# Find duplicate auids in the gln title database
echo "---------------------"
echo "---------------------"
scripts/tdb/tdbout -AXEa tdb/prod/*.tdb | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "GLN. All AUids = $allAUs"
echo "GLN. AUids without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate name/plugin pairs in the gln title database
echo "---------------------"
scripts/tdb/tdbout -AXE -c plugin,name tdb/prod/*.tdb | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "GLN. All plugin/names = $allAUs"
echo "GLN. Plugin/names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate released names in the gln title database
echo "---------------------"
scripts/tdb/tdbout -P -c name tdb/prod/*.tdb | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "GLN. All Released names = $allAUs"
echo "GLN. Released names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find number of AUs ready for release in the prod title database
echo "----------------------"
./scripts/tdb/tdbout -Y -t status tdb/prod/*.tdb | sort | uniq -c
echo " "
#
# Find duplicate auids in the clockss title database
echo "---------------------"
echo "---------------------"
scripts/tdb/tdbout -AXEa tdb/clockssingest/*.tdb | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "CLOCKSS. All AUids = $allAUs"
echo "CLOCKSS. AUids without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate name/plugin pairs in the clockss title database
echo "---------------------"
scripts/tdb/tdbout -AXE -c plugin,name tdb/clockssingest/*.tdb | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "CLOCKSS. All plugin/names = $allAUs"
echo "CLOCKSS. Plugin/names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate released names in the clockss title database
echo "---------------------"
scripts/tdb/tdbout -P -c name tdb/clockssingest/*.tdb | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "CLOCKSS. All Released names = $allAUs"
echo "CLOCKSS. Released names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find number of AUs ready for release in the clockssingest title database
echo "----------------------"
./scripts/tdb/tdbout -Y -t status tdb/clockssingest/*.tdb | sort | uniq -c
echo " "
#
# Find duplicate auids in the ibictpln title database
echo "---------------------"
echo "---------------------"
scripts/tdb/tdbout -AXEa tdb/ibictpln/*.tdb | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "IBICT. All AUids = $allAUs"
echo "IBICT. AUids without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find duplicate name/plugin pairs in the ibictpln title database
echo "---------------------"
scripts/tdb/tdbout -AXE -c plugin,name tdb/ibictpln/*.tdb | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "IBICT. All plugin/names = $allAUs"
echo "IBICT. Plugin/names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
#
# Find number of AUs ready for release in the ibictpln title database
echo "----------------------"
./scripts/tdb/tdbout -Y -t status tdb/ibictpln/*.tdb | sort | uniq -c
echo " "
#
# Find duplicate auids in the whole database. Not exists or expected
#echo "---------------------"
#echo "---------------------"
#scripts/tdb/tdbout -Aa tdb/*/*.tdb | sort > $tpath/allAUs
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
#scripts/tdb/tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher tdb/prod/*.tdb | perl -pe 's/\t+/ /g' | sort -k 1,2 > $tpath/HW_g_all
#scripts/tdb/tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher tdb/prod/*.tdb | perl -pe 's/\t+/ /g' | sort -k 1,2 -u > $tpath/HW_g_dedupe
#cat $tpath/HW_g_all | wc -l
#cat $tpath/HW_g_dedupe | wc -l
#diff $tpath/HW_g_all $tpath/HW_g_dedupe
#diff $tpath/HW_g_all $tpath/HW_g_dedupe | grep "< " | wc -l
#echo "expect 19"
#echo " "
#echo "---------------------"
#echo "---------------------"
#echo "CLOCKSS. HighWire Dupe AUs across plugins"
#scripts/tdb/tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher tdb/clockssingest/*.tdb | perl -pe 's/\t+/ /g' | sort -k 1,2 > $tpath/HW_c_all
#scripts/tdb/tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher tdb/clockssingest/*.tdb | perl -pe 's/\t+/ /g' | sort -k 1,2 -u > $tpath/HW_c_dedupe
#diff $tpath/HW_c_all $tpath/HW_c_dedupe | grep "< " | sort
#diff $tpath/HW_c_all $tpath/HW_c_dedupe | grep "< " | wc -l
#echo "expect 89"
#echo " "
#
# Find issn problems in gln title database
echo "---------------------"
echo "---------------------"
echo "GLN. ISSN issues"
scripts/tdb/tdbout -t publisher,title,issn,eissn tdb/prod/*.tdb | sed 's/\t\(.*\) & /\t\1 and /' | sort -u > $tpath/issn
scripts/tdb/scrub_table.pl $tpath/issn
#
# Find issn problems in clockss title database
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. ISSN issues"
scripts/tdb/tdbout -t publisher,title,issn,eissn tdb/clockssingest/*.tdb | sed 's/\t\(.*\) & /\t\1 and /' | sort -u > $tpath/issn
scripts/tdb/scrub_table.pl $tpath/issn
#
# Find issn problems in ibictpln title database
echo "---------------------"
echo "---------------------"
echo "IBICT. ISSN issues"
scripts/tdb/tdbout -t publisher,title,issn,eissn tdb/ibictpln/*.tdb | sort -u > $tpath/issn
scripts/tdb/scrub_table.pl $tpath/issn
#
# Find Muse titles that don't have attr[journal_id]
echo "---------------------"
echo "---------------------"
echo "GLN. Muse. Titles missing journal_id"
scripts/tdb/tdbout -MTNYP -t publisher,param[journal_dir] -Q '(plugin ~ "ProjectMusePlugin" and (attr[journal_id] is not set or attr[journal_id] is ""))' tdb/prod/*.tdb | sort -u
#
# Find Titles that don't have AUs
echo "---------------------"
echo "---------------------"
echo "GLN. Titles with no AUs"
scripts/tdb/tdbout -j tdb/prod/*.tdb | sort -u > $tpath/AllTitles.txt
scripts/tdb/tdbout -c publisher,title,issn,eissn tdb/prod/*.tdb | sort -u > $tpath/TitlesWAUs.txt
echo "Total Num Titles with no AUs"
diff $tpath/AllTitles.txt $tpath/TitlesWAUs.txt | grep "< " | wc -l
echo "Not incl Springer SBM, AIAA, Annual Reviews, or Medknow"
diff $tpath/AllTitles.txt $tpath/TitlesWAUs.txt | grep "< " | grep -v "Springer Science+Business Media" | grep -v "American Institute of Aeronautics and Astronautics" | grep -v "Annual Reviews," | grep -v "Medknow Publications" | wc -l
diff $tpath/AllTitles.txt $tpath/TitlesWAUs.txt | grep "< " | grep -v "Springer Science+Business Media" | grep -v "American Institute of Aeronautics and Astronautics" | grep -v "Annual Reviews," | grep -v "Medknow Publications" | head -n20
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. Titles with no AUs"
scripts/tdb/tdbout -j tdb/clockssingest/*.tdb | sort -u > $tpath/AllTitlesC.txt
scripts/tdb/tdbout -c publisher,title,issn,eissn tdb/clockssingest/*.tdb | sort -u > $tpath/TitlesWAUsC.txt
echo "Total Num Titles with no AUs"
diff $tpath/AllTitlesC.txt $tpath/TitlesWAUsC.txt | grep "< " | wc -l
echo "Not incl Springer SBM, AIAA, Annual Reviews, or Medknow"
diff $tpath/AllTitlesC.txt $tpath/TitlesWAUsC.txt | grep "< " | grep -v "Springer Science+Business Media" | grep -v "American Institute of Aeronautics and Astronautics" | grep -v "Annual Reviews," | grep -v "Medknow Publications" | wc -l
diff $tpath/AllTitlesC.txt $tpath/TitlesWAUsC.txt | grep "< " | grep -v "Springer Science+Business Media" | grep -v "American Institute of Aeronautics and Astronautics" | grep -v "Annual Reviews," | grep -v "Medknow Publications" | head -n20
echo "---------------------"
echo "---------------------"
echo "Missing Slashes"
grep "param\[base_url\]" tdb/*/*.tdb | grep "http.*://" | grep -v "/\s*$" | grep -v "\/clockss\/"
echo "---------------------"
echo "---------------------"


