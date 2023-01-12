#! /bin/bash
#Usage 
#This script will check the manifest pages for HighWire
cd /lockss/fagan/lockss-daemon
#Test AUs that are exists or expected for CLOCKSS & GLN
echo ""
echo "**************************"
echo "****Exists or Expected****"
echo "**************************"
./scripts/tdb/tdbout -aEX -Q 'plugin ~ "highwire"' tdb/{prod,clockssingest}/*.tdb | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"

#Test recent AUs that are manifest for for CLOCKSS & GLN
echo ""
echo "****************"
echo "****Manifest****"
echo "****************"
./scripts/tdb/tdbout -aM -Q 'plugin ~ "highwire" and year ~ "202"' tdb/{prod,clockssingest}/*.tdb | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"

#Test recent AUs that are released (GLN) or crawling (CLOCKSS). Ignore http.
echo ""
echo "**********************"
echo "****Released (GLN)****"
echo "**********************"
./scripts/tdb/tdbout -aR -Q 'plugin ~ "highwire" and year ~ "202"' tdb/prod/*.tdb | grep https | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"
echo ""
echo "**********************"
echo "****Crawling (CLOCKSS)****"
echo "**********************"
./scripts/tdb/tdbout -aC -Q 'plugin ~ "highwire" and year ~ "202"' tdb/clockssingest/*.tdb | grep https | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"

exit 0

