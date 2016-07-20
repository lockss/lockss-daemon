#!/usr/bin/env python

'''A library and a command line tool to interact with the LOCKSS daemon's
content configuration service via its Web Services API.'''

__copyright__ = '''\
Copyright (c) 2000-2016, Board of Trustees of Leland Stanford Jr. University
All rights reserved.'''

__license__ = '''\
Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.'''

__version__ = '0.2.2'

import getpass
import itertools
from multiprocessing import Pool as ProcessPool
from multiprocessing.dummy import Pool as ThreadPool
from optparse import OptionGroup, OptionParser
import os.path
import sys
from threading import Lock, Thread

import ContentConfigurationServiceImplService_client
from wsutil import zsiauth

#
# Library
#

def add_au_by_id(host, auth, auid):
  '''
  Performs an addAuById operation (which adds a single AU on a single host, by
  AUID), and returns a record with these fields:
  - Id (string): the AUID
  - IsSuccess (boolean): a success flag
  - Message (string): an error message
  - Name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  req = ContentConfigurationServiceImplService_client.addAuById()
  req.AuId = auid
  return _ws_port(host, auth).addAuById(req).Return

def add_aus_by_id_list(host, auth, auids):
  '''
  Performs an addAusByIdList operation (which adds all given AUs on a single
  host, by AUID), and returns a list of records with these fields:
  - Id (string): the AUID
  - IsSuccess (boolean): a success flag
  - Message (string): an error message
  - Name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auids (list of strings): a list of AUIDs
  '''
  req = ContentConfigurationServiceImplService_client.addAusByIdList()
  req.AuIds = auids
  return _ws_port(host, auth).addAusByIdList(req).Return

def deactivate_au_by_id(host, auth, auid):
  '''
  Performs a deactivateAuById operation (which deactivates a single AU on a
  single host, by AUID), and returns a record with these fields:
  - Id (string): the AUID
  - IsSuccess (boolean): a success flag
  - Message (string): an error message
  - Name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  req = ContentConfigurationServiceImplService_client.deactivateAuById()
  req.AuId = auid
  return _ws_port(host, auth).deactivateAuById(req).Return

def deactivate_aus_by_id_list(host, auth, auids):
  '''
  Performs a deactivateAusByIdList operation (which deactivates all given AUs on
  a single host, by AUID), and returns a list of records with these fields:
  - Id (string): the AUID
  - IsSuccess (boolean): a success flag
  - Message (string): an error message
  - Name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auids (list of strings): a list of AUIDs
  '''
  req = ContentConfigurationServiceImplService_client.deactivateAusByIdList()
  req.AuIds = auids
  return _ws_port(host, auth).deactivateAusByIdList(req).Return

def delete_au_by_id(host, auth, auid):
  '''
  Performs a deleteAuById operation (which deletes a single AU on a single host,
  by AUID), and returns a record with these fields:
  - Id (string): the AUID
  - IsSuccess (boolean): a success flag
  - Message (string): an error message
  - Name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  req = ContentConfigurationServiceImplService_client.deleteAuById()
  req.AuId = auid
  return _ws_port(host, auth).deleteAuById(req).Return

def delete_aus_by_id_list(host, auth, auids):
  '''
  Performs a deleteAusByIdList operation (which deletes all given AUs on a
  single host, by AUID), and returns a list of records with these fields:
  - Id (string): the AUID
  - IsSuccess (boolean): a success flag
  - Message (string): an error message
  - Name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auids (list of strings): a list of AUIDs
  '''
  req = ContentConfigurationServiceImplService_client.deleteAusByIdList()
  req.AuIds = auids
  return _ws_port(host, auth).deleteAusByIdList(req).Return

def reactivate_au_by_id(host, auth, auid):
  '''
  Performs a reactivateAuById operation (which reactivates a single AU on a
  single host, by AUID), and returns a record with these fields:
  - Id (string): the AUID
  - IsSuccess (boolean): a success flag
  - Message (string): an error message
  - Name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  req = ContentConfigurationServiceImplService_client.reactivateAuById()
  req.AuId = auid
  return _ws_port(host, auth).reactivateAuById(req).Return

