#! /bin/bash
t="/home/fagan/tmp"
w="/var/www/fagan"

########GLN
tdbout -MWTNYR -Q 'year ~ "2015"' -t publisher,title tdb/prod/*.tdb | sort > $t/xg2015
tdbout -EXMWTNYR -Q 'year ~ "2016"' -t publisher,title tdb/prod/*.tdb | sort > $t/xg2016
comm -23 $t/xg2015 $t/xg2016 > $w/xg_missing_2016.txt
#cat $w/xg_missing_2016.txt | grep -vi books | grep -v "Maney Publishing" | less


#########CLOCKSS
tdbout -MWTNYCZIF -Q 'year ~ "2015"' -t publisher,title tdb/clockssingest/*.tdb | sort > $t/xc2015
tdbout -EXMWTNYCZIF -Q 'year ~ "2016"' -t publisher,title tdb/clockssingest/*.tdb | sort > $t/xc2016
comm -23 $t/xc2015 $t/xc2016 > $w/xc_missing_2016.txt
#cat $w/xc_missing_2016.txt | grep -vi books | grep -v SciELO | grep -v "Maney Publishing" | less


exit 0

