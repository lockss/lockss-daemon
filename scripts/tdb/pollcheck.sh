#! /bin/bash
# $Id$
#
#Checks ingest machines to evaluate how busy they are.
echo "Use the ingest machines with the lowest number of polls in progress."
export http_proxy=http://lockss.org:8888
export no_proxy=localhost,127.0.0.1

for i in ingest1 ingest2 ingest3 ingest4 ingest5
do
   echo "$i:"
   wget -o /dev/null -O - --tries=1 --timeout=4 --http-user=lockss-u --http-password=lockss-p http://${i}.clockss.org:8081/DaemonStatus?table=V3PollerTable 2>&1 | grep -A 1 "<b>Polls</b>:" | tail -1 | sed s/.....// | sed s/....$// | perl -ne 'print $_; $t=0; if (m/(\d+) started, (\d+) Complete/) {$t=$1-$2; if (m/(\d+) No Quorum/) {$t=$t-$1}; if (m/(\d+) Error/) {$t=$t-$1}; print "Polls in progress: $t\n"}'
done

exit 0





