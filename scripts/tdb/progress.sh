#! /bin/bash
#
#One day / one line snapshot of tdb status for GLN AUs
#echo "date,doesNotExist,expected,exists,manifest,doNotProcess,testing,notReady,ready,released,down,superseded,other" > ./progress_out.txt
tpath="/home/$LOGNAME/tmp"
./scripts/tdb/tdbout -c status tdb/prod/*.tdb > $tpath/allstatus
countother=`cat $tpath/allstatus | wc -l`
OutputX=`date +%F`
for j in doesNotExist expected exists manifest doNotProcess testing notReady ready released down superseded
do
count=`grep $j $tpath/allstatus | wc -l`
countother=$(($countother - $count))
OutputX=$OutputX,$count
#echo $OutputX
done
OutputX=$OutputX,$countother
echo $OutputX >> ./scripts/tdb/progress_out.txt
cat ./scripts/tdb/progress_out.txt
exit 0