def reactivate_aus_by_id_list(host, auth, auids):
  '''
  Performs a reactivateAusByIdList operation (which reactivates all given AUs on
  a single host, by AUID), and returns a list of records with these fields:
  - Id (string): the AUID
  - IsSuccess (boolean): a success flag
  - Message (string): an error message
  - Name (string): the AU name

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auids (list of strings): a list of AUIDs
  '''
  req = ContentConfigurationServiceImplService_client.reactivateAusByIdList()
  req.AuIds = auids
  return _ws_port(host, auth).reactivateAusByIdList(req).Return

def _ws_port(host, auth, tracefile=None):
  '''
  Internal convenience method used to set up a Web Services Port.

  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - tracefile (file object): an optional trace file (default None for no trace)
  '''
  url = 'http://%s/ws/ContentConfigurationService' % (host,)
  locator = ContentConfigurationServiceImplService_client.ContentConfigurationServiceImplServiceLocator()
  if tracefile is None: return locator.getContentConfigurationServiceImplPort(url=url, auth=auth)
  else: return locator.getContentConfigurationServiceImplPort(url=url, auth=auth, tracefile=tracefile)

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
    usage = '%prog {--host=HOST|--hosts=HFILE}... {--auid=AUID|--auids=AFILE}... {--add-aus|--deactivate-aus|--delete-aus|--reactivate-aus} [OPTIONS]'
    parser = OptionParser(version=__version__, description=__doc__, usage=usage)
    # Top-level options
    parser.add_option('--copyright', action='store_true', help='display copyright and exit')
    parser.add_option('--license', action='store_true', help='display software license and exit')
    parser.add_option('--tutorial', action='store_true', help='display tutorial and exit')
    # Hosts
    group = OptionGroup(parser, 'Target hosts')
    group.add_option('--host', action='append', default=list(), help='add host:port pair to list of target hosts')
    group.add_option('--hosts', action='append', default=list(), metavar='HFILE', help='add host:port pairs in HFILE to list of target hosts')
    group.add_option('--password', metavar='PASS', help='UI password (default: interactive prompt)')
    group.add_option('--username', metavar='USER', help='UI username (default: interactive prompt)')
    parser.add_option_group(group)
    # AUIDs
    group = OptionGroup(parser, 'Target AUIDs')
    group.add_option('--auid', action='append', default=list(), help='add AUID to list of target AUIDs')
    group.add_option('--auids', action='append', default=list(), metavar='AFILE', help='add AUIDs in AFILE to list of target AUIDs')
    parser.add_option_group(group)
    # Content configuration operations
    group = OptionGroup(parser, 'Content configuration operations')
    group.add_option('--add-aus', action='store_true', help='add target AUs to target hosts')
    group.add_option('--deactivate-aus', action='store_true', help='deactivate target AUs on target hosts')
    group.add_option('--delete-aus', action='store_true', help='delete target AUs from target hosts')
    group.add_option('--reactivate-aus', action='store_true', help='reactivate target AUs on target hosts')
    parser.add_option_group(group)
    # Output options
    group = OptionGroup(parser, 'Output options')
    group.add_option('--list-by-auid', action='store_true', help='list output by AUID')
    group.add_option('--list-by-both', action='store_true', help='list output by both AU name and AUID (default)')
    group.add_option('--list-by-name', action='store_true', help='list output by AU name')
    group.add_option('--sort-by-auid', action='store_true', help='sort output by AUID')
    group.add_option('--sort-by-name', action='store_true', help='sort output by AU name (default)')
    group.add_option('--table-output', action='store_true', help='produce tabular output')
    group.add_option('--text-output', action='store_true', help='produce text output (default)')
    group.add_option('--verbose', action='store_true', default=False, help='make --text-output verbose (default: %default)')
    parser.add_option_group(group)
    # Job pool
    group = OptionGroup(parser, 'Job pool')
    group.add_option('--pool-size', metavar='SIZE', type='int', default=0, help='size of the job pool, 0 for unlimited (default: %default)')
    group.add_option('--process-pool', action='store_true', help='use a process pool')
    group.add_option('--thread-pool', action='store_true', help='use a thread pool (default)')
    parser.add_option_group(group)
    # Other options
    group = OptionGroup(parser, 'Other options')
    group.add_option('--batch-size', metavar='SIZE', type='int', default=100, help='size of AUID batches (default: %default)')
    parser.add_option_group(group)
    return parser

  def __init__(self, parser, opts, args):
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
    if opts.copyright: print __copyright__
    if opts.license: print __license__
    if opts.tutorial: print __tutorial__
    if any([opts.copyright, opts.license, opts.tutorial]): sys.exit()
    # General checks
    if len(args) > 0:
      parser.error('unexpected command line arguments: %s' % (' '.join(args),))
    if len(filter(None, [opts.add_aus, opts.deactivate_aus, opts.delete_aus, opts.reactivate_aus])) != 1:
      parser.error('exactly one of --add-aus, --deactivate-aus, --delete-aus, --reactivate-aus is required')
    if len(filter(None, [opts.table_output, opts.text_output])) > 1:
      parser.error('at most one of --table-output, --text-output can be specified')
    # hosts
    self.hosts = opts.host[:]
    for f in opts.hosts: self.hosts.extend(_file_lines(f))
    if len(self.hosts) == 0: parser.error('at least one target host is required')
    # auids
    self.auids = opts.auid[:]
    for f in opts.auids: self.auids.extend(_file_lines(f))
    if len(self.auids) == 0: parser.error('at least one target AUID is required')
    # au_operation
    if opts.add_aus: self.au_operation = add_aus_by_id_list
    elif opts.deactivate_aus: self.au_operation = deactivate_aus_by_id_list
    elif opts.delete_aus: self.au_operation = delete_aus_by_id_list
    else: self.au_operation = reactivate_aus_by_id_list
    # table_output/text_output/keysort/keydisplay/verbose
    self.table_output = opts.table_output
    self.text_output = not self.table_output
    if opts.sort_by_auid: self.keysort = _sort_by_auid
    else: self.keysort = _sort_by_name # default is --sort-by-name
    if opts.list_by_auid: self.keydisplay = _list_by_auid
    elif opts.list_by_name: self.keydisplay = _list_by_name
    else: self.keydisplay = _list_by_both # default is --list-by-both
    if self.text_output:
      self.verbose = opts.verbose
    elif opts.verbose:
      parser.error('--verbose can only be specified with --text-output')
    # pool_class/pool_size/batch_size
    if opts.process_pool and opts.thread_pool:
      parser.error('--process-pool and --thread-pool are mutually exclusive')
    self.pool_class = ProcessPool if opts.process_pool else ThreadPool
    self.pool_size = opts.pool_size or len(self.hosts)
    self.batch_size = opts.batch_size
    # auth
    u = opts.username or getpass.getpass('UI username: ')
    p = opts.password or getpass.getpass('UI password: ')
    self.auth = zsiauth(u, p)

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
  for i in xrange(0, len(options.auids), options.batch_size):
    result = options.au_operation(host, options.auth, options.auids[i:i+options.batch_size])
    for r in result:
      if r.IsSuccess: msg = None
      else:
        msg = (r.Message or '').partition(':')[0]
        errors = errors + 1
      data[((r.Name, r.Id), (host,))] = msg
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
      hostresults = [(k[0], v) for k, v in data.iteritems() if k[1] == host]
      if options.verbose:
        successful = filter(lambda x: x[1] is None, hostresults)
        if len(successful) > 0:
          _output_record(options, ['Successful on %s:' % (host,)])
          for x in sorted(successful, key=options.keysort):
            _output_record(options, options.keydisplay(x[0]))
          _output_record(options, [])
      unsuccessful = filter(lambda x: x[1] is not None, hostresults)
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
  print '\t'.join([str(x or '') for x in lst])

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

# Last modified 2015-08-31
def _file_lines(fstr):
  with open(os.path.expanduser(fstr)) as f: ret = filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f])
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
  return ret

def _main():
  '''Main method.'''
  # Parse command line
  parser = _ContentConfigurationServiceOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _ContentConfigurationServiceOptions(parser, opts, args)
  # Dispatch
  t = Thread(target=_do_au_operation, args=(options,))
  t.daemon = True
  t.start()
  while True:
    t.join(1.5)
    if not t.is_alive(): break
  # Errors
  if options.errors > 0: sys.exit('%d %s; exiting' % (options.errors, 'error' if options.errors == 1 else 'errors'))

# Main entry point
if __name__ == '__main__': _main()

