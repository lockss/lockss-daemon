#!/usr/bin/python

# This script queries for auids,date of last successful crawl, 
# in order to release to the gln.
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
#query = 'select auId,contentSize,lastCrawl,availableFromPublisher where substanceState = "Yes" and lastCrawlResult = "Successful" and auId like "%HighWirePressH20Plugin%"'
query = 'select auId where substanceState = "Yes" and lastCrawlResult = "Successful" and auId like "%HighWirePressH20Plugin%"'
#query = 'select auId,lastCrawl where substanceState = "Yes"'
results = client.service.queryAus(query)
#for result in results: 
#    print '\t'.join([result.auId, 
#                      str(datetime.fromtimestamp(result.lastCrawl/1000)),
#                      str(result.contentSize)]) #Looks like lastCrawl needs to be last or it causes an error.
#for result in results:
#     print '\t'.join([result.auId,
#                      str(result.contentSize),
#                      str(result.availableFromPublisher),
#                      str(datetime.fromtimestamp(result.lastCrawl/1000))])
for result in results: print result.auId
