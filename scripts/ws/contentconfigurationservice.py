#!/usr/bin/env python3

'''A library and a command line tool to interact with the LOCKSS daemon's
content configuration service via its Web Services API.'''

__copyright__ = '''\
Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.
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

__version__ = '0.3.0'

import sys

try: import requests
except ImportError: sys.exit('The Python Requests module must be installed (or on the PYTHONPATH)')

try: import zeep
except ImportError: sys.exit('The Python Zeep module must be installed (or on the PYTHONPATH)')

import argparse
import getpass
import itertools
from multiprocessing import Pool as ProcessPool
from multiprocessing.dummy import Pool as ThreadPool
import os.path
import requests.auth
from threading import Lock, Thread
import zeep.exceptions
import zeep.helpers
import zeep.transports

from wsutil import datems, datetimems, durationms, file_lines

#
# Library
#

def add_au_by_id(host, username, password, auid):
  '''
  Performs an addAuById operation (which adds a single AU on a single host, by
  AUID), and returns a record with these fields:
  - id (string): the AUID
  - isSuccess (boolean): a success flag
  - message (string): an error message
  - name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - username (string): a username for the host
  - password (string): a password for the host
  - auid (string): an AUID
  '''
  client = _make_client(host, username, password)
  return client.service.addAuById(auId = auid)

def add_aus_by_id_list(host, username, password, auids):
  '''
  Performs an addAusByIdList operation (which adds all given AUs on a single
  host, by AUID), and returns a list of records with these fields:
  - id (string): the AUID
  - isSuccess (boolean): a success flag
  - message (string): an error message
  - name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - username (string): a username for the host
  - password (string): a password for the host
  - auids (list of strings): a list of AUIDs
  '''
  client = _make_client(host, username, password)
  return client.service.addAusByIdList(auIds = auids)

def deactivate_au_by_id(host, username, password, auid):
  '''
  Performs a deactivateAuById operation (which deactivates a single AU on a
  single host, by AUID), and returns a record with these fields:
  - id (string): the AUID
  - isSuccess (boolean): a success flag
  - message (string): an error message
  - name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - username (string): a username for the host
  - password (string): a password for the host
  - auid (string): an AUID
  '''
  client = _make_client(host, username, password)
  return client.service.deactivateAuById(auId = auid)

def deactivate_aus_by_id_list(host, username, password, auids):
  '''
  Performs a deactivateAusByIdList operation (which deactivates all given AUs on
  a single host, by AUID), and returns a list of records with these fields:
  - id (string): the AUID
  - isSuccess (boolean): a success flag
  - message (string): an error message
  - name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - username (string): a username for the host
  - password (string): a password for the host
  - auids (list of strings): a list of AUIDs
  '''
  client = _make_client(host, username, password)
  return client.service.deactivateAusByIdList(auIds = auids)

def delete_au_by_id(host, username, password, auid):
  '''
  Performs a deleteAuById operation (which deletes a single AU on a single host,
  by AUID), and returns a record with these fields:
  - id (string): the AUID
  - isSuccess (boolean): a success flag
  - message (string): an error message
  - name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - username (string): a username for the host
  - password (string): a password for the host
  - auid (string): an AUID
  '''
  client = _make_client(host, username, password)
  return client.service.deleteAuById(auId = auid)

def delete_aus_by_id_list(host, username, password, auids):
  '''
  Performs a deleteAusByIdList operation (which deletes all given AUs on a
  single host, by AUID), and returns a list of records with these fields:
  - id (string): the AUID
  - isSuccess (boolean): a success flag
  - message (string): an error message
  - name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - username (string): a username for the host
  - password (string): a password for the host
  - auids (list of strings): a list of AUIDs
  '''
  client = _make_client(host, username, password)
  return client.service.deleteAusByIdList(auIds = auids)

def reactivate_au_by_id(host, username, password, auid):
  '''
  Performs a reactivateAuById operation (which reactivates a single AU on a
  single host, by AUID), and returns a record with these fields:
  - id (string): the AUID
  - isSuccess (boolean): a success flag
  - message (string): an error message
  - name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - username (string): a username for the host
  - password (string): a password for the host
  - auid (string): an AUID
  '''
  client = _make_client(host, username, password)
  return client.service.reactivateAuById(auId = auid)

def reactivate_aus_by_id_list(host, username, password, auids):
  '''
  Performs a reactivateAusByIdList operation (which reactivates all given AUs on
  a single host, by AUID), and returns a list of records with these fields:
  - id (string): the AUID
  - isSuccess (boolean): a success flag
  - message (string): an error message
  - name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - username (string): a username for the host
  - password (string): a password for the host
  - auids (list of strings): a list of AUIDs
  '''
  client = _make_client(host, username, password)
  return client.service.reactivateAusByIdList(auIds = auids)

#
# Command line tool
#

__tutorial__ = '''\
INTRODUCTION

