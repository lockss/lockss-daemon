#!/bin/bash
#
# Script to provide alerts to problems in the title database
#
# Find duplicates in the gln title database
tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath
cat ../../tdb/prod/*.tdb | ./tdbout -Aa | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "---------------------"
echo "---------------------"
echo "GLN. AUs with all duplicates = $allAUs"
echo "GLN. AUs without duplicates = $uniqAUs"
diff $tpath/allAUs $tpath/dedupedAUs | grep "<" | sed s/..//
echo " "
#
# Find duplicates in the clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -Aa | sort > $tpath/allAUs
uniq $tpath/allAUs > $tpath/dedupedAUs
allAUs=`cat $tpath/allAUs | wc -l`
uniqAUs=`cat $tpath/dedupedAUs | wc -l`
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. AUs with all duplicates = $allAUs"
echo "CLOCKSS. AUs without duplicates = $uniqAUs"
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
echo "expect 1"
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
cat ../../tdb/prod/*.tdb | ./tdbout -t publisher,param[journal_dir] -Q 'plugin ~ "ProjectMusePlugin" and attr[journal_id] is not set' | sort -u
#
# Find Titles that don't have AUs
echo "---------------------"
echo "---------------------"
echo "GLN. Titles with no AUs"
cat ../../tdb/prod/*.tdb | ./tdbout -j | sort -u > $tpath/AllTitles.txt
cat ../../tdb/prod/*.tdb | ./tdbout -c publisher,title,issn,eissn | sort -u > $tpath/TitlesWAUs.txt
titlesNoAUs = `diff $tpath/AllTitles.txt $tpath/TitlesWAUs.txt | grep "< " | grep -v "Springer Science+Business Media" | wc -l
echo "Titles w/o AUs (not incl Spring Sci+Bu):  $titlesNoAUs"
