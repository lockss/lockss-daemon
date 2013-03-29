#!/bin/bash
#
# Script to generate a list of AUs to test in the gln.
#
# cat tdb/prod/*.tdb | ./scripts/tdb/tdbout -MWTN -t publisher,plugin,publisher:info[tester],status | ./scripts/tdb/statuses_gln.awk


tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath
  echo "taylor_and_francis.tdb" > $tpath/glnfilter
  echo "sage_publications.tdb" >> $tpath/glnfilter

  ls ../../tdb/prod | grep .tdb > $tpath/glnlist
  ls ../../tdb/clockssingest | grep .tdb > $tpath/clocksslist
  diff $tpath/glnlist $tpath/clocksslist | grep "< " | sed 's/..//' > $tpath/notclockss
  echo "american_medical_association.tdb" >> $tpath/notclockss
  echo "purdue_university_press.tdb" >> $tpath/notclockss
  echo "centro_de_filosofia_da_universidade_de_lisboa.tdb" >> $tpath/notclockss
  diff $tpath/glnlist $tpath/notclockss | grep "< " | sed 's/..//' > $tpath/glnAndclockss

#Report any AU in the gln marked notReady, wanted, or testing.
cat ../../tdb/prod/*.tdb | ./tdbout -NWT -t publisher,plugin,publisher:info[tester],status,year > $tpath/glntest

#Report any AU in the gln marked manifest that does not have a file in clockss.
for file in `cat $tpath/notclockss`
do
  ./tdbout -M -t publisher,plugin,publisher:info[tester],status,year -i ../../tdb/prod/$file >> $tpath/glntest
done

#Report some AUs in the gln marked manifest that do not have an equivalent in clockss.
for file in `cat $tpath/glnAndclockss`
do
  if ! grep $file $tpath/glnfilter > /dev/null
  then
    ./tdbout -M -t publisher,plugin,publisher:info[tester],status,year -Q 'plugin ~ "OJS" or plugin ~ "ProjectMuse"' -i ../../tdb/prod/$file >> $tpath/glntest
#    ./tdbout -M -t auid,publisher,plugin,publisher:info[tester],status -i ../../tdb/prod/$file > $tpath/glnM
#    cat $tpath/glnM | sed 's/\t.*$//' | sed -e 's/^[^&]*&//' | sort | uniq > $tpath/glnauids
#    ./tdbout -M -t auid | sed -e 's/^[^&]*&//' -i ../../tdb/clockssingest/$file | sort | uniq > $tpath/clockssauids
#    diff $tpath/glnauids $tpath/clockssauids | grep "> " | sed 's/..//' > $tpath/mysteryauids
  fi
done
cat $tpath/glntest | sort | grep -v "needs.plugin"
exit 0




