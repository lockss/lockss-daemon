#! /bin/bash
#Usage 
#This script will check the manifest pages for HighWire
cd /lockss/fagan/lockss-daemon
#Test AUs that are expected for CLOCKSS & GLN
echo ""
echo "****************"
echo "****Expected****"
echo "****************"
./scripts/tdb/tdbout -aX -Q 'plugin ~ "highwire"' tdb/{prod,clockssingest}/*.tdb | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"

#Test AUs that are exists for CLOCKSS & GLN
echo ""
echo "**************"
echo "****Exists****"
echo "**************"
./scripts/tdb/tdbout -aE -Q 'plugin ~ "highwire"' tdb/{prod,clockssingest}/*.tdb | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"

#Test recent AUs that are manifest for for CLOCKSS & GLN
echo ""
echo "****************"
echo "****Manifest****"
echo "****************"
./scripts/tdb/tdbout -aM -Q 'plugin ~ "highwire" and year ~ "^20"' tdb/{prod,clockssingest}/*.tdb | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"

#Test recent AUs that are released (GLN) or crawling (CLOCKSS). Ignore http.
echo ""
echo "**********************"
echo "****Released (GLN)****"
echo "**********************"
./scripts/tdb/tdbout -aR -Q 'plugin ~ "highwire" and year ~ "^20"' tdb/prod/*.tdb | grep https | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"
echo ""
echo "**************************"
echo "****Crawling (CLOCKSS)****"
echo "**************************"
./scripts/tdb/tdbout -aC -Q 'plugin ~ "highwire" and year ~ "^20"' tdb/clockssingest/*.tdb | grep https | ./scripts/tdb/read_auid_new.pl | grep -v "*MANIFEST"

exit 0

