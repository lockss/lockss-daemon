#! /bin/bash
#
# Script that creates a list of auids that are ready to be pushed to the gln
#

tpath="/home/$LOGNAME/tmp"
#mkdir -p $tpath

# Make a list of AUids that are on ingest machine(s), and 'Yes' have substance, have crawled successfully.
   # Date of last successful crawl is unimportant because many good AUs have been frozen or finished.
   # Run this separately.
   #./scripts/tdb/ws_get_healthy.py machine login pw | sort > $tpath/gr_ingest_healthy.txt

# Make a list of AUids that are crawling in clockssingest, manifest in gln
   # Make a list of AUids from clockss
   ./scripts/tdb/tdbout -C -a -Q 'year ~ "2015" and plugin ~ "HighWirePressH20Plugin"' tdb/clockssingest/sage_publications.tdb | sort > $tpath/gr_clockss_c.txt
   # Make a list of AUids from gln
   ./scripts/tdb/tdbout -M -a -Q 'year ~ "2015" and plugin ~ "HighWirePressH20Plugin"' tdb/prod/sage_publications.tdb | sort > $tpath/gr_gln_m.txt
   # Convert the gln list to clockss format
   cat $tpath/gr_gln_m.txt | sed -e 's/HighWirePressH20Plugin/ClockssHighWirePressH20Plugin/' > $tpath/gr_gln_mc.txt
   # Find common items on the clockss list and the clockss-formatted gln list
   comm -12 $tpath/gr_clockss_c.txt $tpath/gr_gln_mc.txt > $tpath/gr_common.txt
   # Select a random collection of 300 clockss AUids
   shuf $tpath/gr_common.txt | head -300 > $tpath/gr_common_shuf.txt


# Does AU have a clockss and gln manifest page?
   # Look for clockss manifest pages for the previously selected set.
   ./scripts/tdb/read_auid_new.pl $tpath/gr_common_shuf.txt | grep "*M" | sed -e 's/.*, \(org|lockss|plugin|highwire|[^,]*\), .*/\1/' > $tpath/gr_man_cl.txt
   # Convert the list from clockss to gln
   cat $tpath/gr_man_cl.txt | sed -e 's/ClockssHighWirePressH20Plugin/HighWirePressH20Plugin/' > $tpath/gr_common_shuf_g.txt
   # Look for lockss manifest pages for AUids that have clockss manifest pages.
   ./scripts/tdb/read_auid_new.pl $tpath/gr_common_shuf_g.txt  | grep "*M" | sed -e 's/.*, \(org|lockss|plugin|highwire|[^,]*\), .*/\1/' > $tpath/gr_common_shuf_c.txt
   # Convert the list from gln to clockss
   cat $tpath/gr_common_shuf_c.txt | sed -e 's/HighWirePressH20Plugin/ClockssHighWirePressH20Plugin/' | sort > $tpath/gr_common_manifest.txt
   # Find common items on the list of AUs with manifest pages, and the list of healthy AUs on the ingest machines.
   comm -12 $tpath/gr_ingest_healthy.txt $tpath/gr_common_manifest.txt

exit 0

# Example successful AUid
# org|lockss|plugin|highwire|ClockssHighWirePressH20Plugin&base_url~http%3A%2F%2Fhpq%2Esagepub%2Ecom%2F&volume_name~20
# org|lockss|plugin|highwire|HighWirePressH20Plugin&base_url~http%3A%2F%2Fhpq%2Esagepub%2Ecom%2F&volume_name~20
