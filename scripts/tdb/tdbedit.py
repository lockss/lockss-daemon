#!/usr/bin/env python

'''A rudimentary script to change the status (or status2, or both) of AUs in TDB
files from one or more old values to a new value.'''

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

__tutorial__ = '''
This utility assists in editing AU statuses in TDB files. It is not a parser; it
is more like text editor macros, that view TDB files as text files to be edited
with regular expressions. As a result, it works well for many typical TDB files
but will not work on arbitrarily complex TDB files. In particular, it assumes
all implicit<...> and au<...> statements do not span multiple lines and do not
share a line with other statements (other than maybe a comment at the end of the
line). Also, it will get confused if in an au<...> statement there is a value
containing a semicolon (for instance the AU's 'name' value) earlier in the line
than the 'status' or 'status2' values. Other smaller caveats may exist.

tdbedit.py is invoked with one or more TDB files, one or more AUIDs, and one or
two status change requests for those AUIDs in those TDB files.

The TDB files are specified as arguments at the command line:

  tdbedit.py tdb/network/foo.tdb tdb/network/bar.tdb ...

The shell might expand globs to list files. You might type:

  tdbedit.py tdb/network/*.tdb

and the shell will invoke tdbedit.py as:

  tdbedit.py tdb/network/aaa.tdb tdb/network/bbbb.tdb tdb/network/ccccc.tdb ...

AUIDs can be added to the list tdbedit.py is going to process in two ways. The
first way is to pass one or more AUIDs with one or more occurrences of the
--auid option:

  tdbedit.py --auid='AUID1' --auid='AUID2' ...

Note that AUIDs contain characters like '&' that the shell will interpret as
special, so quoting AUIDs is necessary. The second way is to pass one or more
names of files full of AUIDs with one or more occurrences of the --auids option:

  tdbedit.py --auids=mybatches/batch1.txt --auids=mybatches/batch2.txt ...

Each line in this type of file is an AUID, except for lines that are empty or
full of whitespace (which are ignored) and comment lines beginning with '#'
(which are also ignored). Currently it is an error to supply a file with --auids
that turns out to have zero AUIDs in it (but this restriction may be relaxed in
the future).

A status change request consists of a pair: one or more statuses to change from,
and one status to change to. One can request changes to the 'status' trait of
AUs with --from and --to or to the 'status2' trait of AUs with --from2 and --to2
(or both --from/--to and --from2/--to2). Multiple statuses in --from or --from2
are listed comma-separated:

  tdbedit.py --from=manifest --to=testing ...

This means "for the given AUIDs, change the status from 'manifest' to
'testing'". If AUs have a status2, it remains unchanged.

  tdbedit.py --from2=frozen --to2=finished ...

This means "for the given AUIDs, change the status2 from 'frozen' to
'finished'". The status remains unchanged, only the status2 is changed.

  tdbedit.py --from=frozen --from2=exists --to=finished --to2=manifest ...

This means "for the given AUIDs, change from status 'frozen' and status2
'exists' to status 'finished' and status2 'manifest'", or, said otherwise, "from
'frozen;exists' to 'finished;manifest'".

  tdbedit.py --from=manifest,testing,ready --to=doNotProcess ...

This means "for the given AUIDs, change the status from 'manifest' or 'testing'
or 'ready' to 'doNotProcess'".

When both --from and --from2 are present and one or both specify multiple
statuses, the result is all possible combinations. For example:

  tdbedit.py --from=manifest,ready --from2=exists,expected ...

means AUs with status 'manifest' and status2 'exists' and AUs with status
'manifest' and status2 'expected' and AUs with status 'ready' and status2
'exists' and AUs with status 'ready' and status2 'expected', or, said otherwise,
AUs with status 'manifest;exists' or 'manifest;expected' or 'ready;exists' or
'ready;expected'.

It is an error to request for an AU to be changed from one or more statuses to
some target status if it turns out the AU is not in any of the one or more given
statuses. In other words, if you request that an AU with status2 'manifest' or
'crawling' be changed to some target status2 and one of the AUs you specify is
found to be in status2 'exists', an error will be reported.

If all change requests are valid, the corresponding TDB files will be altered.
A copy of the original TDB file is made first. For a file named 'a.tdb', the
backup copy is made under 'a.tdb.bak'. If that backup file already exists, it is
overwritten.
'''

__version__ = '0.1.4'

import cStringIO
import optparse
import os.path
import re
import subprocess
import sys

