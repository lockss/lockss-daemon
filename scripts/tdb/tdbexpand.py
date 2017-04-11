#!/usr/bin/env python2

'''A rudimentary tool to add additional entries for serial publications in TDB
files.'''

__copyright__ = '''\
Copyright (c) 2000-2017, Board of Trustees of Leland Stanford Jr. University
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

__version__ = '0.1.0'

import cStringIO
from datetime import datetime
import optparse
import re
import subprocess

class _TdbExpandOptions(object):

    @staticmethod
    def make_parser():
        usage = '%prog {--interative|--non-interactive} [OPTIONS] FILE...'
        parser = optparse.OptionParser(version=__version__, usage=usage, description=__doc__)
        parser.add_option('--copyright', '-C', action='store_true', help='show copyright and exit')
        parser.add_option('--license', '-L', action='store_true', help='show license and exit')
        # Mode
        group = optparse.OptionGroup(parser, 'Mode')
        group.add_option('--interactive', action='store_true', help='Prompt user interactively')
        group.add_option('--non-interactive', action='store_true', help='Do not prompt user interactively (default)')
        parser.add_option_group(group)
        # Options
        group = optparse.OptionGroup(parser, 'Options')
        group.add_option('--end-year', type='int', help='last year in which to add entries (default: this year)')
        group.add_option('--start-year', type='int', help='first year in which to add entries (default: this year)')
        group.add_option('--stoppers', default='doesNotExist', help='AU statuses indicating the end of a run (comma-separated, default: %default)')
        group.add_option('--year-required', action='store_true', help='Only process AUs with a year marker')
        parser.add_option_group(group)
        return parser

    def __init__(self, parser, opts, args):
        super(_TdbExpandOptions, self).__init__()
        # --copyright, --license, (--help, --version already done)
        if any([opts.copyright, opts.license]):
            if opts.copyright: print copyright__
            elif opts.license: print __license__
            else: raise RuntimeError, 'internal error'
            sys.exit()
        # files
        if len(args) == 0: parser.error('at least one file is required')
        self.files = args[:]
        # interactive
        if opts.interactive and opts.non_interactive:
            parser.error('--interactive, --non-interactive are mutually exclusive')
        self.interactive = opts.interactive is True
        # end_year, start_year (integer)
        this_year = datetime.today().year
        self.start_year = opts.start_year or this_year
        self.end_year = opts.end_year or this_year
        if self.start_year > self.end_year:
            parser.error('Start year is later than end year')
        # stoppers
        self.stoppers = set(opts.stoppers.split(',') if len(opts.stoppers) > 0 else [])
        # year_required
        self.year_required = bool(opts.year_required)

class _Au(object):
  '''An internal class to represent an AU entry.'''

  def __init__(self, implicitmap, body, changed=False):
    '''
    Constructor.
    Parameters (all becoming fields, except 'body' which is split into the
    'values' array and can be regenerated with 'generate_body()'):
    - implicitmap (dict): the AU entry's implicit<...> specification, mapping
    from string (key in the implicit<...> statement) to integer (zero-based
    index of the key in the implicit<...> statement)
    - body (string): the AU entry's textual body
    - changed (boolean): a "dirty bit" set to True to indicate the entry has
    been modified (default: False)
    '''
    super(_Au, self).__init__()
    # implicitmap/changed
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
      raise KeyError, '%s' % (field,)
    if val.strip() != self.values[fieldindex].strip():
      self.values[fieldindex] = ' %s ' % (val,)
      self.changed = True

_AUID = 0
_FILE = 1
_LINE = 2
_TITLE = 3
_NAME = 4
_YEAR = 5
_VOLUME = 6
_PYEAR = 7
_PVOLUME = 8
_PVOLNAME = 9
_IYEAR = 10
_IVOL = 11

def _tdbout(options):
    tdbout = 'scripts/tdb/tdbout'
    cmd = [tdbout, '--tsv=auid,file,line,title,name,year,volume,param[year],param[volume],param[volume_name]']
    cmd.extend(options.files)
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    (out, err) = proc.communicate() # out and err are (potentially huge) strings
    if proc.returncode != 0:
        sys.exit('%s exited with error code %d: %s' % (tdbout, proc.returncode, err))
    ret = [line.strip().split('\t') for line in cStringIO.StringIO(out)]
    for auentry in ret:
        auentry[_LINE] = int(auentry[_LINE])
        auentry.append(_select_year(auentry))
        auentry.append(_select_volume(auentry))
    return ret

def _parse_year(yearstr):
    year1, dash, year2 = yearstr.partition('-')
    return (int(year1), int(year2) if dash == '-' else None) # int constructor raises ValueError

def _select_year(auentry):
    for choice in [_YEAR, _PYEAR]:
        if len(auentry[choice]) > 0:
            return _parse_year(auentry[choice])
    return None

def _select_volume(auentry):
    for choice in [_VOLUME, _PVOLUME, _PVOLNAME]:
        if len(auentry[choice]) > 0:
            return _parse_year(auentry[choice])
    return None

# A regular expression to match implicit<...> lines
# - Group 1: semicolon-separated body of the implicit<...> statement
_IMPLICIT = re.compile(r'^[ \t]*implicit[ \t]*<(.*)>[ \t]*(#.*)?$')

# A regular expression to match au<...> lines
# - Group 1: semicolon-separated body of the au<...> statement
_AU = re.compile(r'^[ \t]*au[ \t]*<(.*)>[ \t]*(#.*)?$')

def _recognize(options, aus):
    aindex = 0
    errors = 0
    for fstr in options.files:
        continue_outer = False
        with open(fstr, 'r') as f:
            for lineindex, line in enumerate(f):
                mat = _IMPLICIT.search(line)
                if mat is not None:
                    impl = [trait.strip() for trait in mat.group(1).split(';')]
                    implmap = dict([(x, i) for i, x in enumerate(impl)])
                    if 'name' not in implmap:
                        errors = errors + 1
                        sys.stderr.write('%s:%s: implicit statement does not specify \'name\'\n' % (fstr, lineindex + 1))
                        continue_outer = True # next file
                        break
                    if 'status' not in implmap:
                        errors = errors + 1
                        sys.stderr.write('%s:%s: implicit statement does not specify \'status\'\n' % (fstr, lineindex + 1))
                        continue_outer = True # next file
                        break
                    continue # next line
                if aindex < len(aus) and fstr == aus[aindex][1] and lineindex + 1 == aus[aindex][2]:
                    mat = _AU.search(line)
                    if mat is None:
                        errors = errors + 1
                        sys.stderr.write('%s:%s: text recognizer does not match definition for %s\n' % (fstr, lineindex + 1, aus[aindex][0]))
                        continue_outer = True # next file
                        break
                    au = _Au(implmap, mat.group(1))
                    aus[aindex].append(au)
                    aindex = aindex + 1
                    continue # next line
            if continue_outer: continue # next file
    if len(aus) != aindex:
        errors = errors + 1
        sys.stderr.write('error: tdbout parsed %d AU declarations but tdbedit found %d\n' % (len(aus), aindex))
    if errors > 0:
        sys.exit('%d %s; exiting' % (errors, 'error' if errors == 1 else 'errors'))

def _find_ranges(options, aus):
    ''''''
    ranges = list()
    aindex = len(aus) - 1
    while aindex >= 0:
        auentry = aus[aindex]
        if auentry[-1].get_status() in options.stoppers:
            while aindex >= 0 and aus[aindex][_TITLE] == auentry[_TITLE]:
                aindex = aindex - 1
            continue
        ranges.append([auentry])
        aindex = aindex - 1
        while aindex >= 0 and aus[aindex][_TITLE] == ranges[-1][-1][_TITLE]:
            ranges[-1].append(aus[aindex])
            if (aus[aindex][_IYEAR] and aus[aindex][_IYEAR] > ranges[-1][-1][_IYEAR]):
                break
            if (aus[aindex][_IYEAR] is None and aus[aindex][_IVOL] > ranges[-1][-1][_IVOL]):
                break
            aindex = aindex - 1
        while aindex >= 0 and aus[aindex][_TITLE] == ranges[-1][-1][_TITLE]:
            aindex = aindex - 1
    ranges.reverse()
    ###DEBUG ###FIXME
    for range in ranges:
        for auentry in range:
            print auentry[-1].generate_body()
        print
    return ranges

def _analyze_ranges(options, aus, ranges):
    for range in ranges:
        print ('>>>>>>>', _analyze_range(options, aus, range)) ###FIXME

def _analyze_range(options, aus, range):
    latestau = range[0]
    latestyear = latestau[_IYEAR]
    if latestyear is None:
        # Does not have years
        return ('error', 'no years')
    if latestyear[1] is None:
        # Ends with single year
        n = 1
        while n < len(range) and range[n][_IYEAR] == latestyear:
            n = n + 1
        if n == len(range):
            # Only one year to draw from
            if options.interactive and not _yesno('Only %d %s for %d to draw from; add %d %s per year?' % (n, 'entry' if n == 1 else 'entries', latestyear[0], n, 'entry' if n == 1 else 'entries'), range):
                return ('warning', None) ###FIXME
            else:
                return ('single', n, 0)
        # More than one year to draw from
        prevyear = range[n][_IYEAR]
        if (prevyear[1] is None and prevyear[0] != latestyear[0] - 1) \
                or (prevyear[1] is not None and (prevyear[1] != latestyear[0] or prevyear[0] != latestyear[0] - 1)):
            # Preceding year out of sequence
            return ('error', 'invalid progression')
        # Preceding year in sequence
        if prevyear[1] is None:
            # Preceding year is single year
            p = n
            while p < len(range) and range[p][_IYEAR] == prevyear:
                p = p + 1
            if p != len(range) and (range[p][_IYEAR][1] is None or range[p][_IYEAR][0] != prevyear[0] - 1):
                # Preceding year preceded by single year out of sequence or double year
                return ('error', 'invalid progression')
            # Preceding year preceded by nothing or single year in sequence
            if n == p - n:
                # Sequence of single years with stride n
                return ('single', n, 0)
            if n < p - n:
                # Sequence of single years with stride n and p already in
                if options.interactive and not _yesno('Add %d %s for %d then %d %s per year?' % (p - n - n, 'entry' if n == 1 else 'entries', latestyear[0], p - n, 'entry' if n == 1 else 'entries'), range):
                    return ('warning', None) ###FIXME
                else:
                    return ('single', p - n, n)
            # n > p - n
            if p == len(range):
                # Only two years to draw from, and fewer of the preceding year than the latest
                if options.interactive and not _yesno('Only %d %s for %d and %d %s for %d to draw from; add %d %s per year?' % (p - n, 'entry' if n == 1 else 'entries', prevyear[0], n, 'entry' if n == 1 else 'entries', latestyear[0], n, 'entry' if n == 1 else 'entries'), range):
                    return ('warning', None) ###FIXME
                else:
                    return ('single', n, 0)
            # Fewer of the preceding year than of the latest year
            return ('error', 'invalid progression')
    return ('not yet implemented', None)

def _yesno(st, range=None):
    if range:
        for entry in reversed(range):
            print '%s:%d: au <%s>' % (entry[_FILE], entry[_LINE], entry[-1].generate_body())
        print
    x = raw_input(st + ' [Yn] ')
    return not x.lower().startswith('n')

def _main():
    # Parse command line
    parser = _TdbExpandOptions.make_parser()
    (opts, args) = parser.parse_args()
    options = _TdbExpandOptions(parser, opts, args)
    # Get AUIDs and scan files for AU entries
    aus = _tdbout(options)
    _recognize(options, aus)
    ranges = _find_ranges(options, aus)
    _analyze_ranges(options, aus, ranges)

# Main entry point
if __name__ == '__main__': _main()

