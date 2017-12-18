#!/bin/bash
#
# Script to generate a list of AUs to test in the gln.
#
# scripts/tdb/tdbout -MWTN -t auid,plugin,publisher:info[tester],status,year tdb/prod/*.tdb | scripts/tdb/statuses_gln.awk


tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath
  echo "taylor_and_francis.tdb" > $tpath/glnfilter
  echo "sage_publications.tdb" >> $tpath/glnfilter

  ls tdb/prod | grep .tdb > $tpath/glnlist
  ls tdb/clockssingest | grep .tdb > $tpath/clocksslist
  diff $tpath/glnlist $tpath/clocksslist | grep "< " | sed 's/..//' > $tpath/notclockss
  echo "american_medical_association.tdb" >> $tpath/notclockss
  echo "purdue_university_press.tdb" >> $tpath/notclockss
  echo "centro_de_filosofia_da_universidade_de_lisboa.tdb" >> $tpath/notclockss
  diff $tpath/glnlist $tpath/notclockss | grep "< " | sed 's/..//' > $tpath/glnAndclockss

#Report any AU in the gln marked notReady, wanted, or testing.
scripts/tdb/tdbout -NWT -t auid,plugin,publisher:info[tester],status,year tdb/prod/*.tdb > $tpath/glntest_a

#Report any AU in the gln marked manifest that does not have a file in clockss.
for file in `cat $tpath/notclockss`
do
  scripts/tdb/tdbout -M -t auid,plugin,publisher:info[tester],status,year tdb/prod/$file >> $tpath/glntest_a
done

#Report some AUs in the gln marked manifest that do not have an equivalent in clockss.
for file in `cat $tpath/glnAndclockss`
do
  if ! grep $file $tpath/glnfilter > /dev/null
  then
    scripts/tdb/tdbout -M -t auid,plugin,publisher:info[tester],status,year -Q 'plugin ~ "ProjectMuse"' tdb/prod/$file >> $tpath/glntest_a
  fi
done
cat $tpath/glntest_a | sort | grep -v "needs.plugin"
exit 0




