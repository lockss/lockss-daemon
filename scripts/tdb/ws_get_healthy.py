#!/usr/bin/env python3

# This script queries for auids, date of last successful crawl,
# in order to release to the gln.
# crontab:
# ./scripts/tdb/ws_get_healthy.py ingest1.clockss.org:8081 $iun $ipw | sort > $t/gr_ingest_healthy.txt #see gln_ready.sh

import sys
import signal
import os
from requests.auth import HTTPBasicAuth
import requests
from zeep import Client, Transport

if len(sys.argv[1:]) != 3:
    print('Usage: %s host:uiport uiuser uipass' % (sys.argv[0],))
    sys.exit(1)

hostport, uiuser, uipass = sys.argv[1:]

# Setup the URL for the WSDL
url = 'http://%s/ws/DaemonStatusService?wsdl' % (hostport,)

# Setup HTTP Basic Authentication
session = requests.Session()
session.auth = HTTPBasicAuth(uiuser, uipass)

# Add more debugging options to the session
#session.headers.update({'Content-Type': 'application/soap+xml'})
#session.verify = False  # Disable SSL verification if necessary (use carefully)

# Log session details for debugging
#print('Session headers:', session.headers)
#print('Session auth:', session.auth)

# Create a transport with the authenticated session
transport = Transport(session=session)

# Initialize the zeep client with the transport
client = Client(url, transport=transport)

# Define the query
query = 'select auId where substanceState = "Yes" and lastCrawlResult = "Successful" and tdbYear like "20%"'

# Function to handle broken pipe error
def handle_broken_pipe(signum, frame):
    sys.stdout = open(os.devnull, 'w')
    sys.exit(1)

# Set the signal handler for SIGPIPE
signal.signal(signal.SIGPIPE, handle_broken_pipe)

# Perform the query
try:
    results = client.service.queryAus(query)
    for result in results:
        print(result.auId)
except Exception as e:
    print(f"An error occurred: {e}")
