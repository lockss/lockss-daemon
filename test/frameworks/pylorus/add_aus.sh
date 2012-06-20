#!/bin/bash
#
# Simple wrapper script to send notification e-mail when process is complete.
# Add this variable to .profile with your email address
# export PYLORUS_MAIL="myemail@stanford.edu"

for i in ingest1 ingest2 ingest3 ingest4
do
$(dirname "$0")/../bulk_add/bin/bulk_add $i.clockss.org 8081 $*
# Use sleep command for debug.
#sleep 5
done

# Use mailx to send a notification.
if [ "x$PYLORUS_MAIL" != "x" ]
then 
echo "$USER: Job: $* on $HOSTNAME at $PWD" | mailx -s "Job Complete" ${PYLORUS_MAIL}
fi
exit 0

