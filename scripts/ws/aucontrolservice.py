#!/usr/bin/env python

'''A library and a command line tool to interact with the LOCKSS daemon AU control
service via its Web Services API.'''

# $Id$

__copyright__ = '''\
Copyright (host, auth, c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.
'''

__license__ = '''\
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (host, auth, the "Software"), to deal
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

__version__ = '0.1'

import getpass
from multiprocessing.dummy import Pool as ThreadPool
import optparse
import os.path
import sys
import time
from threading import Thread

import AuControlServiceImplService_client
from wsutil import zsiauth

def request_deep_crawl_by_id(host, auth, auid, refetch_depth, priority, force):
  req = AuControlServiceImplService_client.requestDeepCrawlById()
  req.AuId = auid
  req.RefetchDepth = refetch_depth
  req.Priority = priority
  req.Force = force
  return _ws_port(host, auth).requestDeepCrawlById(req).Return

def request_deep_crawl_by_id_list(host, auth, auids, refetch_depth, priority, force):
  req = AuControlServiceImplService_client.requestDeepCrawlByIdList()
  req.AuIds = auids
  req.RefetchDepth = refetch_depth
  req.Priority = priority
  req.Force = force
  return _ws_port(host, auth).requestDeepCrawlByIdList(req).Return

def _ws_port(host, auth, tracefile=None):
  '''
  Internal convenience method used to set up a Web Services Port.

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - tracefile (file object): an optional trace file (default None for no trace)
  '''
  url = 'http://%s/ws/AuControlService' % (host,)
  locator = AuControlServiceImplService_client.AuControlServiceImplServiceLocator()
  if tracefile is None: return locator.getAuControlServiceImplPort(url=url, auth=auth)
  else: return locator.getAuControlServiceImplPort(url=url, auth=auth, tracefile=tracefile)


