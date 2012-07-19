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
cat ../../tdb/*/*.tdb | ../../scripts/tdb/tdbout -t plugin | sed 's/\./\//g' | awk -F"\t" 'BEGIN {OFS=","} {foo[$0]++} END {for(x in foo) {print x,foo[x]}}' >> $tpath/foo0.txt

#To get the plugins with and without status:
grep -r -A 1 --include "*Plugin.xml" "plugin_status" * | grep "\- " | sed 's/\(.*Plugin\).xml-.*<string>\(.*\)<\/string>/\1,\2/' > $tpath/foo1.txt
grep -rL --include "*Plugin.xml" "plugin_status" * | sed 's/\(.*\).xml/\1,!/' | sort >> $tpath/foo1.txt

#To get the plugins with and without substance checkers:
grep -r --include "*Plugin.xml" "au_substance_url_pattern" * | sed 's/\(.*Plugin\).xml.*<string>.*<\/string>/\1,substance/' > $tpath/foo2.txt
grep -rL --include "*Plugin.xml" "au_substance_url_pattern" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo2.txt

#To get the plugins with and without article iterators:
grep -r --include "*Plugin.xml" "plugin_article_iterator_factory" * | sed 's/\(.*Plugin\).xml.*<string>.*<\/string>/\1,artit/' > $tpath/foo3.txt
grep -rL --include "*Plugin.xml" "plugin_article_iterator_factory" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo3.txt

#To get the plugins with and without metadata extractors:
grep -r --include "*Plugin.xml" "plugin_article_metadata_extractor_factory" * | sed 's/\(.*Plugin\).xml.*<string>.*<\/string>/\1,metadex/' > $tpath/foo4.txt
grep -rL --include "*Plugin.xml" "plugin_article_metadata_extractor_factory" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo4.txt

#To get the plugins with and without OpenURL resolver feature URLs:
grep -r --include "*Plugin.xml" "au_feature_urls" * | sed 's/\(.*Plugin\).xml.*<string>.*<\/string>/\1,openurl/' > $tpath/foo5.txt
grep -rL --include "*Plugin.xml" "au_feature_urls" * | sed 's/\(.*\).xml/\1,!/' >> $tpath/foo5.txt

cat $tpath/foo0.txt | sort -t, > $tpath/blatz0.txt
cat $tpath/foo1.txt | sort -t, > $tpath/blatz1.txt
cat $tpath/foo2.txt | sort -t, > $tpath/blatz2.txt
cat $tpath/foo3.txt | sort -t, > $tpath/blatz3.txt
cat $tpath/foo4.txt | sort -t, > $tpath/blatz4.txt
cat $tpath/foo5.txt | sort -t, > $tpath/blatz5.txt

echo "Plugin,AUs,Substance,Art.It.,MetadataEx,OpenURL,Status"
join -t, $tpath/blatz0.txt $tpath/blatz2.txt | join -t, - $tpath/blatz3.txt | join -t, - $tpath/blatz4.txt | join -t, - $tpath/blatz5.txt | join -t, - $tpath/blatz1.txt | sort -t, -k 2 -nr 

exit 0