This tool can be used to add, delete, activate and deactivate AUs on one or more
LOCKSS hosts. Invoking the tool consists of four parts:

- Specify the target hosts. Each occurrence of --host=HOST adds the host:port
pair HOST to the list of target hosts, and each occurrence of --hosts=HFILE adds
the host:port pairs in the file HFILE to the list of target hosts. HFILE can
contain comments, which begin at the character '#' and extend to the end of the
line. At least one target host is required. You will be prompted for a username
and password unless you pass them via --username and --password.

- Specify the target AUIDs. Likewise, each occurrence of --auid=AUID adds the
given AUID to the list of target AUIDs, and each occurrence of --auids=AFILE
adds the AUIDs in the file AFILE to the list of target AUIDs. AFILE can also
contain comments. At least one target AUID is required.

- Specify the desired operation. This is done by using exactly one of --add-aus,
--delete-aus, --deactivate-aus or --reactivate-aus.

- Optionally specify output options (see below).

OUTPUT

This tool can produce two styles of output: text output with --text-output and
tabular output with --table-output. By default, --text-output is in effect,
unless --table-output is explicitly specified.

When --text-output is in effect, unsuccessful operations are output one per line
on the console, host by host. You can additionally specify --verbose, in which
case all successful operations are also displayed host by host. The --verbose
option is only valid if --text-output is in effect.

When --table-output is in effect, a tab-separated table of unsuccessful
operations is output to the console, one row per target AU with at least one
unsuccessful operation and one column per target host.

In either output mode, the order of AUs listed (for each host in text mode, for
the whole table in tabular mode) is dictated by --sort-by-auid (AUID) or
--sort-by-name (AU name). By default, --sort-by-name is in effect, unless
--sort-by-auid is explicitly specified. Likewise, the way AUs are displayed is
governed by --list-by-auid (show the AUID), --list-by-name (show the AU name),
or --list-by-both (show the name and the AUID separated by a tab). By default,
--list-by-both is in effect unless another option in this category is specified.
The listing by name is currently just a string comparison, not a clever library
sort like in the LOCKSS daemon.

EXAMPLES

$ scripts/ws/contentconfigurationservice --host=foo.university.edu:8081 --auid=aaaaa1  --add-aus

Adds the AUID aaaaa1 to foo.university.edu:8081. Produces text output (the
default) only if the operation does not succeed.

$ scripts/ws/contentconfigurationservice --hosts=mydaemons.hosts --auids=myfile.auids  --add-aus

Adds the AUIDs contained in myfile.auids to all the hosts contained in
mydaemons.hosts. Produces text output (the default) only if some operations do
not succeed. AUs are sorted by AU name (the default) and displayed as a
name-AUID pair (the default).

$ scripts/ws/contentconfigurationservice --hosts=mydaemons.hosts --auids=myfile.auids  --add-aus --verbose

Adds the AUIDs contained in myfile.auids to all the hosts contained in
mydaemons.hosts. Produces text output (the default), both of successful
operations and unsuccessful operations. AUs are sorted by AU name (the default)
and displayed as a name-AUID pair (the default).

$ scripts/ws/contentconfigurationservice --hosts=mydaemons.hosts --auids=myfile.auids  --add-aus --list-by-name

Adds the AUIDs contained in myfile.auids to all the hosts contained in
mydaemons.hosts. Produces text output (the default) only if some operations do
not succeed. AUs are sorted by AU name (the default) and displayed by AU name.

$ scripts/ws/contentconfigurationservice --hosts=mydaemons.hosts --auids=myfile.auids  --add-aus --sort-by-auid --list-by-auid

Adds the AUIDs contained in myfile.auids to all the hosts contained in
mydaemons.hosts. Produces text output (the default) only if some operations do
not succeed. AUs are sorted by AUID and displayed by AUID.

$ scripts/ws/contentconfigurationservice --hosts=mydaemons.hosts --auids=myfile.auids  --add-aus --table-output