class _TdbEditOptions(object):
  '''An internal class to represent the result of parsing the command line.'''

  def __init__(self, parser, opts, args):
    '''Constructor.
    Parameters:
    - parser: the optparse parser returned by _TdbEditOptions.make_parser() used
    to process the command line
    - opts: the first half of the tuple returned by optparse (options)
    - args: the second half of the tuple returned by optparse (other arguments)
    Defines these fields:
    - from1 (list of strings) and to1 (string): if acting on status, statuses to
    change from and status to change to (or both None)
    - from2 (list of strings) and to2 (string): if acting on status2, statuses
    to change from and status to change to (or both None)
    - auids (list of strings): AUIDs the changes apply to
    - files (list of strings): files the changes apply to
    '''
    super(_TdbEditOptions, self).__init__()
    # Tutorial
    if opts.tutorial:
      parser.print_help()
      print __tutorial__
      sys.exit()
    # from1/to1
    if (opts.from1 is None) != (opts.to1 is None): parser.error('--from and --to must be specified together')
    if opts.from1 is None: self.from1 = None
    else: self.from1 = opts.from1.split(',')
    self.to1 = opts.to1
    # from2/to2
    if (opts.from2 is None) != (opts.to2 is None): parser.error('--from2 and --to2 must be specified together')
    if opts.from2 is None: self.from2 = None
    else: self.from2 = opts.from2.split(',')
    self.to2 = opts.to2
    # files
    if len(args) == 0: parser.error('at least one file is required')
    self.files = args[:]
    # auids
    self.auids = opts.auid[:]
    for f in opts.auids: self.auids.extend(_file_lines(f))
    if len(self.auids) == 0: parser.error('at least one AUID is required')

  @staticmethod
  def make_parser():
    '''Makes a command line parser suitable for the command abstraction defined
    by this class.
    '''
    usage = '%prog [--from=CSSTATUSES --to=STATUS] [--from2=CSSTATUSES --to2=STATUS] [--auids=AFILE|--auid=AUID] FILE...'
    parser = optparse.OptionParser(version=__version__, usage=usage, description=__doc__)
    parser.add_option('--tutorial', action='store_true', help='display tutorial text and exit')
    agroup = optparse.OptionGroup(parser, 'AUIDs')
    agroup.add_option('--auid', action='append', default=list(), help='add AUID to the list of AUIDs')
    agroup.add_option('--auids', action='append', default=list(), metavar='AFILE', help='add the AUIDs in AFILE to the list of AUIDs')
    parser.add_option_group(agroup)
    sgroup = optparse.OptionGroup(parser, 'Status changes')
    sgroup.add_option('--from', dest='from1', default=None, metavar='CSSTATUSES', help='statuses to update from (comma-separated)')
    sgroup.add_option('--from2', default=None, metavar='CSSTATUSES', help='"status2" statuses to update from (comma-separated)')
    sgroup.add_option('--to', dest='to1', default=None, metavar='STATUS', help='status to update to')
    sgroup.add_option('--to2', default=None, metavar='STATUS', help='"status2" status to update to')
    parser.add_option_group(sgroup)
    return parser

class _Au(object):
  '''An internal struct class to represent an AU entry.'''

  def __init__(self, filestr, lineindex, statusindex, status, status2index, status2, changed=False):
    '''Constructor.
    Parameters (all becoming fields):
    - filestr (string): the file path of the AU entry
    - lineindex (integer): the line index (meaning, zero-based) within the file
    - statusindex (integer): the index (meaning, zero-based) of the status field
    within the semicolon-separated au<...> body
    - status (string): the status value
    - status2index (integer): the index (meaning, zero-based) of the status2
    field within the semicolon-separated au<...> body, or -1 if undefined
    - status2 (string): the satus2 value
    - changed (boolean): a "dirty bit" set to True to indicate the entry has
    been modified (default: False)
    '''
    super(_Au, self).__init__()
    self.filestr = filestr
    self.lineindex = lineindex
    self.statusindex = statusindex
    self.status = status
    self.status2index = status2index
    self.status2 = status2
    self.changed = changed

  def __repr__(self):
    '''Returns an interpreter-ready string representation of this object.'''
    return '''_Au('%s', %d, %d, '%s', %d, '%s', '%s')''' % (self.filestr, \
        self.lineindex, self.statusindex, self.status, self.status2index, \
        self.status2, self.changed)

# A regular expression to match implicit<...> lines
# - Group 1: semicolon-separated body of the implicit<...> statement
_IMPLICIT = re.compile(r'^[^#]*implicit[ \t]*<(.*)>[ \t]*(#.*)?$')

# A regular expression to match au<...> lines
# - Group 1: semicolon-separated body of the au<...> statement
_AU = re.compile(r'^[^#]*au[ \t]*<(.*)>[ \t]*(#.*)?$')

def _file_lines(fstr):
  '''Returns an array of all the lines in the file fstr that do not begin
  with '#' and that are not entirely whitespace, with leading and trailing
  whitespace trimmed. Exits with sys.exit() if the result has zero elements.
  '''
# Last modified 2015-08-31
  with open(os.path.expanduser(fstr)) as f: ret = filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f])
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (fstr,))
  return ret

def _get_auids(options):
  '''Invokes tdbout on the given files and returns a list of AUIDs.
  Parameters:
  - options (of type _TdbEditOptions): the current options
  '''
  cmd = ['scripts/tdb/tdbout', '--auid']
  cmd.extend(options.files)
  proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  (out, err) = proc.communicate() # out and err are (potentially huge) strings
  if proc.returncode != 0:
    sys.stderr.write(err)
    sys.exit(1)
  return [line.strip() for line in cStringIO.StringIO(out)]

