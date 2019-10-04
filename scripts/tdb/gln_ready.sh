#! /bin/bash
#
# Script that creates a list of auids that are ready to be pushed to the gln, based on clockss test results
#

tpath="/home/$LOGNAME/tmp"
#mkdir -p $tpath

plugin="lockss"
count=250
year=`date +%Y`
#count=25

#After February of this year, you can release content from last year. ignore this year.
week=`date +%W`
if [ $week -lt 8 ]
then
  ((year--))
  #echo $year
fi
#echo $year

# Make a list of AUids that are on ingest machine(s), and 'Yes' have substance, have crawled successfully at least once.
   # Date of last successful crawl is unimportant because many good AUs have been frozen or finished.
   # Run this separately, once per day.
   #./scripts/tdb/ws_get_healthy.py machine login pw | sort > $tpath/gr_ingest_healthy.txt

# Make a list of AUids that are crawling in clockssingest, manifest in gln
   #set -x
   # Make a list of AUids from clockss
   #./scripts/tdb/tdbout -CZLI -a -Q "year ~ '^20' and plugin ~ '$plugin' and year !~ '$year'" tdb/clockssingest/*.tdb | grep -v TaylorAndFrancisPlugin | sort > $tpath/gr_clockss_c.txt
   ./scripts/tdb/tdbout -CZLIF -a -Q "plugin ~ "'$plugin'" and year !~ "'$year'"" tdb/clockssingest/*.tdb | sort > $tpath/gr_clockss_c.txt
   # Make a list of AUids from gln
   #./scripts/tdb/tdbout -M -a -Q "year ~ '^20' and plugin ~ '$plugin' and year !~ '$year'" tdb/prod/*.tdb | grep -v TaylorAndFrancisPlugin | sort > $tpath/gr_gln_m.txt
   ./scripts/tdb/tdbout -M -a -Q "plugin ~ "'$plugin'" and year !~ "'$year'"" tdb/prod/*.tdb | sort > $tpath/gr_gln_m.txt

   # Convert the gln list to clockss format, and start a list
   cat $tpath/gr_gln_m.txt | sed -e 's/\(\|[^\|]*\)Plugin/Clockss\1Plugin/' | sort > $tpath/gr_gln_mc.txt
   # Find common items on the clockss list and the clockss-formatted gln list
   comm -12 $tpath/gr_clockss_c.txt $tpath/gr_gln_mc.txt > $tpath/gr_common.txt

   # Also convert the https items to http items, convert to clockss format, and start a list
   # In this script use http to compare to clockss AUs, and to look for healthy. But convert back to https to look for manifest pages.
   cat $tpath/gr_gln_m.txt | grep https%3A | grep -v ProjectMuse2017Plugin | sed -e 's/https/http/g' | sed -e 's/\(\|[^\|]*\)Plugin/Clockss\1Plugin/' | sort > $tpath/gr_gln_mcs.txt
   # Find common items on the clockss list and the clockss-formatted gln list
   comm -12 $tpath/gr_clockss_c.txt $tpath/gr_gln_mcs.txt > $tpath/gr_common_s.txt
   #set +x

# Document Errors. AUs that are in the GLN but not in clockss
   echo "********ERRORS********" > $tpath/gr_errors.txt #create error file 
   #echo "***Manifest in GLN, but not Crawling in Clockss***" >> $tpath/gr_errors.txt
   #comm -13 $tpath/gr_clockss_c.txt $tpath/gr_gln_mc.txt | shuf | head -10 | sed 's/^/***/' >> $tpath/gr_errors.txt
   #echo "***Not Manifest in GLN, but Crawling in Clockss***" >> $tpath/gr_errors.txt
   #comm -23 $tpath/gr_clockss_c.txt $tpath/gr_gln_mc.txt | shuf | head -10 | sed 's/^/***/' >> $tpath/gr_errors.txt

# Find items not healthy on the ingest machines.
   #echo "***M on gln. C on clockss. Not healthy on ingest machines.***" >> $tpath/gr_errors.txt
   #comm -13 $tpath/gr_ingest_healthy.txt $tpath/gr_common.txt >> $tpath/gr_errors.txt
   # Find common items on the list of AUs with manifest pages, and the list of healthy AUs on the ingest machines.
   
# Find items healthy on the ingest machines.
   comm -12 $tpath/gr_ingest_healthy.txt $tpath/gr_common.txt > $tpath/gr_common_healthy.txt
   comm -12 $tpath/gr_ingest_healthy.txt $tpath/gr_common_s.txt > $tpath/gr_common_healthy_s.txt
   #Check health using http, but before checking for manifest pages move back to https and merge with the other list
   #cat $tpath/gr_common_healthy_s.txt | sed -e 's/http/https/g' >> $tpath/gr_common_healthy.txt
   #Fix Hindawi so that downloads uses http. https%3A%2F%2Fdownloads%2Ehindawi%2Ecom
   cat $tpath/gr_common_healthy_s.txt | sed -e 's/http/https/g' | sed -e 's/https\(%3A%2F%2Fdownloads%2Ehindawi%2Ecom\)/http\1/' >> $tpath/gr_common_healthy.txt

# Select a random collection of clockss AUids
   cat $tpath/gr_common_healthy.txt | sort | uniq | shuf | head -"$count" > $tpath/gr_common_shuf.txt
   #shuf $tpath/gr_common_healthy.txt | head -"$count" > $tpath/gr_common_shuf.txt
   #After health check, convert back to https and merge lists together
   #shuf $tpath/gr_common_healthy_s.txt | sed 's/http/https/g' | head -"$count" >> $tpath/gr_common_shuf.txt
   #cat $tpath/gr_common_shuf_s1.txt | sed -e 's/http/https/g' > $tpath/gr_common_shuf_s2.txt #this one is https. For manifest page finding.

# FOR All AUs
# Does AU have a clockss and gln manifest page?
   # Look for clockss manifest pages for the previously selected set.
   ./scripts/tdb/read_auid_new.pl $tpath/gr_common_shuf.txt > $tpath/gr_man_clks.txt
   cat $tpath/gr_man_clks.txt | grep "*N" >> $tpath/gr_errors.txt
   cat $tpath/gr_man_clks.txt | grep "*M" | sed -e 's/.*, \(org|lockss|plugin|[^,]*\), .*/\1/' > $tpath/gr_found_cl.txt
   # Convert the list from clockss to gln
   cat $tpath/gr_found_cl.txt | sed -e 's/Clockss\([^\|]*\)Plugin/\1Plugin/' > $tpath/gr_found_cl_g.txt
   # Look for lockss manifest pages for AUids that have clockss manifest pages.
   ./scripts/tdb/read_auid_new.pl $tpath/gr_found_cl_g.txt > $tpath/gr_man_gln.txt
   cat $tpath/gr_man_gln.txt | grep "*N" >> $tpath/gr_errors.txt
   cat $tpath/gr_man_gln.txt | grep "*M" | sed -e 's/.*, \(org|lockss|plugin|[^,]*\), .*/\1/' > $tpath/gr_found_gln.txt

# Output
   cat $tpath/gr_found_gln.txt
   cat $tpath/gr_errors.txt

exit 0

# Example successful AUid
# org|lockss|plugin|highwire|ClockssHighWirePressH20Plugin&base_url~http%3A%2F%2Fhpq%2Esagepub%2Ecom%2F&volume_name~20
# org|lockss|plugin|highwire|HighWirePressH20Plugin&base_url~http%3A%2F%2Fhpq%2Esagepub%2Ecom%2F&volume_name~20
