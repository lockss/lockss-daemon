#!/usr/bin/env python

'''A library and a command line tool to interact with the LOCKSS daemon status
service via its Web Services API.'''

# $Id$

__copyright__ = '''\
Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.
'''

__license__ = '''\
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.
'''

__version__ = '0.2.4'

import HasherServiceImplService_client
import HasherServiceImplService_types
from ZSI.auth import AUTH

from datetime import date, datetime
import getpass
import optparse
import sys

#
# External
#

def auth(u, p):
  '''Makes a ZSI authentication object suitable for the methods in this module.
  Parameters:
  - u (string): a UI username
  - p (string): a UI password
  '''
  return (AUTH.httpbasic, u, p)

def datetimems(ms):
  '''Returns a datetime instance from a date and time expressed in milliseconds
  since epoch (or None if the input is None).
  Parameters:
  - ms (numeric): a number of milliseconds since epoch
  '''
  if ms is None: return None
  return datetime.fromtimestamp(ms / 1000)

def datems(ms):
  '''Returns a date instance from a date and time expressed in milliseconds
  since epoch (or None if the input is None).
  Parameters:
  - ms (numeric): a number of milliseconds since epoch
  '''
  if ms is None: return None
  return date.fromtimestamp(ms / 1000)

def durationms(ms):
  '''Returns an approximate text representation of the number of milliseconds
  given. The result is of one of the following forms:
  - 123ms (milliseconds)
  - 12s (seconds)
  - 12m34s (minutes and seconds)
  - 12h34m56s (hours, minutes and seconds)
  - 1d23h45m (days, hours and minutes)
  - 4w3d21h (weeks, days and hours)
  Parameters:
  ms (numeric): a number of milliseconds
  '''
  s, ms = divmod(ms, 1000)
  if s == 0: return '%dms' % (ms,)
  m, s = divmod(s, 60)
  if m == 0: return '%ds' % (s,)
  h, m = divmod(m, 60)
  if h == 0: return '%dm%ds' % (m, s)
  d, h = divmod(h, 24)
  if d == 0: return '%dh%dm%ds' % (h, m, s)
  w, d = divmod(d, 7)
  if w == 0: return '%dd%dh%dm' % (d, h, m)
  return '%dw%dd%dh' % (w, d, h)

def hashAu(host, auth, auid):
  '''Returns the full hash of the given AU
  '''
  req = HasherServiceImplService_client.hash()
  req._hasherParams = HasherServiceImplService_client.hasherParams
  req._hasherParams._auId = auid
  return _ws_port(host, auth).hash(req)._return

def hashAuUrl(host, auth, auid, url):
  '''Returns the filtered file of the given Url and AU
  '''
  req = HasherServiceImplService_client.hash()
  req._hasherParams = HasherServiceImplService_client.hasherParams
  req._hasherParams._auId = auid
  req._hasherParams._url = url
  req._hasherParams._hashType = "V3File"
  req._hasherParams._recordFilteredStream = "True"
  
  return _ws_port(host, auth, sys.stdout).hash(req)._return

def hashAsynchronouslyAu(host, auth, auid):
  '''Returns a request id for a asychrounous hash of the given AU
  '''
  req = HasherServiceImplService_client.hashAsynchronously()
  req._hasherParams = HasherServiceImplService_client.hasherParams
  req._hasherParams._auId = auid
  return _ws_port(host, auth).hashAsynchronously(req)._return._requestId

def hashAsynchronouslyAuUrl(host, auth, auid, url):
  '''Returns a request id for a asychrounous hash of the given url
  '''
  req = HasherServiceImplService_client.hashAsynchronously()
  req._hasherParams = HasherServiceImplService_client.hasherParams
  req._hasherParams._auId = auid
  req._hasherParams._url = url
  req._hasherParams._hashType = "V3File"
  req._hasherParams._recordFilteredStream = True
  return _ws_port(host, auth).hashAsynchronously(req)._return._requestId

def getAsynchronousHashResult(host, auth, request_id):
  '''Returns a hash result for the hash associated with given request_id
  '''
  req = HasherServiceImplService_client.getAsynchronousHashResult()
  req._requestId = request_id
  return _ws_port(host, auth).getAsynchronousHashResult(req)._return
    
def removeAsynchronousHashRequest(host, auth, request_id):
  '''Removes the hash associated with given request_id
  '''
  req = HasherServiceImplService_client.removeAsynchronousHashRequest()
  req._requestId = request_id
  return _ws_port(host, auth).removeAsynchronousHashRequest(req)._return
#
# Internal
#

def _ws_port(host, auth, tracefile=None):
  url = 'http://%s/ws/HasherService' % (host,)
  locator = HasherServiceImplService_client.HasherServiceImplServiceLocator()
  if tracefile is None: return locator.getHasherServiceImplPort(url=url, auth=auth)
  else: return locator.getHasherServiceImplPort(url=url, auth=auth, tracefile=tracefile)

