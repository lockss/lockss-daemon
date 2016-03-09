#!/usr/bin/env python

'''A rudimentary script to change the status, status2, or proxy setting of AUs
in TDB files from one or more old values to a new value.'''

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

__tutorial__ = '''
This utility assists in editing the status, status2, or proxy setting of AUs in
TDB files. It is not a parser; it is more like text editor macros, that view TDB
files as text files to be edited with regular expressions. As a result, it works
well for many typical TDB files, but will not work on arbitrarily complex TDB
files. In particular, it assumes all implicit<...> and au<...> statements do not
span multiple lines and do not share a line with other statements (other than
maybe a comment at the end of the line). Also, it will get confused if in an
au<...> statement there is a value other than the AU's 'name' containing a
semicolon. Other smaller caveats may exist.

tdbedit.py is invoked with one or more TDB files, one or more AUIDs, and one or
more pairs of change requests for those AUIDs in those TDB files.

The TDB files are specified as arguments at the command line:

  $ tdbedit.py tdb/network/foo.tdb tdb/network/bar.tdb ...

The shell might expand globs to list files. You might type:

  $ tdbedit.py tdb/network/*.tdb

and the shell will invoke tdbedit.py as:

  $ tdbedit.py tdb/network/aaa.tdb tdb/network/bbb.tdb tdb/network/ccc.tdb ...

AUIDs can be added to the list tdbedit.py is going to process in two ways. The
first way is to pass one or more AUIDs with one or more occurrences of the
--auid option:

  $ tdbedit.py --auid='AUID1' --auid='AUID2' ...

Note that AUIDs contain characters like '&' that the shell will interpret as
special, so quoting AUIDs is necessary. The second way is to pass one or more
names of files full of AUIDs with one or more occurrences of the --auids option:

  $ tdbedit.py --auids=mybatches/batch1.txt --auids=mybatches/batch2.txt ...

In this type of file, there can be comments beginning with '#' and spanning to
the end of a line, and any non-whitespace text remaining is treated as an AUID.
Currently it is an error to supply a file with --auids that turns out to have
zero AUIDs in it (but this restriction may be relaxed in the future).

A status change request consists of a pair: one or more statuses to change from,
and one status to change to. One can request changes to the 'status' trait of
AUs with --from-status and --to-status, or to the 'status2' trait of AUs with
--from-status2 and --to-status2, or both. Multiple statuses in --from-status or
--from-status2 are listed comma-separated:

  $ tdbedit.py --from-status=manifest --to-status=testing ...

This means "for the given AUIDs, change the status from 'manifest' to
'testing'". If AUs have a status2, it remains unchanged.

  $ tdbedit.py --from-status2=frozen --to-status2=finished ...

This means "for the given AUIDs, change the status2 from 'frozen' to
'finished'". The status remains unchanged, only the status2 is changed.

  $ tdbedit.py --from-status=frozen --from-status2=exists --to-status=finished --to-status2=manifest ...

This means "for the given AUIDs, change from status 'frozen' and status2
'exists' to status 'finished' and status2 'manifest'", or, said otherwise, "from
'frozen;exists' to 'finished;manifest'".

  $ tdbedit.py --from-status=manifest,testing,ready --to-status=doNotProcess ...

This means "for the given AUIDs, change the status from 'manifest' or 'testing'
or 'ready' to 'doNotProcess'".

When both --from-status and --from-status2 are present and one or both specify
multiple statuses, the result is all possible combinations. For example:

  $ tdbedit.py --from-status=manifest,ready --from-status2=exists,expected ...

means AUs with status 'manifest' and status2 'exists' and AUs with status
'manifest' and status2 'expected' and AUs with status 'ready' and status2
'exists' and AUs with status 'ready' and status2 'expected', or, said otherwise,
AUs with status 'manifest;exists' or 'manifest;expected' or 'ready;exists' or
'ready;expected'.

Likewise, a proxy setting change request consists of a pair: one or more proxy
settings to change from and a proxy setting to change to, specified with
--from-proxy and --to-proxy. The special value 'NONE' can be used in either
--from-proxy or --to-proxy to specify an unset proxy setting. The special value
'ANY' can be used in --from-proxy to mean any current value (set or unset).

Alternatively, the request can be to change the proxy setting to one of several
values. With --to-proxy-round-robin, the various comma-separated values given
are used one after the other to set the proxy setting. With --to-proxy-random,
one of the comma-separated values given is picked at random to set the proxy
setting of each target AU. 'NONE' can be used (but it is not expected to have a
practical application in this context).

Note that in TDB parlance, the name of the proxy trait is 'hidden[proxy]' rather
than 'proxy'.

It is an error for a change request to specify that a trait be changed from one
or more values to some target value if it turns out a target AU's trait does not
have a value equal to any of the one or more given values. For example, if you
request that various AUs with status2 'manifest' or 'crawling' be changed to
some target status2 and one of the AUs you specify is found to be in status2
'exists', an error will be reported.

If all change requests are valid, the corresponding TDB files will be altered.
A copy of the original TDB file is made first. For a file named 'a.tdb', the
backup copy is made under 'a.tdb.bak'. If that backup file already exists, it is
overwritten.
'''

