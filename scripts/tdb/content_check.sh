#!/bin/bash
#
# Script to provide alerts to problems in the title database
tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath

# Find incorrect status
echo "---------------------"
echo "---------------------"
echo "*Status typos gln: "
cat ../../tdb/prod/*.tdb | ./tdbout -t status | grep -vx manifest | grep -vx released | grep -vx expected | grep -vx exists | grep -vx testing | grep -vx wanted | grep -vx ready | grep -vx down | grep -vx superseded | grep -vx doNotProcess | grep -vx notReady | grep -vx doesNotExist
echo "*Status typos clockssingest: "
#cat ../../tdb/clockssingest/*.tdb | ./tdbout -t status | grep -vx manifest | grep -vx released | grep -vx expected | grep -vx exists | grep -vx testing | grep -vx wanted | grep -vx down | grep -vx superseded | grep -vx doNotProcess | grep -vx notReady | grep -vx doesNotExist | grep -vx crawling | grep -vx zapped
cat ../../tdb/clockssingest/*.tdb | ./tdbout -c status,status2 | sort | uniq -c | sort -n | grep -vw "manifest,exists" | grep -vw "crawling,exists" | grep -vw "released,crawling" | grep -vw "exists,exists" | grep -vw "down,crawling" | grep -vw "doNotProcess,doNotProcess" | grep -vw "expected,exists" | grep -vw "testing,exists" | grep -vw "notReady,exists" | grep -vw "zapped,down" | grep -vw "doesNotExist,doesNotExist"
#
# Find plugins listed in tdb files, that don't exist
# Script is run from lockss-daemon/scripts/tdb
# These items should be run from lockss-daemon/plugins/src
echo "---------------------"
echo "---------------------"
echo "*Plugins that don't exist, but are listed in tdb files: "
cd ../../plugins/src
grep -rl --include "*.xml" "plugin_identifier" * | sed 's/\(.*\).xml/\1/' | sort -u > $tpath/ab.txt
cat ../../tdb/*/*.tdb | ../../scripts/tdb/tdbout -t plugin | sort -u | sed 's/\./\//g' > $tpath/ac.txt
#plugins that have no AUs.
#diff $tpath/ab.txt $tpath/ac.txt | grep "^< "     
#plugins that don't exist, but are listed in tdb files
diff $tpath/ab.txt $tpath/ac.txt | grep "^> "
cd ../../scripts/tdb
#
# Find duplicate auids in the gln title database
cat ../../tdb/prod/*.tdb | ./tdbout -AXEa | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "---------------------"
echo "---------------------"
echo "GLN. All AUids = $allAUs"
echo "GLN. AUids without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
echo " "
#
# Find duplicate auids in the clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -AXEa | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. All AUids = $allAUs"
echo "CLOCKSS. AUids without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
echo " "
#
# Find duplicate name/plugin pairs in the gln title database
cat ../../tdb/prod/*.tdb | ./tdbout -AXE -c plugin,name | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "---------------------"
echo "---------------------"
echo "GLN. All plugin/names = $allAUs"
echo "GLN. Plugin/names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
echo " "
#
# Find duplicate name/plugin pairs in the clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -AXE -c plugin,name | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. All plugin/names = $allAUs"
echo "CLOCKSS. Plugin/names without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
echo " "
#
# Find HighWire plugin dupes
echo "---------------------"
echo "---------------------"
echo "GLN. HighWire Dupe AUs across plugins"
cat ../../tdb/prod/*.tdb | ./tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher | perl -pe 's/\t+/ /g' | sort -k 1,2 > $tpath/HW_g_all
cat ../../tdb/prod/*.tdb | ./tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher | perl -pe 's/\t+/ /g' | sort -k 1,2 -u > $tpath/HW_g_dedupe
cat $tpath/HW_g_all | wc -l
cat $tpath/HW_g_dedupe | wc -l
diff $tpath/HW_g_all $tpath/HW_g_dedupe
diff $tpath/HW_g_all $tpath/HW_g_dedupe | grep "< " | wc -l
echo "expect 19"
echo " "
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. HighWire Dupe AUs across plugins"
cat ../../tdb/clockssingest/*.tdb | ./tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher | perl -pe 's/\t+/ /g' | sort -k 1,2 > $tpath/HW_c_all
cat ../../tdb/clockssingest/*.tdb | ./tdbout -Q 'plugin ~ "highwire"' -URD -t param[base_url],param[volume_name],param[volume],status,publisher | perl -pe 's/\t+/ /g' | sort -k 1,2 -u > $tpath/HW_c_dedupe
diff $tpath/HW_c_all $tpath/HW_c_dedupe | grep "< " | sort
diff $tpath/HW_c_all $tpath/HW_c_dedupe | grep "< " | wc -l
echo "expect 89"
echo " "
#
# Find issn problems in gln title database
echo "---------------------"
echo "---------------------"
echo "GLN. ISSN issues"
cat ../../tdb/prod/*.tdb | ./tdbout -t publisher,title,issn,eissn | sort -u > $tpath/issn
./scrub_table.pl $tpath/issn
#
# Find issn problems in clockss title database
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. ISSN issues"
cat ../../tdb/clockssingest/*.tdb | ./tdbout -t publisher,title,issn,eissn | sort -u > $tpath/issn
./scrub_table.pl $tpath/issn
#
# Find Muse titles that don't have attr[journal_id]
echo "---------------------"
echo "---------------------"
echo "GLN. Muse. Titles missing journal_id"
cat ../../tdb/prod/*.tdb | ./tdbout -MTNYP -t publisher,param[journal_dir] -Q '(plugin ~ "ProjectMusePlugin" and (attr[journal_id] is not set or attr[journal_id] is ""))' | sort -u
#
# Find Titles that don't have AUs
echo "---------------------"
echo "---------------------"
echo "GLN. Titles with no AUs"
cat ../../tdb/prod/*.tdb | ./tdbout -j | sort -u > $tpath/AllTitles.txt
cat ../../tdb/prod/*.tdb | ./tdbout -c publisher,title,issn,eissn | sort -u > $tpath/TitlesWAUs.txt
echo "Total Num Titles with no AUs"
diff $tpath/AllTitles.txt $tpath/TitlesWAUs.txt | grep "< " | wc -l
echo "Not incl Springer SBM, AIAA, Annual Reviews, or Medknow"
diff $tpath/AllTitles.txt $tpath/TitlesWAUs.txt | grep "< " | grep -v "Springer Science+Business Media" | grep -v "American Institute of Aeronautics and Astronautics" | grep -v "Annual Reviews," | grep -v "Medknow Publications" | wc -l
diff $tpath/AllTitles.txt $tpath/TitlesWAUs.txt | grep "< " | grep -v "Springer Science+Business Media" | grep -v "American Institute of Aeronautics and Astronautics" | grep -v "Annual Reviews," | grep -v "Medknow Publications" | head -n20
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. Titles with no AUs"
cat ../../tdb/clockssingest/*.tdb | ./tdbout -j | sort -u > $tpath/AllTitlesC.txt
cat ../../tdb/clockssingest/*.tdb | ./tdbout -c publisher,title,issn,eissn | sort -u > $tpath/TitlesWAUsC.txt
echo "Total Num Titles with no AUs"
diff $tpath/AllTitlesC.txt $tpath/TitlesWAUsC.txt | grep "< " | wc -l
echo "Not incl Springer SBM, AIAA, Annual Reviews, or Medknow"
diff $tpath/AllTitlesC.txt $tpath/TitlesWAUsC.txt | grep "< " | grep -v "Springer Science+Business Media" | grep -v "American Institute of Aeronautics and Astronautics" | grep -v "Annual Reviews," | grep -v "Medknow Publications" | wc -l
diff $tpath/AllTitlesC.txt $tpath/TitlesWAUsC.txt | grep "< " | grep -v "Springer Science+Business Media" | grep -v "American Institute of Aeronautics and Astronautics" | grep -v "Annual Reviews," | grep -v "Medknow Publications" | head -n20
echo "---------------------"
echo "---------------------"


