#! /bin/bash
t="/home/fagan/tmp"
w="/var/www/fagan"

########GLN
./scripts/tdb/tdbout -MWTNYR -Q 'year ~ "2016"' -t publisher,title tdb/prod/*.tdb | sort > $t/xg2016
./scripts/tdb/tdbout -EXMWTNYR -Q 'year ~ "2017"' -t publisher,title tdb/prod/*.tdb | sort > $t/xg2017
comm -23 $t/xg2016 $t/xg2017 > $w/xg_missing_2017.txt
#cat $w/xg_missing_2017.txt | grep -vi books | grep -v "Maney Publishing" | less


#########CLOCKSS
./scripts/tdb/tdbout -MWTNYCZIF -Q 'year ~ "2016"' -t publisher,title tdb/clockssingest/*.tdb | sort > $t/xc2016
./scripts/tdb/tdbout -EXMWTNYCZIF -Q 'year ~ "2017"' -t publisher,title tdb/clockssingest/*.tdb | sort > $t/xc2017
comm -23 $t/xc2016 $t/xc2017 > $w/xc_missing_2017.txt
#cat $w/xc_missing_2016.txt | grep -vi books | grep -v SciELO | grep -v "Maney Publishing" | less


exit 0

