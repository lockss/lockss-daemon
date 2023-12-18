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
./scripts/tdb/tdbedit --from-status=ready,releasing --to-status=releasing --auids=../SageEdits/tmp tdb/usdocspln/*.tdb
echo "****Generating list of the released AUs****"
echo -e "Publisher\tJournal\tVolume\tYear" > ../SageEdits/ReleasedTodayGPO.tsv ; scripts/tdb/tdbout -G -t publisher,title,name,year tdb/usdocspln/ >> ../SageEdits/ReleasedTodayGPO.tsv
echo "****Generating release email"
./scripts/tdb/tdbout -t publisher,title,name,status,plugin tdb/usdocspln/ > ../SageEdits/ForEmailGPO.txt ; cat ../SageEdits/ForEmailGPO.txt | ./scripts/tdb/releaseemail.py --pln > ../SageEdits/ReleaseEmailGPO.txt

exit 0