__version__ = '0.3.0'

import cStringIO
import optparse
import os.path
import random
import re
import subprocess
import sys

_ANY = 'ANY'
_NONE = 'NONE'

class _TdbEditOptions(object):
  '''An internal class to represent the result of parsing the command line.'''

  @staticmethod
  def make_parser():
    '''
    Makes a command line parser suitable for the command abstraction defined by
    this class.
    '''
    usage = '%prog [--from-status=CSSTATUS --to-status=STATUS] [--from-status2=CSSTATUS --to-status2=STATUS] [--from-proxy=CSPROXY [--to-proxy=PROXY|--to-proxy-round-robin=CSPROXY]] [--auids=AFILE|--auid=AUID] FILE...'
    parser = optparse.OptionParser(version=__version__, usage=usage, description=__doc__)
    parser.add_option('--tutorial', action='store_true', help='show tutorial and exit')
    group = optparse.OptionGroup(parser, 'AUIDs')
    group.add_option('--auid', action='append', default=list(), help='add AUID to list of target AUIDs')
    group.add_option('--auids', action='append', default=list(), metavar='AFILE', help='add AUIDs in AFILE to list of target AUIDs')
    parser.add_option_group(group)
    group = optparse.OptionGroup(parser, 'Status changes')
    group.add_option('--from-status', default=None, metavar='CSSTATUS', help='status values to update from (comma-separated)')
    group.add_option('--from-status2', default=None, metavar='CSSTATUS', help='status2 values to update from (comma-separated)')
    group.add_option('--to-status', default=None, metavar='STATUS', help='status value to update to')
    group.add_option('--to-status2', default=None, metavar='STATUS', help='status2 value to update to')
    parser.add_option_group(group)
    group = optparse.OptionGroup(parser, 'Proxy changes')
    group.add_option('--from-proxy', metavar='CSPROXY', help='proxy settings to update from (comma-separated; can include %s, %s)' % (_NONE, _ANY))
    group.add_option('--to-proxy', metavar='PROXY', help='proxy setting to update to (can be %s)' % (_NONE,))
    group.add_option('--to-proxy-random', metavar='CSPROXY', help='proxy settings to update to, assigned randomly')
    group.add_option('--to-proxy-round-robin', metavar='CSPROXY', help='proxy settings to update to, assigned in round robin')
    parser.add_option_group(group)
    return parser

  def __init__(self, parser, opts, args):
    '''
    Constructor.
    If --tutorial is specified at the command line, prints out a tutorial to
    standard output and exits immediately.
    Parameters:
    - parser: the optparse parser returned by _TdbEditOptions.make_parser() used
    to process the command line
    - opts: the first half of the tuple returned by optparse (options)
    - args: the second half of the tuple returned by optparse (other arguments)
    Defines these fields:
    - status_change (boolean): if acting on status
    - status2_change (boolean): if acting on status2
    - proxy_change (boolean): if acting on proxy setting
    - from_status (set of strings) and to_status (string): if acting on status,
    statuses to change from and status to change to
    - from_status2 (set of strings) and to_status2 (string): if acting on
    status2, statuses to change from and status to change to
    - from_proxy (set of strings) and to_proxy (list of strings): if acting on
    the proxy setting, proxies to change from (including _NONE, _ANY) and
    possible proxies to change to (possibly _NONE)
    - __proxy (generator of strings): bottomless generator of values from
    to_proxy based on the requested type of proxy change; see __proxy_random(),
    __proxy_round_robin() and next_proxy()
    - auids (list of strings): AUIDs the changes apply to
    - files (list of strings): files the changes apply to
    '''
    super(_TdbEditOptions, self).__init__()
    # Tutorial
    if opts.tutorial:
      parser.print_help()
      print __tutorial__
      sys.exit()
    # status_change/from_status/to_status
    if (opts.from_status is None) != (opts.to_status is None):
      parser.error('--from-status and --to-status must be specified together')
    self.status_change = opts.from_status is not None
    if self.status_change:
      self.from_status = set([x.strip() for x in opts.from_status.split(',')])
      self.to_status = opts.to_status
    # status2_change/from_status2/to_status2
    if (opts.from_status2 is None) != (opts.to_status2 is None):
      parser.error('--from-status2 and --to-status2 must be specified together')
    self.status2_change = opts.from_status2 is not None
    if self.status2_change:
      self.from_status2 = set([x.strip() for x in opts.from_status2.split(',')])
      self.to_status2 = opts.to_status2
    # proxy_change/from_proxy/to_proxy/__proxy
    _toproxy = [opts.to_proxy, opts.to_proxy_random, opts.to_proxy_round_robin]
    if (opts.from_proxy is None) != (not any(_toproxy)):
      parser.error('--from-proxy and --to-proxy/--to-proxy-random/-to-proxy-round-robin must be specified together')
    if len(filter(None, _toproxy)) > 1:
      parser.error('only one of --to-proxy, --to-proxy-random, -to-proxy-round-robin can be requested')
    self.proxy_change = opts.from_proxy is not None
    if self.proxy_change:
      self.from_proxy = set([x.strip() for x in opts.from_proxy.split(',')])
      self.to_proxy = [x.strip() for x in opts.to_proxy or (opts.to_proxy_random or opts.to_proxy_round_robin).split(',')]
      if opts.to_proxy_random is not None: self.__proxy = self.__proxy_random()
      else: self.__proxy = self.__proxy_round_robin()
    # files
    if len(args) == 0: parser.error('at least one file is required')
    self.files = args[:]
    # auids
    self.auids = opts.auid[:]
    for f in opts.auids: self.auids.extend(_file_lines(f))
    if len(self.auids) == 0: parser.error('at least one target AUID is required')

  def __proxy_random(self):
    '''Bottomless generator of strings returning random values from to_proxy.'''
    while True: yield random.choice(self.to_proxy)
    
  def __proxy_round_robin(self):
    '''Bottomless generator over the values from to_proxy in a round robin.'''
    while True:
      for x in self.to_proxy: yield x

  def next_proxy(self):
    '''Returns the next desired proxy setting value.'''
    return next(self.__proxy)

