#! /bin/bash
#
# Script that creates a report on the status of plugins
#

tpath="/home/$LOGNAME/tmp"
mkdir -p $tpath

#To get the plugins with AU counts:
grep -rl --include "*.xml" "plugin_identifier" plugins/src | sed 's/\(.*\).xml/\1/' | sort -u > $tpath/ab.txt
scripts/tdb/tdbout -t plugin tdb/*/*.tdb | sort -u | sed 's/\./\//g' > $tpath/ac.txt
#plugins that have no AUs.
diff $tpath/ab.txt $tpath/ac.txt | grep "^< " | sed 's/..\(.*\)/\1,0/' > $tpath/foo00.txt
#plugins that have AUs
#shopt -s extglob
scripts/tdb/tdbout -t plugin `find tdb -name \*.tdb -print` | sed 's/\./\//g' | awk -F"\t" 'BEGIN {OFS=","} {foo[$0]++} END {for(x in foo) {print x,foo[x]}}' >> $tpath/foo00.txt
#shopt -u extglob

#To get the plugins with and without status:
grep -r -A 1 --include "*.xml" "<string>plugin_status</string>" plugins/src | grep "\- " | sed 's/plugins\/src\/\(.*\).xml-.*<string>\(.*\)<\/string>/\1,\2/' > $tpath/foo01.txt
grep -rL --include "*.xml" "<string>plugin_status</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo01.txt

#Create a base list of plugins
cat $tpath/foo01.txt | sed 's/,.*//' | sort -t, -k 1,1 > $tpath/AllPlugins.txt

#To get the plugins with and without substance checkers:
grep -r --include "*.xml" "<string>au_substance_url_pattern</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml.*<string>.*<\/string>/\1,substance/' > $tpath/foo02.txt
grep -rL --include "*.xml" "<string>au_substance_url_pattern</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo02.txt

#To get the plugins with and without article iterators:
grep -r --include "*.xml" "<string>plugin_article_iterator_factory</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml.*<string>.*<\/string>/\1,artit/' > $tpath/foo03.txt
grep -rL --include "*.xml" "<string>plugin_article_iterator_factory</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo03.txt

#To get the plugins with and without metadata extractors:
grep -r --include "*.xml" "<string>plugin_article_metadata_extractor_factory</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml.*<string>.*<\/string>/\1,metadex/' > $tpath/foo04.txt
grep -rL --include "*.xml" "<string>plugin_article_metadata_extractor_factory</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo04.txt

#To get the plugins with and without OpenURL resolver feature URLs:
grep -r --include "*.xml" "<string>au_feature_urls</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml.*<string>.*<\/string>/\1,openurl/' > $tpath/foo05.txt
grep -rL --include "*.xml" "<string>au_feature_urls</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo05.txt

#To get the plugins with and without the parent plugin:
grep -r -A 1 --include "*.xml" "<string>plugin_parent</string>" plugins/src | grep "\- " | sed 's/plugins\/src\/\(.*\).xml-.*<string>.*\.\(.*\)<\/string>/\1,\2/' > $tpath/foo06.txt
grep -rL --include "*.xml" "<string>plugin_parent</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo06.txt

#To get the plugins with and without the parent plugin version:
grep -r -A 1 --include "*.xml" "<string>plugin_parent_version</string>" plugins/src | grep "\- " | sed 's/plugins\/src\/\(.*\).xml-.*<string>\(.*\)<\/string>/\1,\2/' > $tpath/foo07.txt
grep -rL --include "*.xml" "<string>plugin_parent_version</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo07.txt

#To get the plugins with and without the plugin version:
grep -r -A 1 --include "*.xml" "<string>plugin_version</string>" plugins/src | grep "\- " | sed 's/plugins\/src\/\(.*\).xml-.*<string>\(.*\)<\/string>/\1,\2/' > $tpath/foo08.txt
grep -rL --include "*.xml" "<string>plugin_version</string>" plugins/src | sed 's/plugins\/src\/\(.*\).xml/\1,!/' >> $tpath/foo08.txt

#To get the plugins on and not on the content machines 9-16 (gln testing)
sh scripts/tdb/statuses_plugins_content_9-16.sh | grep -i plugin | sed 's/\t/,/' > $tpath/foo09.txt
cat $tpath/foo09.txt | sed 's/,.*//' | sort -t, -k 1,1 > $tpath/bar.txt
diff $tpath/bar.txt $tpath/AllPlugins.txt | grep ">" | sed 's/..//' | sed 's/\(.*\)/\1,!/' >> $tpath/foo09.txt

