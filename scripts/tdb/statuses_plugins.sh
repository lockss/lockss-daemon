#! /bin/bash
#
# Script that creates a report on the status of plugins
#

tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath

cd ../../plugins/src

#To get the plugins with AU counts:
grep -rl --include "*.xml" "plugin_identifier" * | sed 's/\(.*\).xml/\1/' | sort -u > $tpath/ab.txt
cat ../../tdb/*/*.tdb | ../../scripts/tdb/tdbout -t plugin | sort -u | sed 's/\./\//g' > $tpath/ac.txt
#plugins that have no AUs.
diff $tpath/ab.txt $tpath/ac.txt | grep "^< " | sed 's/..\(.*\)/\1,0/' > $tpath/foo0.txt
#plugins that have AUs
#shopt -s extglob
find ../../tdb -name clockssingest -prune -o -name \*.tdb -print | xargs cat | ../../scripts/tdb/tdbout -t plugin | sed 's/\./\//g' | awk -F"\t" 'BEGIN {OFS=","} {foo[$0]++} END {for(x in foo) {print x,foo[x]}}' >> $tpath/foo0.txt
#shopt -u extglob

#To get the plugins with and without status:
grep -r -A 1 --include "*.xml" "<string>plugin_status</string>" * | grep "\- " | sed 's/\(.*\).xml-.*<string>\(.*\)<\/string>/\1,\2/' > $tpath/foo1.txt
grep -rL --include "*.xml" "<string>plugin_status</string>" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo1.txt

#To get the plugins with and without substance checkers:
grep -r --include "*.xml" "<string>au_substance_url_pattern</string>" * | sed 's/\(.*\).xml.*<string>.*<\/string>/\1,substance/' > $tpath/foo2.txt
grep -rL --include "*.xml" "<string>au_substance_url_pattern</string>" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo2.txt

#To get the plugins with and without article iterators:
grep -r --include "*.xml" "<string>plugin_article_iterator_factory</string>" * | sed 's/\(.*\).xml.*<string>.*<\/string>/\1,artit/' > $tpath/foo3.txt
grep -rL --include "*.xml" "<string>plugin_article_iterator_factory</string>" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo3.txt

#To get the plugins with and without metadata extractors:
grep -r --include "*.xml" "<string>plugin_article_metadata_extractor_factory</string>" * | sed 's/\(.*\).xml.*<string>.*<\/string>/\1,metadex/' > $tpath/foo4.txt
grep -rL --include "*.xml" "<string>plugin_article_metadata_extractor_factory</string>" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo4.txt

#To get the plugins with and without OpenURL resolver feature URLs:
grep -r --include "*.xml" "<string>au_feature_urls</string>" * | sed 's/\(.*\).xml.*<string>.*<\/string>/\1,openurl/' > $tpath/foo5.txt
grep -rL --include "*.xml" "<string>au_feature_urls</string>" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo5.txt

#To get the plugins with and without the parent plugin:
grep -r -A 1 --include "*.xml" "<string>plugin_parent</string>" * | grep "\- " | sed 's/\(.*\).xml-.*<string>.*\.\(.*\)<\/string>/\1,\2/' > $tpath/foo6.txt
grep -rL --include "*.xml" "<string>plugin_parent</string>" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo6.txt

#To get the plugins with and without the parent plugin version:
grep -r -A 1 --include "*.xml" "<string>plugin_parent_version</string>" * | grep "\- " | sed 's/\(.*\).xml-.*<string>\(.*\)<\/string>/\1,\2/' > $tpath/foo7.txt
grep -rL --include "*.xml" "<string>plugin_parent_version</string>" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo7.txt

#To get the plugins with and without the plugin version:
grep -r -A 1 --include "*.xml" "<string>plugin_version</string>" * | grep "\- " | sed 's/\(.*\).xml-.*<string>\(.*\)<\/string>/\1,\2/' > $tpath/foo8.txt
grep -rL --include "*.xml" "<string>plugin_version</string>" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo8.txt

cat $tpath/foo0.txt | sort -t, -k 1,1 > $tpath/blatz0.txt
cat $tpath/foo1.txt | sort -t, -k 1,1 > $tpath/blatz1.txt
cat $tpath/foo2.txt | sort -t, -k 1,1 > $tpath/blatz2.txt
cat $tpath/foo3.txt | sort -t, -k 1,1 > $tpath/blatz3.txt
cat $tpath/foo4.txt | sort -t, -k 1,1 > $tpath/blatz4.txt
cat $tpath/foo5.txt | sort -t, -k 1,1 > $tpath/blatz5.txt
cat $tpath/foo6.txt | sort -t, -k 1,1 > $tpath/blatz6.txt
cat $tpath/foo7.txt | sort -t, -k 1,1 > $tpath/blatz7.txt
cat $tpath/foo8.txt | sort -t, -k 1,1 > $tpath/blatz8.txt

echo "Plugin,V,Parent,PV,AUs,Substance,Art.It.,MetadataEx,OpenURL,Status"
join -t, -e EMPTY $tpath/blatz8.txt $tpath/blatz6.txt | join -t, -e EMPTY - $tpath/blatz7.txt | join -t, -e EMPTY - $tpath/blatz0.txt | join -t, -e EMPTY - $tpath/blatz2.txt | join -t, -e EMPTY - $tpath/blatz3.txt | join -t, -e EMPTY - $tpath/blatz4.txt | join -t, -e EMPTY - $tpath/blatz5.txt | join -t, -e EMPTY - $tpath/blatz1.txt | sort -t, -k 5 -nr 

exit 0