class _Au(object):
  '''An internal class to represent an AU entry.'''

  def __init__(self, filestr, lineindex, implicitmap, body, changed=False):
    '''
    Constructor.
    Parameters (all becoming fields, except 'body' which is split into the
    'values' array and can be regenerated with 'generate_body()'):
    - filestr (string): the file path of the AU entry
    - lineindex (integer): the (zero-based) line index within the file
    - implicitmap (dict): the AU entry's implicit<...> specification, mapping
    from string (key in the implicit<...> statement) to integer (zero-based
    index of the key in the implicit<...> statement)
    - body (string): the AU entry's textual body
    - changed (boolean): a "dirty bit" set to True to indicate the entry has
    been modified (default: False)
    '''
    super(_Au, self).__init__()
    # filestr/lineindex/implicitmap/changed
    self.filestr = filestr
    self.lineindex = lineindex
    self.implicitmap = implicitmap
    self.changed = changed
    # values
    vals = body.split(';')
    namelen = len(vals) - len(self.implicitmap) + 1
    if namelen == 1:
      self.values = vals
    else:
      namebegin = self.implicitmap['name']
      nameend = namebegin + namelen
      self.values = vals[:namebegin] + [';'.join(vals[namebegin:nameend])] + vals[nameend:]

  def is_changed(self):
    '''Determines if the AU's "dirty bit" is set.'''
    return self.changed

  def get_name(self):
    '''Returns the AU's name.'''
    return self._get('name')

  def get_proxy(self):
    '''Returns the AU's proxy setting (or None if unset).'''
    val = self._get('hidden[proxy]')
    if val is not None and len(val) == 0: val = None
    return val

  def set_proxy(self, val):
    '''Sets the AU's proxy setting to the given value (or None to unset).'''
    if val is None: val = ''
    self._set('hidden[proxy]', val)

  def get_status(self):
    '''Returns the AU's status.'''
    return self._get('status')

  def set_status(self, val):
    '''Sets the AUs'a status to the given value.'''
    self._set('status', val)

  def get_status2(self):
    '''Returns the AU's status2.'''
    return self._get('status2')

  def set_status2(self, val):
    '''Sets the AU's status2 to the given value.'''
    self._set('status2', val)

  def generate_body(self):
    '''Regenerates the AU's textual body.'''
    return ';'.join(self.values)

  def _get(self, field):
    '''
    Retrieves the value of the given field with leading and trailing whitespace
    stripped (or None if no such field is defined).
    '''
    fieldindex = self.implicitmap.get(field)
    if fieldindex is None: return None
    else: return self.values[fieldindex].strip()

  def _set(self, field, val):
    '''
    Sets the given field to the given value with added leading and trailing
    whitespace, and sets the "dirty bit" if the new value is different from the
    old value. Raises KeyError if no such field is defined.
    '''
    fieldindex = self.implicitmap.get(field)
    if fieldindex is None:
      sys.stderr.write('%s:%d: KeyError' % (self.filestr, self.lineindex)) ###DEBUG
      raise KeyError, field
    if val.strip() != self.values[fieldindex].strip():
      self.values[fieldindex] = ' %s ' % (val,)
      self.changed = True

