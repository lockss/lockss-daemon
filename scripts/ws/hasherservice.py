#!/usr/bin/env python

'''A library and a command line tool to interact with the LOCKSS daemon hasher
service via its Web Services API.'''

# $Id$

__copyright__ = '''\
Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

__version__ = '0.4.1'

import getpass
from multiprocessing.dummy import Pool as ThreadPool
import optparse
import os.path
import sys
import time
from threading import Thread

import HasherServiceImplService_client
from wsutil import zsiauth

def hash_au(host, auth, auid):
  '''Returns the full hash of the given AU
  '''
  req = HasherServiceImplService_client.hash()
  req.HasherParams = req.new_hasherParams()
  req.HasherParams.AuId = auid
  return _ws_port(host, auth).hash(req).Return

def hash_au_url(host, auth, auid, url):
  '''Returns the filtered file of the given Url and AU
  '''
  req = HasherServiceImplService_client.hash()
  req.HasherParams = req.new_hasherParams()
  req.HasherParams = HasherServiceImplService_client.hasherParams
  req.HasherParams.AuId = auid
  req.HasherParams.Url = url
  req.HasherParams.HashType = "V3File"
  req.HasherParams.RecordFilteredStream = "True"
  return _ws_port(host, auth, sys.stdout).hash(req).Return

def hash_asynchronously_au(host, auth, auid):
  '''Returns a request id for a asychrounous hash of the given AU
  '''
  req = HasherServiceImplService_client.hashAsynchronously()
  req.HasherParams = req.new_hasherParams()
  req.HasherParams.AuId = auid
  try: return _ws_port(host, auth).hashAsynchronously(req).Return.RequestId
  except AttributeError: return None

def hash_asynchronously_au_url(host, auth, auid, url):
  '''Returns a request id for a asychrounous hash of the given url
  '''
  req = HasherServiceImplService_client.hashAsynchronously()
  req.HasherParams = req.new_hasherParams()
  req.HasherParams.AuId = auid
  req.HasherParams.Url = url
  req.HasherParams.HashType = "V3File"
  req.HasherParams.RecordFilteredStream = True
  try: return _ws_port(host, auth).hashAsynchronously(req).Return.RequestId
  except AttributeError: return None

def get_asynchronous_hash_result(host, auth, request_id):
  '''Returns a hash result for the hash associated with given request_id
  '''
  req = HasherServiceImplService_client.getAsynchronousHashResult()
  req.RequestId = request_id
  return _ws_port(host, auth).getAsynchronousHashResult(req).Return

def remove_asynchronous_hash_request(host, auth, request_id):
  '''Removes the hash associated with given request_id
  '''
  req = HasherServiceImplService_client.removeAsynchronousHashRequest()
  req.RequestId = request_id
  return _ws_port(host, auth).removeAsynchronousHashRequest(req).Return

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
    group.add_option('--url', help='target URL (optional)')
    parser.add_option_group(group)
    # Output
    group = optparse.OptionGroup(parser, 'Output')
    group.add_option('--output-directory', metavar='OUTDIR', default='.', help='output directory (default: current directory)')
    group.add_option('--output-prefix', metavar='PREFIX', default='hasherservice', help='prefix for output file names (default: "hasherservice")')
    parser.add_option_group(group)
    # Other options
    group = optparse.OptionGroup(parser, 'Other options')
    group.add_option('--long-html-line', action='store_true', help='add a newline before each "<" character')
    group.add_option('--long-text-line', action='store_true', help='replace each space with a newline')
    group.add_option('--threads', type='int', help='maximum number of parallel jobs allowed (default: no limit)')
    group.add_option('--wait', type='int', help='seconds to wait between asynchronous checks (default: 10 with --url, 30 without)')
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
    self.output_directory = os.path.expanduser(opts.output_directory)
    if not os.path.isdir(self.output_directory):
      parser.error('no such directory: %s' % (self.output_directory,))
    if opts.output_prefix is None: parser.error('--output-prefix is required')
    if '/' in opts.output_prefix: parser.error('output prefix cannot contain a slash')
    self.output_prefix = opts.output_prefix
    # long_html_line/long_text_line/wait/threads
    if any([opts.long_html_line, opts.long_text_line]) and self.url is None:
      parser.error('--long-html-line, --long-text-line only apply to --url')
    if opts.long_html_line and opts.long_text_line:
      parser.error('--long-html-line, --long-text-line are incompatible')
    self.long_html_line = opts.long_html_line
    self.long_text_line = opts.long_text_line
    if opts.wait is None: self.wait = 30 if self.url is None else 10
    else: self.wait = opts.wait
    # threads
    self.threads = opts.threads or len(self.hosts)
    # auth
    u = opts.username or getpass.getpass('UI username: ')
    p = opts.password or getpass.getpass('UI password: ')
    self.auth = zsiauth(u, p)

def _do_hash(options, host):
  if options.url is None: reqid = hash_asynchronously_au(host, options.auth, options.auid)
  else: reqid = hash_asynchronously_au_url(host, options.auth, options.auid, options.url)
  if reqid is None: return host, False
  while True:
    time.sleep(options.wait)
    res = get_asynchronous_hash_result(host, options.auth, reqid)
    if res._status == 'Done': break
  if options.url is None:
    source = res._blockFileDataHandler
    fstr = '%s.%s.hash' % (options.output_prefix, host)
  else:
    source = res._recordFileDataHandler
    fstr = '%s.%s.filtered' % (options.output_prefix, host)
  if source is not None:
    lines = [line for line in source]
    if options.long_html_line: lines = map(lambda s: s.replace('<', '\n<'), lines)
    if options.long_text_line: lines = map(lambda s: s.replace(' ', '\n'), lines)
    with open(os.path.join(options.output_directory, fstr), 'w') as f:
      f.writelines(lines)
  res = remove_asynchronous_hash_request(host, options.auth, reqid)
  return host, source is not None

def _do_hashes(options):
  for host, result in ThreadPool(options.threads).imap_unordered( \
      lambda _host: _do_hash(options, _host), \
      options.hosts):
    if result is False:
      sys.stderr.write('Warning: not found on %s\n' % (host,))

# Last modified 2015-08-31
def _file_lines(fstr):
  with open(os.path.expanduser(fstr)) as f: ret = filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f])
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
  return ret

def _main():
  '''Main method.'''
  parser = _HasherServiceOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _HasherServiceOptions(parser, opts, args)
  t = Thread(target=_do_hashes, args=(options,))
  t.daemon = True
  t.start()
  while True:
    t.join(1.5)
    if not t.is_alive(): break

# Main entry point
if __name__ == '__main__': _main()

