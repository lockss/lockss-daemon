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
echo "GLN. Num AUs with duplicates = $allAUs"
uniqAUs=`cat /tmp/bar | wc -l`
echo "GLN. Num AUs without duplicates = $uniqAUs"
diff /tmp/foo /tmp/bar | grep "<" | sed s/..//
echo " "
#
# Find duplicates in the clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -Aa | sort > /tmp/foo
uniq /tmp/foo > /tmp/bar
allAUs=`cat /tmp/foo | wc -l`
echo "---------------------"
echo "---------------------"
echo "CLOCKSS. Num AUs with duplicates = $allAUs"
uniqAUs=`cat /tmp/bar | wc -l`
echo "CLOCKSS. Num AUs without duplicates = $uniqAUs"
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

