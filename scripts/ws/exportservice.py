#!/usr/bin/env python3

'''A library and a command line tool to interact with the LOCKSS daemon export
service via its Web Services API.'''

__copyright__ = '''\
Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University,
all rights reserved.
'''

__license__ = '''\
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
'''

__version__ = '0.1'

import sys

try: import zeep
except ImportError: sys.exit('The Python Zeep module must be installed (or on the PYTHONPATH)')

import argparse
import getpass
import itertools
from multiprocessing.dummy import Pool as ThreadPool
import os.path
import requests.auth
from threading import Thread
import zeep.exceptions
import zeep.helpers
import zeep.transports

import logging.config

#
# Library
#

def create_export_files(host, username, password, auid, options):
  '''Performs a createExportFiles operation on the given host for the given AUID, and
  returns a record with the files.
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  Returns:
  - ret (dict):
    {
      'auId': '<auid>',
      'dataHandlerWrappers': [
        {
          'dataHandler' (base64Binary): zipped AU
          'name' (string): '<prefix>-<timestamp>-<5 digit id>.zip',
          'size' (long): size of dataHandler in bytes
        }
      ]
    }
  '''
  req = {
    'auid': auid,
    'compress': options.compress,
    'excludeDirNodes': options.exclude_dir,
    'filePrefix': options.output_prefix,
    'fileType': options.file_type,
    'maxSize': options.max_size,
    'maxVersions': options.max_vers,
    'xlateFilenames': options.translate
  }
  client = _make_client(host, username, password)
  try:
    # ret = _ws_port(host, auth).createExportFiles(req)
    # return ret.Return
    ret = client.service.createExportFiles(req)
    return zeep.helpers.serialize_object(ret)
  except zeep.exceptions.Fault as e:
    if e.message == 'No Archival Unit with provided identifier':
      return None
    else:
      raise

#
# Command line tool
#

class _ExportServiceOptions(object):

  @staticmethod
  def make_parser():
    usage = '%(prog)s {--host=HOST|--hosts=HFILE}... [OPTIONS]'
    parser = argparse.ArgumentParser(description=__doc__, usage=usage)
    parser.add_argument('--version', '-V', action='version', version=__version__)
    parser.add_argument('--debug-zeep', action='store_true', help='adds zeep debugging logging')
    # Hosts
    group = parser.add_argument_group('Target hosts')
    group.add_argument('--host', action='append', default=list(), help='add host:port pair to list of target hosts')
    group.add_argument('--hosts', action='append', default=list(), metavar='HFILE', help='add host:port pairs in HFILE to list of target hosts')
    group.add_argument('--password', metavar='PASS', help='UI password (default: interactive prompt)')
    group.add_argument('--username', metavar='USER', help='UI username (default: interactive prompt)')
    # AUIDs
    group = parser.add_argument_group('Target AUIDs')
    group.add_argument('--auid', action='append', default=list(), help='add AUID to list of target AUIDs')
    group.add_argument('--auids', action='append', default=list(), metavar='AFILE', help='add AUIDs in AFILE to list of target AUIDs')
    # AUID operations
    group = parser.add_argument_group('AU operations')
    # this seems to be redundant. leaving in in case some future functionality does make this an optional flag
    group.add_argument('--create-export-files', action='store_true', required=True, help='output export files of target AUIDs')
    # Output
    group = parser.add_argument_group('Output')
    group.add_argument('--output-directory', metavar='OUTDIR', default='.', help='output directory (default: current directory)')
    group.add_argument('--output-prefix', metavar='PREFIX', default='exportservice', help='prefix for output file names (default: %(default)s)')
    group.add_argument('--compress', action='store_false', help='compress the export files (default: True).')
    group.add_argument('--exclude-dir', action='store_false', help='exclude directory nodes from the export files (default: True).')
    group.add_argument('--file-type', default="ZIP", choices=['ZIP'], help='file type of the exported AU. (default: %(default)s)')
    group.add_argument('--max-size', default=1000, type=int, help=' (default: %(default)d)')
    group.add_argument('--max-vers', default=-1, type=int, help=' (default: %(default)d)')
    group.add_argument('--translate', default=None, choices=[None], help='translate export file filenames. (default: %(default)s)')
    # Other options
    group = parser.add_argument_group('Other options')
    group.add_argument('--group-by-field', action='store_true', default=False, help='group results by field instead of host')
    group.add_argument('--threads', type=int, help='max parallel jobs allowed (default: no limit)')
    return parser

  def __init__(self, parser, args):
    '''
    Constructor.

    Parameters:
    - parser (OptionParser instance): the option parser
    - args (list of strings): the remaining command line arguments returned by
    the parser
    '''
    super(_ExportServiceOptions, self).__init__()
    if len(args.auid) + len(args.auids) > 0 and not any([args.create_export_files]):
      parser.error('--auid, --auids can only be applied to --create-export-files')
    # hosts
    self.hosts = args.host[:]
    for f in args.hosts: self.hosts.extend(_file_lines(f))
    if len(self.hosts) == 0: parser.error('at least one target host is required')
    # auids
    self.auids = args.auid[:]
    for f in args.auids: self.auids.extend(_file_lines(f))
    # get_auids/get_auids_names/is_daemon_ready/is_daemon_ready_quiet
    if len(self.auids) == 0: parser.error('at least one target AUID is required')
    # create_export_files
    self.create_export_files = args.create_export_files
    if self.create_export_files:
      if len(self.auids) == 0: parser.error('at least one target AUID is required with --create-export-files')
    # threads
    self.threads = args.threads or len(self.hosts)
    # output_directory/output_prefix
    self.output_directory = os.path.expanduser(args.output_directory)
    if not os.path.isdir(self.output_directory):
      parser.error('no such directory: %s' % (self.output_directory,))
    if args.output_prefix is None: parser.error('--output-prefix is required')
    if '/' in args.output_prefix: parser.error('output prefix cannot contain a slash')
    self.output_prefix = args.output_prefix
    # sorting options
    self.group_by_field = args.group_by_field
    # operation options
    self.compress = args.compress
    self.exclude_dir = args.exclude_dir
    self.file_type = args.file_type
    self.max_size = args.max_size
    self.max_vers = args.max_vers
    self.translate = args.translate
    # verbosity (adds logging for zeep)
    if args.debug_zeep:
      _enable_zeep_debugging()
    # auth
    self._u = args.username or getpass.getpass('UI username: ')
    self._p = args.password or getpass.getpass('UI password: ')
    #self.auth = requests_basic_auth(self._u, self._p)