Adds the AUIDs contained in myfile.auids to all the hosts contained in
mydaemons.hosts. If any operation does not succeed, prints a table of
unsuccessful operations where each row is an AU and each column is a host. The
rows are sorted by AU name (the default) and displayed as a name-AUID pair (the
default).'''

class _ContentConfigurationServiceOptions(object):
  '''An internal object to encapsulate options suitable for this tool.'''

  @staticmethod
  def make_parser():
    '''Static method to make a command line parser suitable for this tool.'''
    usage = '%(prog)s {--host=HOST|--hosts=HFILE}... {--auid=AUID|--auids=AFILE}... {--add-aus|--deactivate-aus|--delete-aus|--reactivate-aus} [OPTIONS]'
    parser = argparse.ArgumentParser(description=__doc__, usage=usage)
    # Top-level options
    parser.add_argument('--copyright', action='store_true', help='display copyright and exit')
    parser.add_argument('--license', action='store_true', help='display software license and exit')
    parser.add_argument('--tutorial', action='store_true', help='display tutorial and exit')
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
    # Content configuration operations
    group = parser.add_argument_group('Content configuration operations')
    group.add_argument('--add-aus', action='store_true', help='add target AUs to target hosts')
    group.add_argument('--deactivate-aus', action='store_true', help='deactivate target AUs on target hosts')
    group.add_argument('--delete-aus', action='store_true', help='delete target AUs from target hosts')
    group.add_argument('--reactivate-aus', action='store_true', help='reactivate target AUs on target hosts')
    # Output options
    group = parser.add_argument_group('Output options')
    group.add_argument('--list-by-auid', action='store_true', help='list output by AUID')
    group.add_argument('--list-by-both', action='store_true', help='list output by both AU name and AUID (default)')
    group.add_argument('--list-by-name', action='store_true', help='list output by AU name')
    group.add_argument('--sort-by-auid', action='store_true', help='sort output by AUID')
    group.add_argument('--sort-by-name', action='store_true', help='sort output by AU name (default)')
    group.add_argument('--table-output', action='store_true', help='produce tabular output')
    group.add_argument('--text-output', action='store_true', help='produce text output (default)')
    group.add_argument('--verbose', action='store_true', default=False, help='make --text-output verbose (default: %(default)s)')
    # Job pool
    group = parser.add_argument_group('Job pool')
    group.add_argument('--pool-size', metavar='SIZE', type=int, default=0, help='size of the job pool, 0 for unlimited (default: %(default)s)')
    group.add_argument('--process-pool', action='store_true', help='use a process pool')
    group.add_argument('--thread-pool', action='store_true', help='use a thread pool (default)')
    # Other options
    group = parser.add_argument_group('Other options')
    group.add_argument('--batch-size', metavar='SIZE', type=int, default=100, help='size of AUID batches (default: %(default)s)')
    return parser

  def __init__(self, parser, args):
    '''
    Constructor.

    Parameters:
    - parser (OptionParser instance): the option parser
    - opts (Options instance): the Options instance returned by the parser
    - args (list of strings): the remaining command line arguments returned by
    the parser
    '''
    super(_ContentConfigurationServiceOptions, self).__init__()
    self.errors = 0
    # Special options
    if args.copyright: print(__copyright__)
    if args.license: print(__license__)
    if args.tutorial: print(__tutorial__)
    if any([args.copyright, args.license, args.tutorial]): sys.exit()
    # General checks
    # no need to check for unrecognized, argparse does this out of box
    if len(list(filter(None, [args.add_aus, args.reactivate_aus, args.delete_aus, args.deactivate_aus ]))) != 1:
      parser.error('exactly one of --add-aus, --reactivate-aus, --delete-aus, --deactivate-aus  is required')
    if len(list(filter(None, [args.table_output, args.text_output]))) > 1:
      parser.error('at most one of --table-output, --text-output can be specified')
    # hosts
    self.hosts = args.host[:]
    for f in args.hosts: self.hosts.extend(file_lines(f))
    if len(self.hosts) == 0: parser.error('at least one target host is required')
    # auids
    self.auids = args.auid[:]
    for f in args.auids: self.auids.extend(file_lines(f))
    # get_auids/get_auids_names/is_daemon_ready/is_daemon_ready_quiet
    if len(self.auids) == 0: parser.error('at least one target AUID is required')
    # au_operation
    if len(self.auids) > 1:
      if args.add_aus: self.au_operation = add_aus_by_id_list
      elif args.deactivate_aus: self.au_operation = deactivate_aus_by_id_list
      elif args.delete_aus: self.au_operation = delete_aus_by_id_list
      else: self.au_operation = reactivate_aus_by_id_list
    else:
      if args.add_aus: self.au_operation = add_au_by_id
      elif args.deactivate_aus: self.au_operation = deactivate_au_by_id
      elif args.delete_aus: self.au_operation = delete_au_by_id
      else: self.au_operation = reactivate_au_by_id
    # table_output/text_output/keysort/keydisplay/verbose
    self.table_output = args.table_output
    self.text_output = not self.table_output
    if args.sort_by_auid: self.keysort = _sort_by_auid
    else: self.keysort = _sort_by_name # default is --sort-by-name
    if args.list_by_auid: self.keydisplay = _list_by_auid
    elif args.list_by_name: self.keydisplay = _list_by_name
    else: self.keydisplay = _list_by_both # default is --list-by-both
    if self.text_output:
      self.verbose = args.verbose
    elif args.verbose:
      parser.error('--verbose can only be specified with --text-output')
    # pool_class/pool_size/batch_size
    if args.process_pool and args.thread_pool:
      parser.error('--process-pool and --thread-pool are mutually exclusive')
    self.pool_class = ProcessPool if args.process_pool else ThreadPool
    self.pool_size = args.pool_size or len(self.hosts)
    self.batch_size = args.batch_size
    # auth
    self._u = args.username or getpass.getpass('UI username: ')
    self._p = args.password or getpass.getpass('UI password: ')

# This is to allow pickling, so the process pool works, but this isn't great
# Have the sort and list params be enums and have keysort and keydisplay be methods?
def _sort_by_name(t): return t
def _sort_by_auid(t): return (t[1], t[0])
def _list_by_auid(t): return (t[1],) if t else ['AUID']
def _list_by_name(t): return (t[0],) if t else ['AU name']
def _list_by_both(t): return t if t else ['AU name', 'AUID']

def _do_au_operation_job(options_host):
  options, host = options_host
  data = dict()
  errors = 0
  if len(options.auids) == 1:
    r = options.au_operation(host, options._u, options._p, options.auids[0])
    if r.isSuccess: msg = None
    else:
      msg = (r.message or '').partition(':')[0]
      errors += 1
    data[((r.name, r.id), (host,))] = msg
  else:
      for i in range(0, len(options.auids), options.batch_size):
        result = options.au_operation(host,  options._u, options._p, options.auids[i:i+options.batch_size])
        for r in result:
          if r.isSuccess: msg = None
          else:
            msg = (r.message or '').partition(':')[0]
            errors += 1
          data[((r.name, r.id), (host,))] = msg
  return (host, data, errors)

def _do_au_operation(options):
  data = dict()
  pool = options.pool_class(options.pool_size)
  jobs = [(options, _host) for _host in options.hosts]
  for host, result, errors in pool.imap_unordered(_do_au_operation_job, jobs):
    data.update(result)
    options.errors = options.errors + errors
  if options.text_output:
    for host in sorted(options.hosts):
      hostresults = [(k[0], v) for k, v in data.items() if k[1][0] == host]
      if options.verbose:
        successful = list(filter(lambda x: x[1] is None, hostresults))
        if len(successful) > 0:
          _output_record(options, ['Successful on %s:' % (host,)])
          for x in sorted(successful, key=options.keysort):
            _output_record(options, options.keydisplay(x[0]))
          _output_record(options, [])
      unsuccessful = list(filter(lambda x: x[1] is not None, hostresults))
      if len(unsuccessful) > 0:
        _output_record(options, ['Unsuccessful on %s:' % (host,)])
        for x in sorted(unsuccessful, key=options.keysort):
          _output_record(options, options.keydisplay(x[0]) + (x[1],))
        _output_record(options, [])
  else:
    display = dict([((options.keydisplay(k[0]), k[1]), v) for k, v in data.iteritems()])
    _output_table(options, display, options.keydisplay(None), [options.hosts])

# Last modified 2015-08-05
def _output_record(options, lst):
  '''Internal method to display a single record.'''
  print('\t'.join([str(x or '') for x in lst]))

# Last modified 2016-05-16
def _output_table(options, data, rowheaders, lstcolkeys, rowsort=None):
  '''Internal method to display tabular output. (Should be refactored.)'''
  colkeys = [x for x in itertools.product(*lstcolkeys)]
  for j in xrange(len(lstcolkeys)):
    if j < len(lstcolkeys) - 1: rowpart = [''] * len(rowheaders)
    else: rowpart = rowheaders
    _output_record(options, rowpart + [x[j] for x in colkeys])
  for rowkey in sorted(set([k[0] for k in data]), key=rowsort):
    _output_record(options, list(rowkey) + [data.get((rowkey, colkey)) for colkey in colkeys])

def _make_client(host, username, password):
    session = requests.Session()
    session.auth = requests.auth.HTTPBasicAuth(username, password)
    transport = zeep.transports.Transport(session=session)
    wsdl = 'http://{}/ws/ContentConfigurationService?wsdl'.format(host)
    client = zeep.Client(wsdl, transport=transport)
    return client

def _main():
  '''Main method.'''
  # Parse command line
  parser = _ContentConfigurationServiceOptions.make_parser()
  args = parser.parse_args()
  options = _ContentConfigurationServiceOptions(parser, args)
  # Dispatch
  t = Thread(target=_do_au_operation, args=(options,))
  t.daemon = True
  t.start()
  while True:
    t.join(1.5)
    if not t.is_alive(): break
  # Errors
  if options.errors > 0:
    sys.exit('%d %s' % (options.errors, 'error' if options.errors == 1 else 'errors'))

# Main entry point
if __name__ == '__main__':
  _main()

