#!/bin/bash
#
# Script to provide alerts to problems in the title database
#
# Find duplicates in the gln title database
cat ../../tdb/prod/*.tdb | ./tdbout -Aa | sort > /tmp/foo
uniq /tmp/foo > /tmp/bar
allAUs=`cat /tmp/foo | wc -l`
echo "---------------------"
echo "---------------------"
echo "GLN. AUs with all duplicates = $allAUs"
uniqAUs=`cat /tmp/bar | wc -l`
echo "GLN. AUs without duplicates = $uniqAUs"
diff /tmp/foo /tmp/bar | grep "<" | sed s/..//
echo " "
#
# Find duplicates in the clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -Aa | sort > /tmp/foo
uniq /tmp/foo > /tmp/bar
allAUs=`cat /tmp/foo | wc -l`
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. AUs with all duplicates = $allAUs"
uniqAUs=`cat /tmp/bar | wc -l`
echo "CLOCKSS. AUs without duplicates = $uniqAUs"
diff /tmp/foo /tmp/bar | grep "<" | sed s/..//
echo " "
#
# Find issn problems in gln title database
cat ../../tdb/prod/*.tdb | ./tdbout -t publisher,title,issn,eissn | sort -u > /tmp/foo
echo "---------------------"
echo "---------------------"
echo "GLN. ISSN issues"
./scrub_table.pl /tmp/foo
#
# Find issn problems in clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -t publisher,title,issn,eissn | sort -u > /tmp/foo
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. ISSN issues"
./scrub_table.pl /tmp/foo
#
# Find Muse titles that don't have attr[journal_id]
echo "---------------------"
echo "---------------------"
echo "GLN. Muse. Titles missing journal_id"
cat *.tdb | ../../scripts/tdb/tdbout -t publisher,param[journal_dir] -Q 'plugin ~ "Muse" and attr[journal_id] is not set' | sort -u