# A regular expression to match implicit<...> lines
# - Group 1: semicolon-separated body of the implicit<...> statement
_IMPLICIT = re.compile(r'^[ \t]*implicit[ \t]*<(.*)>[ \t]*(#.*)?$')

# A regular expression to match au<...> lines
# - Group 1: semicolon-separated body of the au<...> statement
_AU = re.compile(r'^[ \t]*au[ \t]*<(.*)>[ \t]*(#.*)?$')

def _get_auids(options):
  '''
  Invokes tdbout on the given files and returns a list of AUIDs.
  Parameters:
  - options (of type _TdbEditOptions): the current options
  '''
  cmd = ['scripts/tdb/tdbout', '--auid']
  cmd.extend(options.files)
  proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  (out, err) = proc.communicate() # out and err are (potentially huge) strings
  if proc.returncode != 0: sys.exit(err)
  return [line.strip() for line in cStringIO.StringIO(out)]

def _build_au_index(options):
  '''
  Scans the given files, attempting to detect every AU entry, and returns a list
  of _Au instances. Gives up and exits with sys.exit() if an implicit<...>
  statement does not seem to define a 'name' or 'status' trait, or a 'status2'
  trait when status2 processing is requested.
  Parameters:
  - options (of type _TdbEditOptions): the current options
  '''
  errors = 0
  aus = list()
  for fstr in options.files:
    continue1 = False
    with open(fstr, 'r') as f:
      for lineindex, line in enumerate(f):
        mat = _IMPLICIT.search(line)
        if mat is not None:
          impl = [trait.strip() for trait in mat.group(1).split(';')]
          implmap = dict([(x, i) for i, x in enumerate(impl)])
          if 'name' not in implmap:
            errors = errors + 1
            sys.stderr.write('%s:%s: implicit statement does not specify \'name\'\n' % (fstr, lineindex + 1))
            continue1 = True # next file
            break
          if 'status' not in implmap:
            errors = errors + 1
            sys.stderr.write('%s:%s: implicit statement does not specify \'status\'\n' % (fstr, lineindex + 1))
            continue1 = True # next file
            break
          if options.status2_change is not None and 'status2' not in implmap:
            errors = errors + 1
            sys.stderr.write('%s:%s: implicit statement does not specify \'status2\'\n' % (fstr, lineindex + 1))
            continue1 = True # next file
            break
          continue # next line
        mat = _AU.search(line)
        if mat is not None:
          au = _Au(fstr, lineindex, implmap, mat.group(1))
          aus.append(au)
          continue # next line
      if continue1: continue # next file
  if errors > 0: sys.exit('%d %s; exiting' % (errors, 'error' if errors == 1 else 'errors'))
  return aus

