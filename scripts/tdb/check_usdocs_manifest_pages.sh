#! /bin/bash
# Script that creates a list of auids that are ready to be pushed to the gln, based on clockss test results
#

tpath="/home/$LOGNAME/tmp"
#mkdir -p $tpath

tdbout -Dc param[collection_id],param[year] tdb/usdocspln/united_states_government_printing_office.fdsys.tdb > $tpath/list1a
tdbout -EMTYRc param[collection_id],param[year] tdb/usdocspln/united_states_government_printing_office.tdb >> $tpath/list1a
tdbout -EMTYRc param[collection_id],param[year] tdb/usdocspln/united_states_government_printing_office.tdb | sort > $tpath/list1d
cat $tpath/list1a | sort  > $tpath/list1b
cat $tpath/list1b | uniq > $tpath/list1c
echo "Duplicate AUs in previous and new tdb files."
comm -23 $tpath/list1b $tpath/list1c
perl scripts/tdb/check_usdocs_manifest_pages.pl | sort | uniq > $tpath/list2a
echo "AUs missing from the tdb files."
comm -13 $tpath/list1c $tpath/list2a
echo "Extra AUs in the tdb files."
comm -23 $tpath/list1d $tpath/list2a
exit 0

