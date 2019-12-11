#! /bin/bash
# Script that creates a list of AUs by collection_id and year, that are missing from the USDocs tdb files.
#

tpath="/home/$LOGNAME/tmp"
#mkdir -p $tpath

tdbout -Dc param[collection_id],param[year] tdb/usdocspln/united_states_government_printing_office.fdsys.tdb | sort > $tpath/list1a #list of Down AUs in old tdb file.
tdbout -EMTYRc param[collection_id],param[year] tdb/usdocspln/united_states_government_printing_office.tdb | sort > $tpath/list1b #list of pending and released AUs in new tdb file.
cat $tpath/list1a $tpath/list1b | sort  > $tpath/list1c #combined list sorted
cat $tpath/list1c | uniq > $tpath/list1d #combined list sorted and uniqed
echo "Duplicate AUs in previous and new tdb files."
comm -23 $tpath/list1c $tpath/list1d #output. duplicate aus in new and old tdb files?
perl scripts/tdb/check_usdocs_manifest_pages.pl | sort | uniq > $tpath/list2a #list of AUs on website
echo "AUs missing from the combined tdb files. Add to the new tdb file."
comm -13 $tpath/list1d $tpath/list2a #output. AUs missing from the new tdb file.
echo "Extra AUs in the new tdb file."
comm -23 $tpath/list1b $tpath/list2a #output. AUs in the new tdb file, which are not on the website
#echo "AUs missing from the old tdb file."
#comm -13 $tpath/list1a $tpath/list2a #output. AUs missing from the old tdb file.
exit 0

