#!/bin/bash
###
# Create a list of plugins that have released AUs with both http & https, that is not tagged with "https transition enabled"
# Run from the root of lockss-daemon. Parallel directory is "tmp"
#tpath="/home/$LOGNAME/tmp"
tpath="../tmp"
mkdir -p $tpath
quick=false
clockss=false
gln=true

USAGE="Usage: `basename $0` [-hvkcg]. If no network is chosen, gln is default"
while getopts :hvkcg OPT; do
    case "$OPT" in
        h)
            echo $USAGE
            exit 0
            ;;
        v)
            echo "`basename $0` version 0.1"
            exit 0
            ;;
        c)
            #Compare clockssingest network 
            clockss=true
            gln=false
            ;;
        g)
            #Compare gln network
            gln=true
            clockss=false
            ;;
        k)
            #Quick mode. Assume lists are already generated correctly
            quick=true
            ;;
        \?)
            # getopts issues an error message
            echo $USAGE >&2
            exit 1
            ;;
    esac
done
# Remove the switches we parsed above.
shift `expr $OPTIND - 1`

echo "clockss $clockss"
echo "gln $gln"
echo "quick $quick"

if [ ${quick} == false ]; then
    #http + https + no_count
    if [ ${clockss} == true ]; then
        ./scripts/tdb/tdbout -CLZFB -t param[base_url],plugin tdb/clockssingest/*.tdb tdb/clockssingest/_retired/*.tdb  | sort | uniq > $tpath/base_url1.txt
    else
        ./scripts/tdb/tdbout -DSR -t param[base_url],plugin tdb/prod/*.tdb tdb/prod/_retired/*.tdb  | sort | uniq > $tpath/base_url1.txt
    fi
    #http+https+count
    if [ ${clockss} == true ]; then
        ./scripts/tdb/tdbout -CLZFB -t param[base_url],plugin tdb/clockssingest/*.tdb tdb/clockssingest/_retired/*.tdb  | sed s/https*:..// |sort | uniq -c > $tpath/base_url4.txt
    else
        ./scripts/tdb/tdbout -DSR -t param[base_url],plugin tdb/prod/*.tdb tdb/prod/_retired/*.tdb  | sed s/https*:..// |sort | uniq -c > $tpath/base_url4.txt
    fi
fi

#Use http + https + no_count and make new lists: one for http and one for https, and remove "https?" so the lists can be compared.
cat $tpath/base_url1.txt | grep http: | sed s/http:..// > $tpath/base_url_http.txt
cat $tpath/base_url1.txt | grep https: | sed s/https:..// > $tpath/base_url_https.txt

#Create a list of plugins with statuses (these are seperated by forward slashes instead of dots)
grep -r -A 1 --include "*.xml" "<string>plugin_status</string>" plugins/src | grep "\- " | sed 's/plugins\/src\/\(.*\).xml-.*<string>\(.*\)<\/string>/\1,\2/' > $tpath/foo01.txt 
grep -rL --include "*.xml" "<string>plugin_status</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo01.txt
cat $tpath/foo01.txt | sort -t, -k 1,1 > $tpath/blatz01.txt #plugin status

#Create a list of plugins with a flag whether they have https or not
grep -r --include "*.xml" " https transition enabled " plugins/src | sed 's/plugins\/src\/\(.*\).xml.*<.*>/\1,HTTPS/' > $tpath/foo05.txt
grep -rL --include "*.xml" " https transition enabled " plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,NO_HTTPS/' >> $tpath/foo05.txt
cat $tpath/foo05.txt | sort -t, -k 1,1 > $tpath/blatz05.txt #https status

#Compare the lists (http and https) and create a new list of url/plugin combos in both lists.
comm -12 $tpath/base_url_http.txt $tpath/base_url_https.txt > $tpath/base_url_both.txt
#Now find those items in a list with a count
echo "base_url/plugin pairs with a count of AUs"
grep -f $tpath/base_url_both.txt $tpath/base_url4.txt | sort -rn
echo "************************************************"
grep -f $tpath/base_url_both.txt $tpath/base_url4.txt | cut -d$'\t' -f2 | sort | uniq | wc -l
grep -f $tpath/base_url_both.txt $tpath/base_url4.txt | cut -d$'\t' -f2 | sed 's%\.%\/%g' | sort | uniq > $tpath/base_url_comp.txt

echo "Plugin name, Plugin status, HTTPS status"
join -t, -e EMPTY $tpath/base_url_comp.txt $tpath/blatz01.txt | join -t, -e EMPTY - $tpath/blatz05.txt

exit 0
