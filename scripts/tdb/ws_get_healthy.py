#!/usr/bin/env python2

# This script queries for auids,date of last successful crawl, 
# in order to release to the gln.
# crontab:
# ./scripts/tdb/ws_get_healthy.py ingest1.clockss.org:8081 $iun $ipw | sort > $t/gr_ingest_healthy.txt #see gln_ready.sh
from suds.client import Client
from suds.sudsobject import asdict
import sys
from datetime import datetime

if len(sys.argv[1:]) != 3:
  print 'Usage: %s host:uiport uiuser uipass' % (sys.argv[0],)
  sys.exit(1)
hostport, uiuser, uipass = sys.argv[1:]

url = 'http://%s/ws/DaemonStatusService?wsdl' % (hostport,)
client = Client(url, username=uiuser, password=uipass)
#query = 'select auId,lastCrawl,contentSize where substanceState = "Yes" and lastCrawlResult = "Successful"'
#query = 'select auId,contentSize,lastCrawl,availableFromPublisher where substanceState = "Yes" and lastCrawlResult = "Successful" and auId like "%TaylorAndFrancisPlugin%"'
#query = 'select auId where substanceState = "Yes" and lastCrawlResult = "Successful" and tdbYear like "%201%" and auId like "%Books%"'
query = 'select auId where substanceState = "Yes" and lastCrawlResult = "Successful" and tdbYear like "19%"'
#query = 'select auId where substanceState = "Yes" and lastCrawlResult = "Successful"'
#query = 'select auId,lastCrawl where substanceState = "Yes"'
results = client.service.queryAus(query)
for result in results: print result.auId
