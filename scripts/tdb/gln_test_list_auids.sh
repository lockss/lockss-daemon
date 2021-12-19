#!/bin/bash
#
# Script to generate a list of AUs to test in the gln.
#
# scripts/tdb/tdbout -MWTN -t auid,plugin,publisher:info[tester],status,year tdb/prod/*.tdb | scripts/tdb/statuses_gln.awk

tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath
#  echo "taylor_and_francis.tdb" > $tpath/glnfilter
#  echo "sage_publications.tdb" >> $tpath/glnfilter

  #Collect just the names of the files, not the paths. 
  #Make sure all the files end in ".tdb"
  #Do not look in the _retired folder or other sub-folders
  ls tdb/prod/*.tdb | sed s'/tdb\/prod\///' > $tpath/glnlist
  ls tdb/clockssingest/*.tdb  | sed s'/tdb\/clockssingest\///' > $tpath/clocksslist
  #do not look at Springer AUids.
  diff $tpath/glnlist $tpath/clocksslist | grep "< " | sed 's/..//' | grep -v springer > $tpath/notclockss
  #Add these files to be processed because they are different enough from the clockss version.
  echo "american_medical_association.tdb" >> $tpath/notclockss
  #echo "purdue_university_press.tdb" >> $tpath/notclockss
  #echo "centro_de_filosofia_da_universidade_de_lisboa.tdb" >> $tpath/notclockss
  #  diff $tpath/glnlist $tpath/notclockss | grep "< " | sed 's/..//' > $tpath/glnAndclockss

  #Report any AU in the gln marked manifest that does not have a file in clockss.
  echo "" > $tpath/glntest_a #clear the file.
  for file in `cat $tpath/notclockss`
  do
    scripts/tdb/tdbout -M -t publisher,plugin,auid,publisher:info[tester],status,year tdb/prod/$file >> $tpath/glntest_a #don't clear the file in the loop
  done

#Report specific publishers AUs in the gln marked manifest that do have an equivalent in clockss.
##for file in `cat $tpath/glnAndclockss`
#do
#  if ! grep $file $tpath/glnfilter > /dev/null
#  then
#    scripts/tdb/tdbout -M -t publisher,plugin,auid,publisher:info[tester],status,year -Q 'plugin ~ "ProjectMuse"' tdb/prod/$file >> $tpath/glntest_a # don't clear the file in the loop
#  fi
#done
cat $tpath/glntest_a | sort | grep -v "needs.plugin" | grep -v HWDrupalPlugin | grep -v GenericAtyponPlugin
exit 0

