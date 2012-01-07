#!/bin/bash
#
# Script to provide alerts to problems in the title database
#
# Find duplicates in the gln title database
cat ../../tdb/prod/*.tdb | ./tdbout -Aa | sort > /tmp/allAUs
uniq /tmp/allAUs > /tmp/dedupedAUs
allAUs=`cat /tmp/allAUs | wc -l`
uniqAUs=`cat /tmp/dedupedAUs | wc -l`
echo "---------------------"
echo "---------------------"
echo "GLN. AUs with all duplicates = $allAUs"
echo "GLN. AUs without duplicates = $uniqAUs"
diff /tmp/allAUs /tmp/dedupedAUs | grep "<" | sed s/..//
echo " "
#
# Find duplicates in the clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -Aa | sort > /tmp/foo
uniq /tmp/allAUs > /tmp/dedupedAUs
allAUs=`cat /tmp/allAUs | wc -l`
uniqAUs=`cat /tmp/dedupedAUs | wc -l`
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. AUs with all duplicates = $allAUs"
echo "CLOCKSS. AUs without duplicates = $uniqAUs"
diff /tmp/allAUs /tmp/dedupedAUs | grep "<" | sed s/..//
echo " "
#
# Find issn problems in gln title database
cat ../../tdb/prod/*.tdb | ./tdbout -t publisher,title,issn,eissn | sort -u > /tmp/issn
echo "---------------------"
echo "---------------------"
echo "GLN. ISSN issues"
./scrub_table.pl /tmp/issn
#
# Find issn problems in clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -t publisher,title,issn,eissn | sort -u > /tmp/issn
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. ISSN issues"
./scrub_table.pl /tmp/issn
#
# Find Muse titles that don't have attr[journal_id]
echo "---------------------"
echo "---------------------"
echo "GLN. Muse. Titles missing journal_id"
cat ../../tdb/prod/*.tdb | ./tdbout -t publisher,param[journal_dir] -Q 'plugin ~ "ProjectMusePlugin" and attr[journal_id] is not set' | sort -u
