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
