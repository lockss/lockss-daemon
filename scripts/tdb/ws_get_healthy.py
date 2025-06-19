#!/usr/bin/env python3

# This script queries for auids,date of last successful crawl, 
# in order to release to the gln.
# crontab:
# ./scripts/tdb/ws_get_healthy.py ingest1.clockss.org:8081 $iun $ipw | sort > $t/gr_ingest_healthy.txt #see gln_ready.sh

import sys
from datetime import datetime
from zeep import Client, Plugin
from zeep.exceptions import Fault

if len(sys.argv[1:]) != 3:
  print('Usage: %s host:uiport uiuser uipass' % (sys.argv[0],))
  sys.exit(1)

hostport, uiuser, uipass = sys.argv[1:]

url = 'http://%s/ws/DaemonStatusService?wsdl' % (hostport,)
client = Client(url)

# Inject authentication into Zeep
factory = client._default_soapheaders._factory()
security = factory('Security', {
    'UsernameToken': {
        'Username': uiuser,
        'Password': uipass
    }
})
client.set_soapheaders([security])

# Define the query
# Find AUids for the GLN which have substance, the most recent crawl is successful, and the year of publication starts with "20"
query = 'select auId where substanceState = "Yes" and lastCrawlResult = "Successful" and tdbYear like "20%"'
# All the items to select
#query = 'select auId,lastCrawl,contentSize,lastCrawl,availableFromPublisher where substanceState = "Yes" and lastCrawlResult = "Successful" and auId like "%Taylor%"'

# Perform the query
try:
    results = client.service.queryAus(query)
    for result in results:
        print(result.auId)
except Fault as e:
    print(f"An error occurred: {e}")
