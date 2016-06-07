#! /bin/bash
#
# Script that creates a list of auids that are ready to be pushed to the gln
#

tpath="/home/$LOGNAME/tmp"
#mkdir -p $tpath

plugin="lockss"
count=500

# Make a list of AUids that are on ingest machine(s), and 'Yes' have substance, have crawled successfully.
   # Date of last successful crawl is unimportant because many good AUs have been frozen or finished.
   # Run this separately.
   #./scripts/tdb/ws_get_healthy.py machine login pw | sort > $tpath/gr_ingest_healthy.txt

# Make a list of AUids that are crawling in clockssingest, manifest in gln
   #set -x
   # Make a list of AUids from clockss
   ./scripts/tdb/tdbout -CZLI -a -Q "year ~ '^19' and plugin ~ '$plugin' and year !~ '2016'" tdb/clockssingest/*.tdb | sort > $tpath/gr_clockss_c.txt
   # Make a list of AUids from gln
   ./scripts/tdb/tdbout -M -a -Q "year ~ '^19' and plugin ~ '$plugin' and year !~ '2016'" tdb/prod/*.tdb | sort > $tpath/gr_gln_m.txt
   # Convert the gln list to clockss format
   cat $tpath/gr_gln_m.txt | sed -e 's/\(\|[^\|]*\)Plugin/Clockss\1Plugin/' > $tpath/gr_gln_mc.txt
   # Find common items on the clockss list and the clockss-formatted gln list
   comm -12 $tpath/gr_clockss_c.txt $tpath/gr_gln_mc.txt > $tpath/gr_common.txt
   #set +x

# Document Errors. AUs that are in the GLN but not in clockss
   echo "********ERRORS********" > $tpath/gr_errors.txt
   #echo "***Manifest in GLN, but not Crawling in Clockss***" >> $tpath/gr_errors.txt
   #comm -13 $tpath/gr_clockss_c.txt $tpath/gr_gln_mc.txt >> $tpath/gr_errors.txt
   #echo "***Not Manifest in GLN, but Crawling in Clockss***" >> $tpath/gr_errors.txt
   #comm -23 $tpath/gr_clockss_c.txt $tpath/gr_gln_mc.txt >> $tpath/gr_errors.txt

# Find items not healthy on the ingest machines.
   echo "***M on gln. C on clockss. Not healthy on ingest machines.***" >> $tpath/gr_errors.txt
   comm -13 $tpath/gr_ingest_healthy.txt $tpath/gr_common.txt >> $tpath/gr_errors.txt
   # Find common items on the list of AUs with manifest pages, and the list of healthy AUs on the ingest machines.
   comm -12 $tpath/gr_ingest_healthy.txt $tpath/gr_common.txt > $tpath/gr_common_healthy.txt

# Select a random collection of clockss AUids
   shuf $tpath/gr_common_healthy.txt | head -"$count" > $tpath/gr_common_shuf.txt

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