# Last modified 2018-03-19 for unicode support and boolean False when boolean is None
def _output_record(options, lst):
  print('\t'.join([str(x or '') for x in lst]))
  #print('\t'.join([x.encode('utf-8') if type(x) is str else str(x or False) if type(x)==type(True) else str(x or '') for x in lst]))

# Last modified 2021-05-28
def _output_table(options, data, rowheaders, lstcolkeys, rowsort=None):
  '''Internal method to display tabular output. (Should be refactored.)'''
  colkeys = [x for x in itertools.product(*lstcolkeys)]
  for j in range(len(lstcolkeys)):
    if j < len(lstcolkeys) - 1: rowpart = [''] * len(rowheaders)
    else: rowpart = rowheaders
    _output_record(options, rowpart + [x[j] for x in colkeys])
  for rowkey in sorted(set([k[0] for k in data]), key=rowsort):
    _output_record(options, list(rowkey) + [data.get((rowkey, colkey)) for colkey in colkeys])

# Last modified 2021-05-28
def _file_lines(fstr):
  with open(os.path.expanduser(fstr)) as f:
    ret = list(filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f]))
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
  return ret

_AU_STATUS = {
  'name': ('File name', lambda r: r.get('name')),
  'size': ('Size', lambda r: r.get('size')),
}

def _do_create_export_files(options):
  headlamb = [_AU_STATUS[x] for x in _AU_STATUS]
  data = dict()
  for host, auid, result in ThreadPool(options.threads).imap_unordered( \
      lambda _tup: (_tup[1], _tup[0],
                    create_export_files(_tup[1], options._u, options._p, _tup[0], options)), \
      itertools.product(options.auids, options.hosts)):
    if result is not None:
      if result['dataHandlerWrappers'] is not None:
        source = result['dataHandlerWrappers'][0]['dataHandler']
        fstr = result['dataHandlerWrappers'][0]['name']
        if source is not None:
          with open(os.path.join(options.output_directory, fstr), 'wb') as f:
            f.write(source)
        for head, lamb in headlamb:
          if options.group_by_field: colkey = (head, host)
          else: colkey = (host, head)
          data[((auid,), colkey)] = lamb(result['dataHandlerWrappers'][0])
        _output_table(options, data, ['AUID'], [[x[0] for x in headlamb],
          sorted(options.hosts)] if options.group_by_field else [sorted(options.hosts), [x[0] for x in headlamb]])
      else:
        print('File not found, unknown error encountered.')

def _dispatch(options):
  if options.create_export_files: _do_create_export_files(options)
  else: raise RuntimeError('Unreachable')

def _make_client(host, username, password):
  session = requests.Session()
  session.auth = requests.auth.HTTPBasicAuth(username, password)
  transport = zeep.transports.Transport(session=session)
  wsdl = 'http://{}/ws/ExportService?wsdl'.format(host)
  client = zeep.Client(wsdl, transport=transport)
  return client

def _enable_zeep_debugging():
  logging.config.dictConfig({
    'version': 1,
    'formatters': {
      'verbose': {
        'format': '%(name)s: %(message)s'
      }
    },
    'handlers': {
      'console': {
        'level': 'DEBUG',
        'class': 'logging.StreamHandler',
        'formatter': 'verbose',
      },
    },
    'loggers': {
      'zeep.transports': {
        'level': 'DEBUG',
        'propagate': True,
        'handlers': ['console'],
      },
    }
  })

def _main():
  '''Main method.'''
  # Parse command line
  parser = _ExportServiceOptions.make_parser()
  args = parser.parse_args()
  options = _ExportServiceOptions(parser, args)
  # Dispatch
  t = Thread(target=_dispatch, args=(options,))
  t.daemon = True
  t.start()
  while True:
    t.join(1.5)
    if not t.is_alive(): break

if __name__ == '__main__': _main()

