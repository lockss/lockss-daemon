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

__version__ = '0.3.0'

import getpass
import optparse
import os.path
import sys
from time import sleep

import HasherServiceImplService_client
from wsutil import zsiauth

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

#
# Command line tool
#

class _HasherServiceOptions(object):

  @staticmethod
  def make_parser():
    usage = '%prog [--host=HOST|--hosts=HFILE]... --auid=AUID [--url=URL] [--output-directory=OUTDIR] --output-prefix=PREFIX [OPTIONS]'
    parser = optparse.OptionParser(version=__version__, description=__doc__, usage=usage)
    # Hosts
    group = optparse.OptionGroup(parser, 'Hosts')
    group.add_option('--host', action='append', default=list(), help='add host:port pair to list of target hosts')
    group.add_option('--hosts', action='append', default=list(), metavar='HFILE', help='add host:port pairs in HFILE to list of target hosts')
    group.add_option('--password', metavar='PASS', help='UI password (default: interactive prompt)')
    group.add_option('--username', metavar='USER', help='UI username (default: interactive prompt)')
    parser.add_option_group(group)
    # AUID and URL
    group = optparse.OptionGroup(parser, 'AUID and URL')
    group.add_option('--auid', help='target AUID')
    group.add_option('--url', help='target URL')
    parser.add_option_group(group)
    # Output
    group = optparse.OptionGroup(parser, 'Output')
    group.add_option('--output-directory', default='.', metavar='OUTDIR', help='output directory (default: current directory)')
    group.add_option('--output-prefix', metavar='PREFIX', help='prefix for output file names')
    parser.add_option_group(group)
    # Other options
    group = optparse.OptionGroup(parser, 'Other options')
    group.add_option('--wait', type=int, default=30, help='seconds to wait between asynchronous checks')
    parser.add_option_group(group)
    return parser

  def __init__(self, parser, opts, args):
    super(_HasherServiceOptions, self).__init__()
    if len(args) != 0: parser.error('extraneous arguments: %s' % (' '.join(args)))
    # hosts
    self.hosts = opts.host[:]
    for f in opts.hosts: self.hosts.extend(_file_lines(f))
    if len(self.hosts) == 0: parser.error('at least one target host is required')
    # auid/url
    self.auid = opts.auid
    self.url = opts.url
    # output_directory/output_prefix
    if not os.path.isdir(opts.output_directory):
      parser.error('no such directory: %s' % (opts.output_directory,))
    self.output_directory = opts.output_directory
    if opts.output_prefix is None: parser.error('--output-prefix is required')
    if '/' in opts.output_prefix: parser.error('output prefix cannot contain a slash')
    self.output_prefix = opts.output_prefix
    # wait
    self.wait = opts.wait
    # auth
    u = opts.username or getpass.getpass('UI username: ')
    p = opts.password or getpass.getpass('UI password: ')
    self.auth = zsiauth(u, p)

def _do_hashes(options):
  wholeau = options.url is None
  reqids = dict()
  for host in options.hosts:
    if wholeau: reqids[host] = hash_asynchronously_au(host, options.auth, options.auid)
    else: reqids[host] = hash_asynchronously_au_url(host, options.auth, options.auid, options.url)
  while len(reqids) > 0:
    sleep(options.wait)
    finished = list()
    for host, reqid in reqids.iteritems():
      res = get_asynchronous_hash_result(host, options.auth, reqid)
      if res._status == 'Done':
        if wholeau:
          with open(os.path.join(options.output_directory, '%s.%s.hash' % (options.output_prefix, host)), 'w') as f:
            f.write(res._blockFileDataHandler)
        else:
          with open(os.path.join(options.output_directory, '%s.%s.filtered' % (options.output_prefix, host)), 'w') as f:
            f.write(res._recordFileDataHandler)
        remove_asynchronous_hash_request(host, options.auth, reqid)
        finished.append(host)
    for host in finished: del reqids[host]

# Last modified 2015-08-10
def _file_lines(fstr):
  with open(fstr) as f: ret = filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f])
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
  return ret

def _main():
  '''Main method.'''
  parser = _HasherServiceOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _HasherServiceOptions(parser, opts, args)
  _do_hashes(options)

# Main entry point
if __name__ == '__main__': _main()