def _build_au_index(options):
  '''Scans the given files, attempting to detect every AU entry, and returns a
  list of _Au instances. Gives up and exits with sys.exit() if an implicit<...>
  statement does not seem to define a 'status' trait, or a 'status2' trait when
  status2 processing is requested,
  Parameters:
  - options (of type _TdbEditOptions): the current options
  '''
  aus = list()
  for filestr in options.files:
    f = open(filestr, 'r')
    for lineindex, line in enumerate(f):
      mat = _IMPLICIT.search(line)
      if mat is not None:
        impl = [trait.strip() for trait in mat.group(1).split(';')]
        try: statusindex = impl.index('status')
        except ValueError: sys.exit('%s:%s: implicit statement does not specify \'status\'' % (filestr, lineindex + 1))
        try: status2index = impl.index('status2')
        except ValueError:
          if options.from2: sys.exit('%s:%s: implicit statement does not specify \'status2\'' % (filestr, lineindex + 1))
          else: status2index = -1
        continue # next line
      mat = _AU.search(line)
      if mat is not None:
        if status2index < 0: maxsplit = statusindex + 1
        else: maxsplit = max(statusindex, status2index) + 1
        au = [trait.strip() for trait in mat.group(1).split(';', maxsplit)]
        status = au[statusindex]
        if status2index < 0: status2 = None
        else: status2 = au[status2index]
        aus.append(_Au(filestr, lineindex, statusindex, status, status2index, status2))
        continue # next line
    f.close()
  return aus

def _change_aus(options, auids, aus):
  '''Performs the requested changes, in memory. Exits with sys.exit() if a
  requested AUID is not found or if a requested change is not valid for an AUID.
  Parameters:
  - options (of type _TdbEditOptions): the current options
  - auids (list of strings): the list of parsed AUIDs
  - aus (list of _Au instances): the list of AU entries
  '''
  errors = 0
  for auid in options.auids:
    # Check that the requested AUID is in the parsed files
    try: index = auids.index(auid)
    except ValueError:
      errors = errors + 1
      sys.stderr.write('%s is not represented in these files\n' % (auid,))
      continue # next change
    # Check that the status or status2 changes are valid
    au = aus[index]
    if options.from1 and au.status not in options.from1:
      errors = errors + 1
      sys.stderr.write('%s:%s: the status of %s \'%s\' is not one of %s\n' % (au.filestr, au.lineindex + 1, auid, au.status, options.from1))
      continue # next change
    if options.from2 and au.status2 not in options.from2:
      errors = errors + 1
      sys.stderr.write('%s:%s: the status2 of %s \'%s\' is not one of %s\n' % (au.filestr, au.lineindex + 1, auid, au.status2, options.from2))
      continue # next change
    # Perform requested changes if necessary
    if options.from1 and au.status != options.to1: au.status, au.changed = options.to1, True
    if options.from2 and au.status2 != options.to2: au.status2, au.changed = options.to2, True
  if errors > 0: sys.exit('Errors: %d (see above); exiting' % (errors,))

def _alter_files(options, aus):
  '''Rewrite files to reflect changes to AUs.
  Parameters:
  - options (of type _TdbEditOptions): the current options
  - auids (list of strings): the list of parsed AUIDs
  - aus (list of _Au instances): the list of AU entries
  '''
  # Reduce to list of changed AUS and list of affected files
  touch_aus = filter(lambda a: a.changed, aus)
  touch_files = list()
  for au in touch_aus:
    if au.filestr not in touch_files: touch_files.append(au.filestr)
  # Process each affected file
  for touch_file in touch_files:
    # Read file into memory
    f = open(touch_file, 'r')
    lines = f.readlines()
    f.close()
    # Copy into backup file
    fbak = open('%s.bak' % (touch_file,), 'w')
    fbak.writelines(lines)
    fbak.close()
    # Process AUs from that file
    for au in filter(lambda a: a.filestr == touch_file, touch_aus):
      # Reconstruct altered au<...> statement
      line = lines[au.lineindex]
      mat = _AU.search(line)
      aumid = mat.group(1).split(';')
      if options.from1: aumid[au.statusindex] = ' %s ' % (options.to1,)
      if options.from2: aumid[au.status2index] = ' %s ' % (options.to2,)
      line = line[:mat.start(1)] + ';'.join(aumid) + line[mat.end(1):]
      lines[au.lineindex] = line
    # Write out modified file
    f = open(touch_file, 'w')
    f.writelines(lines)
    f.close()

def _main():
  '''Main method.'''
  # Parse command line
  parser = _TdbEditOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _TdbEditOptions(parser, opts, args)
  # Get AUIDs and scan files for AU entries
  auids = _get_auids(options)
  aus = _build_au_index(options)
  if len(auids) != len(aus): sys.exit('Error: %d AUIDs represented in files, %d AUs found by scanning these files' % (len(auids), len(aus)))
  # Perform changes in memory
  _change_aus(options, auids, aus)
  # Translate changes into files
  _alter_files(options, aus)

# Main entry point
if __name__ == '__main__': _main()