def _change_aus(options, auids, aus):
  '''
  Performs the requested changes, in memory. Exits with sys.exit() if a
  requested AUID is not found or if a requested change is not valid for an AUID.
  Parameters:
  - options (of type _TdbEditOptions): the current options
  - auids (list of strings): the list of parsed AUIDs
  - aus (list of _Au instances): the list of AU entries
  '''
  auindex = dict([(x, i) for i, x in enumerate(auids)])
  errors = 0
  for auid in options.auids:
    # Check that the requested AUID is in the parsed files
    index = auindex.get(auid)
    if index is None:
      errors = errors + 1
      sys.stderr.write('No AU declaration found in these files for %s\n' % (auid,))
      continue # next AUID
    # Check that the changes are valid
    au = aus[index]
    if options.status_change and au.get_status() not in options.from_status:
      errors = errors + 1
      sys.stderr.write('%s:%s: %s: status \'%s\' is not one of %s\n' % (au.filestr, au.lineindex + 1, au.get_name(), au.get_status(), ', '.join(options.from_status)))
      continue # next AUID
    if options.status2_change and au.get_status2() not in options.from_status2:
      errors = errors + 1
      sys.stderr.write('%s:%s: %s: status2 \'%s\' is not one of %s\n' % (au.filestr, au.lineindex + 1, au.get_name(), au.get_status2(), ', '.join(options.from_status2)))
      continue # next AUID
    if options.proxy_change and _ANY not in options.from_proxy:
      found = au.get_proxy() or _NONE
      if found not in options.from_proxy:
        errors = errors + 1
        sys.stderr.write('%s:%s: %s: proxy \'%s\' is not one of %s\n' % (au.filestr, au.lineindex + 1, au.get_name(), found, ', '.join(options.from_proxy)))
        continue # next AUID
    # Perform requested changes if necessary
    if options.status_change: au.set_status(options.to_status)
    if options.status2_change: au.set_status2(options.to_status2)
    if options.proxy_change:
      target = options.next_proxy()
      if target == _NONE: target = None
      au.set_proxy(target)
  if errors > 0: sys.exit('%d %s; exiting' % (errors, 'error' if errors == 1 else 'errors'))

def _alter_files(options, aus):
  '''
  Rewrites files to reflect changes to AUs.
  Parameters:
  - options (of type _TdbEditOptions): the current options
  - auids (list of strings): the list of parsed AUIDs
  - aus (list of _Au instances): the list of AU entries
  '''
  # Reduce to list of changed AUS and list of affected files
  touch_aus = filter(lambda a: a.is_changed(), aus)
  touch_files = list()
  for au in touch_aus:
    if au.filestr not in touch_files: touch_files.append(au.filestr)
  # Process each affected file
  for touch_file in touch_files:
    # Read file into memory and copy into backup file
    with open(touch_file, 'r') as f: lines = f.readlines()
    with open('%s.bak' % (touch_file,), 'w') as fbak: fbak.writelines(lines)
    # Process AUs from that file
    for au in filter(lambda a: a.filestr == touch_file, touch_aus):
      # Reconstruct altered au<...> statement
      line = lines[au.lineindex]
      mat = _AU.search(line)
      aumid = mat.group(1).split(';')
      line = line[:mat.start(1)] + au.generate_body() + line[mat.end(1):]
      lines[au.lineindex] = line
    # Write out modified file
    with open(touch_file, 'w') as f: f.writelines(lines)

def _file_lines(fstr):
  '''
  Returns an array of all the lines in the file fstr that do not begin
  with '#' and that are not entirely whitespace, with leading and trailing
  whitespace trimmed. Exits with sys.exit() if the result has zero elements.
  '''
# Last modified 2015-08-31
  with open(os.path.expanduser(fstr)) as f: ret = filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f])
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
  return ret

def _main():
  '''Main method.'''
  # Parse command line
  parser = _TdbEditOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _TdbEditOptions(parser, opts, args)
  # Get AUIDs and scan files for AU entries
  auids = _get_auids(options)
  aus = _build_au_index(options)
  if len(auids) != len(aus): sys.exit('Error: tdbout parsed %d AU declarations but tdbedit found %d; exiting' % (len(auids), len(aus)))
  # Perform changes in memory
  _change_aus(options, auids, aus)
  # Translate changes into files
  _alter_files(options, aus)

# Main entry point
if __name__ == '__main__': _main()