#To get the plugins on and not on the ingest machines
sh scripts/tdb/statuses_plugins_ingest.sh | grep -i plugin | sed 's/\t/,/' > $tpath/foo10.txt
cat $tpath/foo10.txt | sed 's/,.*//' | sort -t, -k 1,1 > $tpath/bar.txt
diff $tpath/bar.txt $tpath/AllPlugins.txt | grep ">" | sed 's/..//' | sed 's/\(.*\)/\1,!/' >> $tpath/foo10.txt

#To get the plugins on and not on the delta machines
sh scripts/tdb/statuses_plugins_delta.sh | grep -i plugin | sed 's/\t/,/' > $tpath/foo11.txt
cat $tpath/foo11.txt | sed 's/,.*//' | sort -t, -k 1,1 > $tpath/bar.txt
diff $tpath/bar.txt $tpath/AllPlugins.txt | grep ">" | sed 's/..//' | sed 's/\(.*\)/\1,!/' >> $tpath/foo11.txt

#To get the plugins on and not on the content machines 1-4 (clockss testing)
sh scripts/tdb/statuses_plugins_content_1-4.sh | grep -i plugin | sed 's/\t/,/' > $tpath/foo12.txt
cat $tpath/foo12.txt | sed 's/,.*//' | sort -t, -k 1,1 > $tpath/bar.txt
diff $tpath/bar.txt $tpath/AllPlugins.txt | grep ">" | sed 's/..//' | sed 's/\(.*\)/\1,!/' >> $tpath/foo12.txt

cat $tpath/foo00.txt | sort -t, -k 1,1 > $tpath/blatz00.txt #numbers of AUs
cat $tpath/foo01.txt | sort -t, -k 1,1 > $tpath/blatz01.txt #plugin status
cat $tpath/foo02.txt | sort -t, -k 1,1 > $tpath/blatz02.txt #substance checkers
cat $tpath/foo03.txt | sort -t, -k 1,1 > $tpath/blatz03.txt #article iterators
cat $tpath/foo04.txt | sort -t, -k 1,1 > $tpath/blatz04.txt #metadata extractors
cat $tpath/foo05.txt | sort -t, -k 1,1 > $tpath/blatz05.txt #url resolver
cat $tpath/foo06.txt | sort -t, -k 1,1 > $tpath/blatz06.txt #Parent plugin
cat $tpath/foo07.txt | sort -t, -k 1,1 > $tpath/blatz07.txt #Parent verions
cat $tpath/foo08.txt | sort -t, -k 1,1 > $tpath/blatz08.txt #Plugin version
cat $tpath/foo09.txt | sort -t, -k 1,1 > $tpath/blatz09.txt #Plugins on content machines 9-16
cat $tpath/foo10.txt | sort -t, -k 1,1 > $tpath/blatz10.txt #Plugins on ingest machines
cat $tpath/foo11.txt | sort -t, -k 1,1 > $tpath/blatz11.txt #Plugins on delta machines
cat $tpath/foo12.txt | sort -t, -k 1,1 > $tpath/blatz12.txt #Plugins on content machines 1-4

echo "Plugin,CVS,C9-16,D1-13,C1-4,I1-4,Parent,PV,AUs,Substance,ArtIt,MetadataEx,OpenURL,Status"
join -t, -e EMPTY $tpath/blatz08.txt $tpath/blatz09.txt | join -t, -e EMPTY - $tpath/blatz11.txt | join -t, -e EMPTY - $tpath/blatz12.txt | join -t, -e EMPTY - $tpath/blatz10.txt | join -t, -e EMPTY - $tpath/blatz06.txt | join -t, -e EMPTY - $tpath/blatz07.txt | join -t, -e EMPTY - $tpath/blatz00.txt | join -t, -e EMPTY - $tpath/blatz02.txt | join -t, -e EMPTY - $tpath/blatz03.txt | join -t, -e EMPTY - $tpath/blatz04.txt | join -t, -e EMPTY - $tpath/blatz05.txt | join -t, -e EMPTY - $tpath/blatz01.txt | sort -t, -k 9 -nr 
date

exit 0
