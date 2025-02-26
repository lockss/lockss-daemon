#! /bin/bash
# Script that creates a list of AUs by collection_id and year, that are missing from the USDocs tdb files.
#

tpath="/home/$LOGNAME/tmp"
#mkdir -p $tpath

#list of Down AUs in old tdb file.
tdbout -Dc param[collection_id],param[year] tdb/usdocspln/united_states_government_publishing_office.fdsys.tdb | sort > $tpath/list1a 
#list of pending and released AUs in new tdb file.
tdbout -EMTYRDc param[collection_id],param[year] tdb/usdocspln/united_states_government_publishing_office.tdb | sort > $tpath/list1b 
#list of pending and released AUs in COURTS tdb file.
tdbout -c param[court_id],param[year] tdb/usdocspln/united_states_government_publishing_office.uscourts.tdb | sed 's/^/USCOURTS_/' | sort > $tpath/list1e 

cat $tpath/list1a $tpath/list1b $tpath/list1e | sort  > $tpath/list1c #combined list sorted
cat $tpath/list1c | uniq > $tpath/list1d #combined list sorted and uniqed

#output. AUs duplicated in old and new tdb files. reprocessed.
echo "**Duplicate AUs in previous and new tdb files."
comm -23 $tpath/list1c $tpath/list1d #output. duplicate aus in new and old tdb files?

#output. AUs missing from the new tdb file.
echo "**AUs missing from the combined tdb files. Add to the new tdb file."
perl scripts/tdb/check_usdocs_manifest_pages.pl | sort | uniq > $tpath/list2a #list of AUs on website
#NOT USCOURTS
comm -13 $tpath/list1d $tpath/list2a | grep -v 'USCOURTS_' | sed 's/^/    au < manifest ; /' | sed 's/\([A-Z]*\),\([12][67890][0-9][0-9]\)/\2 ; \1 \2 ; \2 ; https:\/\/www.govinfo.gov\/ >/'
#USCOURTS
comm -13 $tpath/list1d $tpath/list2a | grep 'USCOURTS_' | grep -v 'USCOURTS_.*,200' | sed 's/^/    au < manifest ; /' | sed 's/\USCOURTS_\([a-zA-Z0-9]*\),\([12][67890][0-9][0-9]\)/\2 ; USCOURTS_\1 ; USCOURTS_\1 \2 ; USCOURTS ; \1 ; \2 >/'

#output. AUs in the new tdb file, which are not on the website
echo "**Extra AUs in the new tdb file."
comm -23 $tpath/list1b $tpath/list2a 

#echo "AUs missing from the old tdb file."
#comm -13 $tpath/list1a $tpath/list2a #output. AUs missing from the old tdb file.

exit 0

#Sitemap: https://www.govinfo.gov/sitemap/BILLS_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/BUDGET_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CCAL_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CDIR_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CDOC_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CFR_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CHRG_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CMR_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/COMPS_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CPRT_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CRECB_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CREC_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CRI_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CRPT_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/CZIC_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/DCPD_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/ECONI_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/ERIC_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/ERP_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/FR_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/GAOREPORTS_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/GOVMAN_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/GOVPUB_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/GPO_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/HJOURNAL_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/HMAN_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/HOB_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/LSA_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/PAI_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/PLAW_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/PPP_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/SERIALSET_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/SMAN_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/STATUTE_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/USCODE_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/USCOURTS_sitemap_index.xml
#Sitemap: https://www.govinfo.gov/sitemap/WCPD_sitemap_index.xml

