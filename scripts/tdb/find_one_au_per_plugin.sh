#! /bin/bash
t="~/tmp"

rm eachplugin.txt
#for file in tdb/prod/*.tdb tdb/clockssingest/*.tdb
#for file in tdb/clockssingest/*.tdb
for file in tdb/prod/*.tdb
    do
    echo $file
    ./scripts/tdb/tdbout -EXMTYRCLZF -a $file | sort -u -t$'&' -k1,1 >> eachplugin.txt
done

cat eachplugin.txt | sort -u -t$'&' -k1,1 > eachplugin_final.txt

exit 0

#sort -u -t$'\t' -k3,3 test_uniq.csv
#sort -u -t$'&' -k1,1 test_uniq.csv
