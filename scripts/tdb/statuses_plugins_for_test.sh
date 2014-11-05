#! /bin/bash
#
# Script that creates a report with the name of the plugin and the status only.
#

tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath

cd plugins/src

#To get the plugins with and without status:
grep -r -A 1 --include "*.xml" "<string>plugin_status</string>" * | grep "\- " | sed 's/.*\/\(.*\).xml-.*<string>\(.*\)<\/string>/\1\t\2/' > $tpath/foo01.txt
grep -rL --include "*.xml" "<string>plugin_status</string>" * | sed 's/.*\/\(.*\).xml/\1\t!/' >> $tpath/foo01.txt

echo -e "Plugin\tStatus"
cat $tpath/foo01.txt | sort -t$'\t' -k 1,1 

exit 0
