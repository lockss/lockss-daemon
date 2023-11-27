#! /bin/bash
#Usage 
#Have a list of AUs in a tmp file ../SageEdits/tmp
#This script will change the AU status to releasing, 
#  create a list of publisher+plugin, 
#  create a list of atypon publishers, 
#  generate a spreadsheet of AUs being released, 
#  generate the email for the release.
#After
#Review and confirm all outputs,
#commit the changes, change "releasing" to "released" and commit again.

########GLN
echo "****Moving AUs to releasing status.****"
./scripts/tdb/tdbedit --from-status=ready,releasing --to-status=releasing --auids=../SageEdits/tmp tdb/prod/{,*/}*.tdb
echo "****Find AUs from the same publisher but different plugins (optional)****"
./scripts/tdb/tdbout -G -t publisher,plugin tdb/prod/ | awk -F"\t" 'BEGIN {OFS="\t"} {foo[$0]++} END {for (x in foo) {print x,foo[x]}}' | sort
echo "****List of the Atypon publishers****"
./scripts/tdb/tdbout -G -t publisher -Q 'plugin ~ "typon"' tdb/prod/ | sort -u ; tdbout -G -t publisher -Q 'plugin ~ "TaylorAndFrancisPlugin"' tdb/prod/taylor_and_francis.tdb | uniq ; tdbout -G -t publisher -Q 'plugin ~ "EdinburghUniversityPressP"' tdb/prod/edinburgh_university_press.tdb | uniq
echo "****Generating list of the released AUs****"
echo -e "Publisher\tJournal\tISSN\tEISSN\tVolume\tYear" > ../SageEdits/ReleasedToday.tsv ; scripts/tdb/tdbout -G -t publisher,title,issn,eissn,name,year tdb/prod/ >> ../SageEdits/ReleasedToday.tsv
echo "****Generating release email"
./scripts/tdb/tdbout -t publisher,title,name,status,plugin tdb/prod/ > ../SageEdits/ForEmail.txt ; cat ../SageEdits/ForEmail.txt | ./scripts/tdb/releaseemail.py --gln > ../SageEdits/ReleaseEmail.txt
 
exit 0

