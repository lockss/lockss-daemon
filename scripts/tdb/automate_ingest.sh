#! /bin/bash
t="/home/$LOGNAME/tmp"

#Sage/Highwire (not OUP), in order by year
#Ready
echo "###Ready highwire" > $t/tmp_HW
./scripts/tdb/tdbout -Y -a -Q 'plugin ~ "highwire" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/*.tdb | grep -v oxfordjournals | shuf >> $t/tmp_HW
#Manifest
echo "###Manifest sage 2015" >> $t/tmp_HW
./scripts/tdb/tdbout -MT -a -Q 'plugin ~ "highwire" and year is "2015" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/sage_publications.tdb | shuf >> $t/tmp_HW
echo "###Manifest sage 2014" >> $t/tmp_HW
./scripts/tdb/tdbout -MT -a -Q 'plugin ~ "highwire" and year ~ "2014" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/sage_publications.tdb | shuf >> $t/tmp_HW
echo "###Manifest sage 2013" >> $t/tmp_HW
./scripts/tdb/tdbout -MT -a -Q 'plugin ~ "highwire" and year ~ "2013" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/sage_publications.tdb | shuf >> $t/tmp_HW
echo "###Manifest sage 2012" >> $t/tmp_HW
./scripts/tdb/tdbout -MT -a -Q 'plugin ~ "highwire" and year ~ "2012" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/sage_publications.tdb | shuf >> $t/tmp_HW
echo "###Manifest sage 2011" >> $t/tmp_HW
./scripts/tdb/tdbout -MT -a -Q 'plugin ~ "highwire" and year ~ "2011" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/sage_publications.tdb | shuf >> $t/tmp_HW
echo "###Manifest sage 2010" >> $t/tmp_HW
./scripts/tdb/tdbout -MT -a -Q 'plugin ~ "highwire" and year ~ "2010" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/sage_publications.tdb | shuf >> $t/tmp_HW
echo "###Manifest sage 2009" >> $t/tmp_HW
./scripts/tdb/tdbout -MT -a -Q 'plugin ~ "highwire" and year ~ "2009" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/sage_publications.tdb | shuf >> $t/tmp_HW
echo "###Manifest sage 2008" >> $t/tmp_HW
./scripts/tdb/tdbout -MT -a -Q 'plugin ~ "highwire" and year ~ "2008" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/sage_publications.tdb | shuf >> $t/tmp_HW

#Atypon (not T&F), in order by year
#Ready
echo "###Ready Atypon 2015" > $t/tmp_Atypon
./scripts/tdb/tdbout -Y -a -Q 'plugin ~ "typon" and year is "2015" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/*.tdb | shuf >> $t/tmp_Atypon
echo "###Ready Atypon not 2015" >> $t/tmp_Atypon
./scripts/tdb/tdbout -Y -a -Q 'plugin ~ "typon" and year is not "2015" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/*.tdb | shuf >> $t/tmp_Atypon

#Misc: all but Highwire, T&F, Atypon
#Ready
echo "###Ready Misc 2015" > $t/tmp_Misc
./scripts/tdb/tdbout -Y -a -Q 'year is "2015" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/*.tdb | grep -v highwire | grep -v typon | grep -v ClockssTaylorAndFrancisPlugin | shuf >> $t/tmp_Misc
echo "###Ready Misc not 2015" >> $t/tmp_Misc
./scripts/tdb/tdbout -Y -a -Q 'year is not "2015" and (au:hidden[proxy] is not set or au:hidden[proxy] is "")' tdb/clockssingest/*.tdb | grep -v highwire | grep -v typon | grep -v ClockssTaylorAndFrancisPlugin | shuf >> $t/tmp_Misc
#Single Ingest Machines: all but Highwire, T&F
echo "*********************" >> $t/tmp_Misc
echo "###Ready Misc Ingest1" >> $t/tmp_Misc
./scripts/tdb/tdbout -Y -a -Q 'au:hidden[proxy] is "reingest1.clockss.org:8082"' tdb/clockssingest/*.tdb | grep -v highwire | grep -v ClockssTaylorAndFrancisPlugin | shuf >> $t/tmp_Misc
echo "*********************" >> $t/tmp_Misc
echo "###Ready Misc Ingest2" >> $t/tmp_Misc
./scripts/tdb/tdbout -Y -a -Q 'au:hidden[proxy] is "reingest2.clockss.org:8085"' tdb/clockssingest/*.tdb | grep -v highwire | grep -v ClockssTaylorAndFrancisPlugin | shuf >> $t/tmp_Misc
echo "*********************" >> $t/tmp_Misc
echo "###Ready Misc Ingest3" >> $t/tmp_Misc
./scripts/tdb/tdbout -Y -a -Q 'au:hidden[proxy] is "reingest3.clockss.org:8083"' tdb/clockssingest/*.tdb | grep -v highwire | grep -v ClockssTaylorAndFrancisPlugin | shuf >> $t/tmp_Misc
echo "*********************" >> $t/tmp_Misc
echo "###Ready Misc Ingest4" >> $t/tmp_Misc
./scripts/tdb/tdbout -Y -a -Q 'au:hidden[proxy] is "reingest4.clockss.org:8082"' tdb/clockssingest/*.tdb | grep -v highwire | grep -v ClockssTaylorAndFrancisPlugin | shuf >> $t/tmp_Misc

head -n30 $t/tmp_HW > $t/tmp_All
head -n100 $t/tmp_Atypon >> $t/tmp_All
head -n50 $t/tmp_Misc >> $t/tmp_All

exit 0

# ./scripts/tdb/tdbedit --from-status=manifest,ready --to-status=crawling --auids=tmp_All tdb/clockssingest/*.tdb
# ./scripts/tdb/tdbedit --from-status=exists --to-status=manifest --auids=../SageEdits/tmp tdb/clockssingest/multi_science.tdb
# ./scripts/tdb/tdbedit --from-status=exists,expected --to-status=manifest --auids=../SageEdits/tmp tdb/ibictpln/*.tdb