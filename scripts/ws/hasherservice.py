#!/usr/bin/env python

'''A library and a command line tool to interact with the LOCKSS daemon hasher
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

__version__ = '0.2.5'

import HasherServiceImplService_client

def hash_au(host, auth, auid):
  '''Returns the full hash of the given AU
  '''
  req = HasherServiceImplService_client.hash()
  req._hasherParams = HasherServiceImplService_client.hasherParams
  req._hasherParams._auId = auid
  return _ws_port(host, auth).hash(req)._return

def hashAu(host, auth, auid): return hash_au(host, auth, auid)

def hash_au_url(host, auth, auid, url):
  '''Returns the filtered file of the given Url and AU
  '''
  req = HasherServiceImplService_client.hash()
  req._hasherParams = HasherServiceImplService_client.hasherParams
  req._hasherParams._auId = auid
  req._hasherParams._url = url
  req._hasherParams._hashType = "V3File"
  req._hasherParams._recordFilteredStream = "True"
  return _ws_port(host, auth, sys.stdout).hash(req)._return

def hashAuUrl(host, auth, auid, url): return hash_au_url(host, auth, auid, url)

def hash_asynchronously_au(host, auth, auid):
  '''Returns a request id for a asychrounous hash of the given AU
  '''
  req = HasherServiceImplService_client.hashAsynchronously()
  req._hasherParams = HasherServiceImplService_client.hasherParams
  req._hasherParams._auId = auid
  return _ws_port(host, auth).hashAsynchronously(req)._return._requestId

def hashAsynchronouslyAu(host, auth, auid): return hash_asynchronously_au(host, auth, auid)

def hash_asynchronously_au_url(host, auth, auid, url):
  '''Returns a request id for a asychrounous hash of the given url
  '''
  req = HasherServiceImplService_client.hashAsynchronously()
  req._hasherParams = HasherServiceImplService_client.hasherParams
  req._hasherParams._auId = auid
  req._hasherParams._url = url
  req._hasherParams._hashType = "V3File"
  req._hasherParams._recordFilteredStream = True
  return _ws_port(host, auth).hashAsynchronously(req)._return._requestId

def hashAsynchronouslyAuUrl(host, auth, auid, url): return hash_asynchronously_au_url(host, auth, auid, url)

def get_asynchronous_hash_result(host, auth, request_id):
  '''Returns a hash result for the hash associated with given request_id
  '''
  req = HasherServiceImplService_client.getAsynchronousHashResult()
  req._requestId = request_id
  return _ws_port(host, auth).getAsynchronousHashResult(req)._return

def getAsynchronousHashResult(host, auth, request_id): return get_asynchronous_hash_result(host, auth, request_id)

def remove_asynchronous_hash_request(host, auth, request_id):
  '''Removes the hash associated with given request_id
  '''
  req = HasherServiceImplService_client.removeAsynchronousHashRequest()
  req._requestId = request_id
  return _ws_port(host, auth).removeAsynchronousHashRequest(req)._return

def removeAsynchronousHashRequest(host, auth, request_id): return remove_asynchronous_hash_request(host, auth, request_id)

def _ws_port(host, auth, tracefile=None):
  url = 'http://%s/ws/HasherService' % (host,)
  locator = HasherServiceImplService_client.HasherServiceImplServiceLocator()
  if tracefile is None: return locator.getHasherServiceImplPort(url=url, auth=auth)
  else: return locator.getHasherServiceImplPort(url=url, auth=auth, tracefile=tracefile)

