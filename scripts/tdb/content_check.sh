#!/bin/bash
#
# Script to provide alerts to problems in the title database
#
# This section finds duplicates in the gln title database
cat ../../tdb/prod/*.tdb | ./tdbout -Aa | sort > /tmp/foo
uniq /tmp/foo > /tmp/bar
allAUs=`wc -l /tmp/foo`
echo "GLN. Num AUs with duplicates = $allAUs"
uniqAUs=`wc -l /tmp/bar`
echo "GLN. Num AUs without duplicates = $uniqAUs"
diff /tmp/foo /tmp/bar | grep "<" | sed s/..//
echo " "
#
# This section finds duplicates in the clockss title database
cat ../../tdb/clockssingest/*.tdb | ./tdbout -Aa | sort > /tmp/foo
uniq /tmp/foo > /tmp/bar
allAUs=`wc -l /tmp/foo`
echo "CLOCKSS. Num AUs with duplicates = $allAUs"
uniqAUs=`wc -l /tmp/bar`
echo "CLOCKSS. Num AUs without duplicates = $uniqAUs"
diff /tmp/foo /tmp/bar | grep "<" | sed s/..//
echo " "
