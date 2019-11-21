#!/usr/bin/env python2

'''A library and a command line tool to interact with the LOCKSS daemon export
service via its Web Services API.'''

__copyright__ = '''\
Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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

__version__ = '0.1'

import getpass
import itertools
from multiprocessing.dummy import Pool as ThreadPool
import optparse
import os.path
import sys
from threading import Thread

try: import ZSI
except ImportError: sys.exit('The Python ZSI module must be installed (or on the PYTHONPATH)')

import ExportServiceImplService_client
from wsutil import datems, datetimems, durationms, zsiauth

#
# Library
#

def create_export_files(host, auth, auid):
  '''Performs a createExportFiles operation on the given host for the given AUID, and
  returns a record with the files.
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  req = ExportServiceImplService_client.createExportFiles()
  req.Arg0 = req.new_arg0()
  req.Arg0.Auid = auid
  req.Arg0.Compress = True
  req.Arg0.ExcludeDirNodes = True
  #req.Arg0.FilePrefix = "SomePrefix"
  #req.Arg0.FileType = "ZIP"
  req.Arg0.MaxSize = 1000
  req.Arg0.MaxVersions = -1
  #req.Arg0.XlateFilenames = "None"
  try:
    ret = _ws_port(host, auth).createExportFiles(req)
    return ret.Return
  except ZSI.FaultException as e:
    if str(e).startswith('No Archival Unit with provided identifier'):
      return None
    raise

def _ws_port(host, auth, tracefile=None):
  url = 'http://%s/ws/ExportService' % (host,)
  locator = ExportServiceImplService_client.ExportServiceImplServiceLocator()
  if tracefile is None: return locator.getExportServiceImplPort(url=url, auth=auth)
  else: return locator.getExportServiceImplPort(url=url, auth=auth, tracefile=tracefile)

#
# Command line tool
#

class _ExportServiceOptions(object):

  @staticmethod
  def make_parser():
    usage = '%prog {--host=HOST|--hosts=HFILE}... [OPTIONS]'
    parser = optparse.OptionParser(version=__version__, description=__doc__, usage=usage)
    # Hosts
    group = optparse.OptionGroup(parser, 'Target hosts')
    group.add_option('--host', action='append', default=list(), help='add host:port pair to list of target hosts')
    group.add_option('--hosts', action='append', default=list(), metavar='HFILE', help='add host:port pairs in HFILE to list of target hosts')
    group.add_option('--password', metavar='PASS', help='UI password (default: interactive prompt)')
    group.add_option('--username', metavar='USER', help='UI username (default: interactive prompt)')
    parser.add_option_group(group)
    # AUIDs
    group = optparse.OptionGroup(parser, 'Target AUIDs')
    group.add_option('--auid', action='append', default=list(), help='add AUID to list of target AUIDs')
    group.add_option('--auids', action='append', default=list(), metavar='AFILE', help='add AUIDs in AFILE to list of target AUIDs')
    parser.add_option_group(group)
    # AUID operations
    group = optparse.OptionGroup(parser, 'AU operations')
    group.add_option('--create-export-files', action='store_true', help='output export files of target AUIDs')
    parser.add_option_group(group)
    # Other options
    group = optparse.OptionGroup(parser, 'Other options')
    group.add_option('--group-by-field', action='store_true', help='group results by field instead of host')
    group.add_option('--no-special-output', action='store_true', help='no special output format for a single target host')
    group.add_option('--select', metavar='FIELDS', help='comma-separated list of fields for narrower output')
    group.add_option('--threads', type='int', help='max parallel jobs allowed (default: no limit)')
    group.add_option('--where', help='optional WHERE clause for query operations')
    parser.add_option_group(group)
    return parser

  def __init__(self, parser, opts, args):
    super(_ExportServiceOptions, self).__init__()
    if len(args) > 0: parser.error('extraneous arguments: %s' % (' '.join(args)))
    if len(filter(None, [opts.create_export_files])) != 1:
      parser.error('exactly one of --create-export-files is required')
    if len(opts.auid) + len(opts.auids) > 0 and not any([opts.create_export_files]):
      parser.error('--auid, --auids can only be applied to --create-export-files')
    # hosts
    self.hosts = opts.host[:]
    for f in opts.hosts: self.hosts.extend(_file_lines(f))
    if len(self.hosts) == 0: parser.error('at least one target host is required')
    # auids
    self.auids = opts.auid[:]
    for f in opts.auids: self.auids.extend(_file_lines(f))
    # create_export_files
    self.create_export_files = opts.create_export_files
    if self.create_export_files:
      if len(self.auids) == 0: parser.error('at least one target AUID is required with --create-export-files')
      self.select = ''#self.__init_select(parser, opts, _AU_STATUS)
    # threads
    self.threads = opts.threads or len(self.hosts)
    # auth
    u = opts.username or getpass.getpass('UI username: ')
    p = opts.password or getpass.getpass('UI password: ')
    self.auth = zsiauth(u, p)

# Last modified 2018-03-19 for unicode support and boolean False when boolean is None
def _output_record(options, lst):
  print '\t'.join([x.encode('utf-8') if type(x) is unicode else str(x or False) if type(x)==type(True) else str(x or '') for x in lst])

# Last modified 2015-08-05
def _output_table(options, data, rowheaders, lstcolkeys):
  colkeys = [x for x in itertools.product(*lstcolkeys)]
  for j in xrange(len(lstcolkeys)):
    if j < len(lstcolkeys) - 1: rowpart = [''] * len(rowheaders)
    else: rowpart = rowheaders
    _output_record(options, rowpart + [x[j] for x in colkeys])
  for rowkey in sorted(set([k[0] for k in data])):
    _output_record(options, list(rowkey) + [data.get((rowkey, colkey)) for colkey in colkeys])

# Last modified 2015-08-31
def _file_lines(fstr):
  with open(os.path.expanduser(fstr)) as f: ret = filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f])
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
  return ret

_AU_STATUS = {
}

def _do_create_export_files(options):
  headlamb = [_AU_STATUS[x] for x in options.select]
  data = dict()
  for host, auid, result in ThreadPool(options.threads).imap_unordered( \
      lambda _tup: (_tup[1], _tup[0], create_export_files(_tup[1], options.auth, _tup[0])), \
      itertools.product(options.auids, options.hosts)):
    if result is not None:
      for head, lamb in headlamb:
        if options.group_by_field: colkey = (head, host)
        else: colkey = (host, head)
        data[((auid,), colkey)] = lamb(result)
  _output_table(options, data, ['AUID'], [[x[0] for x in headlamb], sorted(options.hosts)] if options.group_by_field else [sorted(options.hosts), [x[0] for x in headlamb]])

def _dispatch(options):
  if options.create_export_files: _do_create_export_files(options)
  else: raise RuntimeError, 'Unreachable'

def _main():
  '''Main method.'''
  # Parse command line
  parser = _ExportServiceOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _ExportServiceOptions(parser, opts, args)
  # Dispatch
  t = Thread(target=_dispatch, args=(options,))
  t.daemon = True
  t.start()
  while True:
    t.join(1.5)
    if not t.is_alive(): break

if __name__ == '__main__': _main()

