#! /bin/bash
#
#Run tdbxml to check for tdb file errors.
#Normally run from the head of the lockss-daemon. lvalidate
tpath="/home/$LOGNAME/tmp"
rm $tpath/valerr
svn update > $tpath/valfiles 2>&1
for d in tdb/* ; do if [ `basename $d` = "CVS" ]; then continue ; fi ; ./scripts/tdb/tdbxml --keep-going -o /dev/null $d/*.tdb 2>> $tpath/valerr ; done
#cat tdb/*/*.tdb | ./scripts/tdb/tdbxml -o /dev/null 2> $tpath/valerr
if [ -s $tpath/valerr ]
then
cat $tpath/valfiles $tpath/valerr | mailx -s "TDB Validation Failed" one_email_address@lockss.org
#else
#cat $tpath/valfiles $tpath/valerr | mailx -s "Validation Passes" another_email_address@lockss.org
fi
exit 0
