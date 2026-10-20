#!/usr/bin/env python3

'''Read-only scan of a LOCKSS V1 repository for the damage described in
sul-dlss/lockss-engineering-tasks#751: NUL-filled ("zeroed") files, a stray
version 0, and a current.props whose version number doesn't follow the
numbered versions.'''

__copyright__ = '''\
Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University
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

__tutorial__ = '''
Usage: check-v1-repo.py [-v] [--tsv FILE] [--seed N] [--timeout DURATION]
                        [--min-aus N] [--verify-content]
                        REPO_DIR [REPO_DIR ...]

REPO_DIR is any directory above one or more AU directories (an AU directory is
one containing #au_id_file or #au_state.xml), e.g. /cache0/gamma/cache or
/cache0/gamma/cache/b. Nothing is modified.

The summary lists, per AU, how many nodes have each finding. -v prints every
finding; --tsv writes every finding as a tab-separated row (au_dir, au_id,
severity, code, node, url, file, detail).

AUs (from all REPO_DIRs together) and the directories within each AU are
scanned in random order. The summary prints the seed; pass it to --seed to
repeat the same order.

--timeout DURATION (e.g. 900, 90s, 30m, 2h) stops the scan once DURATION has
elapsed: the directory being scanned is finished, the report is written, and
the script exits. The summary marks the AU that was cut short as INCOMPLETE
and counts the AUs that weren't reached.

--verify-content reads every content file in full (slower: the scan otherwise
reads only props files and the first and last block of content). It checks
each version against its x-lockss-checksum header and looks for all-NUL 4 KB
blocks, the signature of the partial zeroing seen in #751.

--min-aus N (default 10) keeps a too-short timeout from producing an empty
report: the scan doesn't stop until at least N AUs (or all of them, if fewer)
have been scanned completely, even if that runs past the timeout. The timeout
also applies while AU directories are being found, which proceeds in random
order and stops once N have been found; the not-scanned count is then
unknown.

DAMAGE findings:
  VERSION_ZERO            '0' or '0.props' exists. The detail compares '0' with
                          the highest numbered version and 'current', and says
                          whether 0.props is the rewritten zeroed file that
                          V1's inactive-node path leaves behind.
  CURRENT_VERSION_MISMATCH  current.props version != max(numbered) + 1.
  CURRENT_PROPS_UNUSABLE  'current' exists but current.props is missing,
                          zeroed, unparseable or has no positive version. V1
                          deactivates such a node and its next new version
                          renames 'current' to '0' (latent VERSION_ZERO).
  PROPS_ZEROED            A props file consisting entirely of NUL bytes.
  PROPS_NUL_BYTES         A props file containing some NUL bytes.
  PROPS_REWRITTEN_ZEROED  A props file containing \\u0000 escapes: a zeroed
                          props file that V1 loaded and stored again.
  PROPS_EMPTY             A zero-length props file.
  PROPS_UNPARSEABLE       java.util.Properties would reject the file.
  PROPS_NO_VERSION        Parses, but has no org.lockss.version.number.
  PROPS_VERSION_MISMATCH  N.props (or N.props-<date>) doesn't say version N.
  PROPS_MISSING           Numbered content file N has no N.props.
  CONTENT_ZEROED          A content file consisting entirely of NUL bytes.
  CONTENT_CHECKSUM_MISMATCH  (--verify-content) Content doesn't match its
                          x-lockss-checksum. The detail lists all-NUL 4 KB
                          block runs, if any.
  AUX_ZEROED              #node_props, #au_id_file, #au_state.xml, etc.
                          consisting entirely of (or containing) NUL bytes.

INFO findings:
  NODE_MARKED_INACTIVE, NODE_MARKED_DELETED  Flags in #node_props.
  INACTIVE_FILES          'inactive' or 'inactive.props' present.
  WAS_INACTIVE            Props with org.lockss.node.was.inactive=true.
  LEFTOVER_TEMP           'temp' or 'temp.props' present.
  FAULTY_RENAMED          *.ERROR files from an earlier V1 consistency check.
  CONTENT_EMPTY           A zero-length content file (often legitimate).
  ORPHAN_PROPS            N.props with no content file N.
  CONTENT_ZERO_BLOCKS     (--verify-content) No x-lockss-checksum to check,
                          but the content has all-NUL 4 KB blocks. Usually
                          damage; can be legitimate in some binary formats.
  SCAN_ERROR              A file vanished or couldn't be read mid-scan (e.g.
                          renamed by a running daemon); that directory's
                          findings may be incomplete.
'''

import argparse
import collections
import hashlib
import os
import random
import re
import sys
import time

CONTENT_DIR = '#content'
AU_MARKERS = ('#au_id_file', '#au_state.xml')
VERSION_KEY = 'org.lockss.version.number'
WAS_INACTIVE_KEY = 'org.lockss.node.was.inactive'
NODE_URL_KEY = 'x-lockss-node-url'
INACTIVE_KEY = 'node.content.isInactive'
DELETED_KEY = 'node.isDeleted'
FAULTY_EXT = '.ERROR'

DAMAGE = 'DAMAGE'
INFO = 'INFO'

SEVERITY = {
  'VERSION_ZERO': DAMAGE,
  'CURRENT_VERSION_MISMATCH': DAMAGE,
  'CURRENT_PROPS_UNUSABLE': DAMAGE,
  'PROPS_ZEROED': DAMAGE,
  'PROPS_NUL_BYTES': DAMAGE,
  'PROPS_REWRITTEN_ZEROED': DAMAGE,
  'PROPS_EMPTY': DAMAGE,
  'PROPS_UNPARSEABLE': DAMAGE,
  'PROPS_NO_VERSION': DAMAGE,
  'PROPS_VERSION_MISMATCH': DAMAGE,
  'PROPS_MISSING': DAMAGE,
  'CONTENT_ZEROED': DAMAGE,
  'CONTENT_CHECKSUM_MISMATCH': DAMAGE,
  'AUX_ZEROED': DAMAGE,
  'NODE_MARKED_INACTIVE': INFO,
  'NODE_MARKED_DELETED': INFO,
  'INACTIVE_FILES': INFO,
  'WAS_INACTIVE': INFO,
  'LEFTOVER_TEMP': INFO,
  'FAULTY_RENAMED': INFO,
  'CONTENT_EMPTY': INFO,
  'ORPHAN_PROPS': INFO,
  'CONTENT_ZERO_BLOCKS': INFO,
  'SCAN_ERROR': INFO,
}

NUMERIC_RE = re.compile(r'^[0-9]+$')
NUMBERED_PROPS_RE = re.compile(r'^([0-9]+)\.props(?:-([0-9]+))?$')
INT_RE = re.compile(r'^[+-]?[0-9]+$')
BLOCK = 65536
FS_BLOCK = 4096
CHECKSUM_KEY = 'x-lockss-checksum'


class PropsError(Exception):
  pass


def _unescape(s):
  '''java.util.Properties.loadConvert().'''
  if '\\' not in s:
    return s
  out = []
  i, n = 0, len(s)
  while i < n:
    c = s[i]
    i += 1
    if c != '\\':
      out.append(c)
      continue
    if i >= n:
      break
    c = s[i]
    i += 1
    if c == 'u':
      hexdigits = s[i:i + 4]
      if len(hexdigits) < 4 or not re.match(r'^[0-9a-fA-F]{4}$', hexdigits):
        raise PropsError('Malformed \\uxxxx encoding')
      out.append(chr(int(hexdigits, 16)))
      i += 4
    else:
      out.append({'t': '\t', 'r': '\r', 'n': '\n', 'f': '\f'}.get(c, c))
  return ''.join(out)


def _logical_lines(text):
  '''java.util.Properties.LineReader: drop comments and blank lines, join
  backslash continuations, strip leading whitespace.'''
  pending = None
  for raw in re.split(r'\r\n|\r|\n', text):
    s = raw.lstrip(' \t\f')
    if pending is None:
      if s == '' or s[0] in '#!':
        continue
      cur = s
    else:
      cur = pending + s
    trailing = len(cur) - len(cur.rstrip('\\'))
    if trailing % 2 == 1:
      pending = cur[:-1]
    else:
      pending = None
      yield cur
  if pending is not None:
    yield pending


def _split_key_value(line):
  '''java.util.Properties.load0() key/value separation.'''
  n = len(line)
  i = 0
  backslash = False
  has_sep = False
  value_start = n
  while i < n:
    c = line[i]
    if c in '=:' and not backslash:
      value_start = i + 1
      has_sep = True
      break
    if c in ' \t\f' and not backslash:
      value_start = i + 1
      break
    backslash = (not backslash) if c == '\\' else False
    i += 1
  key_len = i
  while value_start < n:
    c = line[value_start]
    if c not in ' \t\f':
      if not has_sep and c in '=:':
        has_sep = True
      else:
        break
    value_start += 1
  return _unescape(line[:key_len]), _unescape(line[value_start:])


def parse_java_props(data):
  '''Parse bytes as Properties.load(InputStream) would (ISO-8859-1).'''
  props = {}
  for line in _logical_lines(data.decode('latin-1')):
    key, value = _split_key_value(line)
    props[key] = value
  return props


def read_bytes(path):
  with open(path, 'rb') as f:
    return f.read()


def is_all_nul_file(path, size):
  '''True iff the (non-empty) file is all NUL. Checks the first and last
  block before reading the whole file.'''
  if size == 0:
    return False
  with open(path, 'rb') as f:
    head = f.read(BLOCK)
    if head.count(0) != len(head):
      return False
    if size > BLOCK:
      f.seek(max(BLOCK, size - BLOCK))
      tail = f.read()
      if tail.count(0) != len(tail):
        return False
      f.seek(BLOCK)
      while True:
        buf = f.read(BLOCK)
        if not buf:
          break
        if buf.count(0) != len(buf):
          return False
  return True


def digest_and_zero_blocks(path, algorithm):
  '''Return (hex digest or None, list of (start, end) byte ranges of runs
  of all-NUL FS_BLOCK-aligned blocks; the last block may be short).'''
  h = hashlib.new(algorithm) if algorithm else None
  runs = []
  offset = 0
  with open(path, 'rb') as f:
    while True:
      buf = f.read(BLOCK)
      if not buf:
        break
      if h:
        h.update(buf)
      for i in range(0, len(buf), FS_BLOCK):
        blk = buf[i:i + FS_BLOCK]
        if blk.count(0) == len(blk):
          start = offset + i
          if runs and runs[-1][1] == start:
            runs[-1] = (runs[-1][0], start + len(blk))
          else:
            runs.append((start, start + len(blk)))
      offset += len(buf)
  return (h.hexdigest() if h else None), runs


def checksum_algorithm(value):
  '''Map an x-lockss-checksum value ("SHA-1:HEX") to (hashlib name, hex).'''
  alg, _, hexval = value.partition(':')
  name = alg.replace('-', '').lower()
  if not hexval or name not in hashlib.algorithms_available:
    return None, None
  return name, hexval.strip().lower()


def count_nuls(path):
  total = 0
  with open(path, 'rb') as f:
    while True:
      buf = f.read(BLOCK)
      if not buf:
        return total
      total += buf.count(0)


def files_equal(a, b):
  if os.path.getsize(a) != os.path.getsize(b):
    return False
  with open(a, 'rb') as fa, open(b, 'rb') as fb:
    while True:
      ba = fa.read(BLOCK)
      bb = fb.read(BLOCK)
      if ba != bb:
        return False
      if not ba:
        return True


def fmt_mtime(t):
  return time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(t))


class PropsInfo(object):
  '''Classification of one props file.'''

  def __init__(self, path):
    self.path = path
    self.size = os.path.getsize(path)
    self.mtime = os.path.getmtime(path)
    self.problem = None    # finding code, or None if usable
    self.detail = ''
    self.props = None
    self.version = None
    self.header_url = None
    data = read_bytes(path)
    if self.size == 0:
      self.problem, self.detail = 'PROPS_EMPTY', '0 bytes'
      return
    nuls = data.count(0)
    if nuls == len(data):
      self.problem = 'PROPS_ZEROED'
      self.detail = '%d bytes, all NUL' % len(data)
      return
    m = re.match(br'#HTTP headers for (\S+)', data)
    if m:
      self.header_url = m.group(1).decode('latin-1')
    if nuls:
      self.problem = 'PROPS_NUL_BYTES'
      self.detail = '%d of %d bytes NUL' % (nuls, len(data))
      return
    try:
      self.props = parse_java_props(data)
    except PropsError as e:
      self.problem, self.detail = 'PROPS_UNPARSEABLE', str(e)
      return
    escaped = sum(k.count('\0') + v.count('\0') for k, v in self.props.items())
    if escaped:
      self.problem = 'PROPS_REWRITTEN_ZEROED'
      self.detail = '%d \\u0000 escapes' % escaped
      if self.props.get(WAS_INACTIVE_KEY) == 'true':
        self.detail += ', %s=true' % WAS_INACTIVE_KEY
      return
    ver = self.props.get(VERSION_KEY)
    if ver is None:
      self.problem, self.detail = 'PROPS_NO_VERSION', 'no %s' % VERSION_KEY
      return
    if not INT_RE.match(ver):
      self.problem = 'PROPS_UNPARSEABLE'
      self.detail = 'non-integer %s=%r' % (VERSION_KEY, ver)
      return
    self.version = int(ver)

  def usable(self):
    return self.problem is None

  def url(self):
    if self.props and NODE_URL_KEY in self.props:
      return self.props[NODE_URL_KEY]
    return self.header_url

  def describe(self):
    if self.problem is None:
      return 'version %d' % self.version
    return '%s (%s)' % (self.problem, self.detail)


def parse_duration(s):
  '''Seconds from '900', '90s', '30m' or '2h'.'''
  m = re.match(r'^\s*([0-9]+(?:\.[0-9]*)?)\s*([smh]?)\s*$', s)
  if not m:
    raise argparse.ArgumentTypeError('bad duration %r (e.g. 900, 30m, 2h)' % s)
  return float(m.group(1)) * {'': 1, 's': 1, 'm': 60, 'h': 3600}[m.group(2)]


class Scanner(object):

  def __init__(self, verbose, tsv, rng, timeout, min_aus, verify_content):
    self.verbose = verbose
    self.verify_content = verify_content
    self.tsv = tsv
    self.rng = rng
    self.timeout = timeout
    self.min_aus = min_aus
    self.start = time.monotonic()
    self.timed_out = False
    self.au_summaries = []
    self.n_au_not_scanned = 0
    self.discovery_incomplete = False
    self.ran_past_timeout = False

  def expired(self):
    if (not self.timed_out and self.timeout is not None
        and time.monotonic() - self.start >= self.timeout):
      self.timed_out = True
    return self.timed_out

  def should_stop(self, n_aus):
    '''True if the timeout has expired and n_aus already meets --min-aus.'''
    if not self.expired():
      return False
    if n_aus < self.min_aus:
      self.ran_past_timeout = True
      return False
    return True

  def emit(self, au, code, node, url, fname, detail):
    sev = SEVERITY[code]
    au['counts'][code] += 1
    au['nodes'][code].add(node)
    if sev == DAMAGE:
      au['damaged_nodes'].add(node)
    if fname and code in ('PROPS_ZEROED', 'CONTENT_ZEROED', 'AUX_ZEROED'):
      subdir = '' if code == 'AUX_ZEROED' else CONTENT_DIR
      try:
        mt = os.path.getmtime(os.path.join(au['dir'], node, subdir, fname))
        au['zeroed_mtimes'].append(mt)
      except OSError:
        pass
    row = (au['dir'], au['id'], sev, code, node, url or '', fname or '',
           detail)
    if self.verbose:
      print('  %-6s %-24s %s %s%s' % (
        sev, code, node or '.', ('[%s] ' % fname) if fname else '', detail))
    if self.tsv:
      self.tsv.write('\t'.join(s.replace('\t', ' ').replace('\n', ' ')
                               for s in row) + '\n')

  def find_au_dirs(self, root):
    for dirpath, dirnames, filenames in os.walk(root):
      if any(m in filenames for m in AU_MARKERS):
        dirnames[:] = []
        yield dirpath
      else:
        self.rng.shuffle(dirnames)

  def scan_roots(self, roots):
    au_dirs = []
    roots = list(roots)
    self.rng.shuffle(roots)
    for root in roots:
      found = False
      for au_dir in self.find_au_dirs(root):
        found = True
        au_dirs.append(au_dir)
        if self.should_stop(len(au_dirs)):
          self.discovery_incomplete = True
          break
      if self.discovery_incomplete:
        break
      if not found:
        sys.stderr.write('No AU directories (containing %s) under %s\n'
                         % (' or '.join(AU_MARKERS), root))
    self.rng.shuffle(au_dirs)
    for ix, au_dir in enumerate(au_dirs):
      if self.should_stop(len(self.au_summaries)):
        self.n_au_not_scanned = len(au_dirs) - ix
        break
      self.scan_au(au_dir)

  def read_au_id(self, au_dir):
    path = os.path.join(au_dir, '#au_id_file')
    if not os.path.isfile(path):
      return None
    try:
      return parse_java_props(read_bytes(path)).get('au.id')
    except PropsError:
      return None

  def scan_au(self, au_dir):
    au = {
      'dir': au_dir,
      'id': self.read_au_id(au_dir) or '',
      'counts': collections.Counter(),
      'nodes': collections.defaultdict(set),
      'damaged_nodes': set(),
      'zeroed_mtimes': [],
      'n_nodes': 0,
      'n_versions': 0,
      'incomplete': False,
    }
    if self.verbose:
      print('AU %s  %s' % (au_dir, au['id'] or '(no readable au.id)'))
    for dirpath, dirnames, filenames in os.walk(au_dir):
      if self.should_stop(len(self.au_summaries)):
        au['incomplete'] = True
        if self.verbose:
          print('  (timeout: AU scan incomplete)')
        break
      self.rng.shuffle(dirnames)
      rel = os.path.relpath(dirpath, au_dir)
      if rel == '.':
        rel = ''
      if os.path.basename(dirpath) == CONTENT_DIR:
        dirnames[:] = []
        au['n_nodes'] += 1
        node = os.path.dirname(rel)
        try:
          self.scan_content_dir(au, node, dirpath, filenames)
        except OSError as e:
          self.emit(au, 'SCAN_ERROR', node, None, CONTENT_DIR, str(e))
        continue
      try:
        self.scan_aux_files(au, rel, dirpath, filenames)
      except OSError as e:
        self.emit(au, 'SCAN_ERROR', rel, None, None, str(e))
    # Keep only counts, so memory doesn't grow with the number of nodes
    au['n_damaged'] = len(au.pop('damaged_nodes'))
    au['n_nodes_with'] = dict((c, len(n)) for c, n in au.pop('nodes').items())
    mtimes = au.pop('zeroed_mtimes')
    au['zeroed_range'] = (min(mtimes), max(mtimes)) if mtimes else None
    self.au_summaries.append(au)

  def scan_aux_files(self, au, rel, dirpath, filenames):
    '''#node_props, #au_id_file, #au_state.xml, #agreement, ...'''
    for fname in sorted(filenames):
      if not fname.startswith('#'):
        continue
      path = os.path.join(dirpath, fname)
      if os.path.islink(path) or not os.path.isfile(path):
        continue
      size = os.path.getsize(path)
      if size == 0:
        continue
      if fname.endswith(FAULTY_EXT):
        self.emit(au, 'FAULTY_RENAMED', rel, None, fname, '%d bytes' % size)
        continue
      if size > 4 * 1024 * 1024:
        continue
      data = read_bytes(path)
      nuls = data.count(0)
      if nuls == len(data):
        self.emit(au, 'AUX_ZEROED', rel, None, fname,
                  '%d bytes, all NUL' % size)
      elif nuls and fname in ('#node_props', '#au_id_file'):
        self.emit(au, 'AUX_ZEROED', rel, None, fname,
                  '%d of %d bytes NUL' % (nuls, size))
      if fname == '#node_props' and nuls == 0:
        try:
          np = parse_java_props(data)
        except PropsError:
          continue
        if np.get(INACTIVE_KEY) == 'true':
          self.emit(au, 'NODE_MARKED_INACTIVE', rel, None, fname,
                    '%s=true' % INACTIVE_KEY)
        if np.get(DELETED_KEY) == 'true':
          self.emit(au, 'NODE_MARKED_DELETED', rel, None, fname,
                    '%s=true' % DELETED_KEY)

  def scan_content_dir(self, au, node, cdir, filenames):
    names = set(filenames)
    numbered = sorted(int(n) for n in names if NUMERIC_RE.match(n))
    au['n_versions'] += len(numbered) + (1 if 'current' in names else 0)
    props = {}
    url = None

    def classify(fname):
      info = PropsInfo(os.path.join(cdir, fname))
      props[fname] = info
      return info

    # Classify every props file first so a node URL is known for reporting
    prop_names = sorted(n for n in names
                        if n.endswith('.props') or NUMBERED_PROPS_RE.match(n))
    for fname in prop_names:
      classify(fname)
    # Prefer x-lockss-node-url (has the real trailing slash) over the header
    urls = [i.url() for i in props.values() if i.props and NODE_URL_KEY in i.props]
    urls += [i.url() for i in props.values() if i.url()]
    if urls:
      url = urls[0]

    # Per-props-file findings
    for fname in prop_names:
      info = props[fname]
      if info.problem:
        self.emit(au, info.problem, node, url, fname, info.detail)
      elif info.props.get(WAS_INACTIVE_KEY) == 'true':
        self.emit(au, 'WAS_INACTIVE', node, url, fname,
                  '%s=true' % WAS_INACTIVE_KEY)
      m = NUMBERED_PROPS_RE.match(fname)
      if m:
        n = int(m.group(1))
        if info.usable() and info.version != n:
          self.emit(au, 'PROPS_VERSION_MISMATCH', node, url, fname,
                    'says version %d' % info.version)
        if n not in numbered and not m.group(2):
          self.emit(au, 'ORPHAN_PROPS', node, url, fname,
                    'no content file %d' % n)

    # Per-content-file findings
    for fname in sorted(names):
      if not (NUMERIC_RE.match(fname) or fname in ('current', 'inactive')):
        if fname.endswith(FAULTY_EXT):
          self.emit(au, 'FAULTY_RENAMED', node, url, fname, '')
        continue
      path = os.path.join(cdir, fname)
      size = os.path.getsize(path)
      if size == 0:
        self.emit(au, 'CONTENT_EMPTY', node, url, fname, '0 bytes')
      elif is_all_nul_file(path, size):
        self.emit(au, 'CONTENT_ZEROED', node, url, fname,
                  '%d bytes, all NUL' % size)
      if self.verify_content and size:
        self.verify_version(au, node, url, fname, path, size,
                            props.get(fname + '.props'))
      if NUMERIC_RE.match(fname) and (fname + '.props') not in names:
        self.emit(au, 'PROPS_MISSING', node, url, fname,
                  'no %s.props' % fname)

    if 'temp' in names or 'temp.props' in names:
      self.emit(au, 'LEFTOVER_TEMP', node, url, None,
                ', '.join(n for n in ('temp', 'temp.props') if n in names))
    if 'inactive' in names or 'inactive.props' in names:
      detail = ', '.join(n for n in ('inactive', 'inactive.props')
                         if n in names)
      if 'inactive.props' in props:
        detail += '; inactive.props: ' + props['inactive.props'].describe()
      self.emit(au, 'INACTIVE_FILES', node, url, None, detail)

    # current.props usability (the precondition for the version-0 clobber)
    cur = props.get('current.props')
    if 'current' in names:
      if cur is None:
        self.emit(au, 'CURRENT_PROPS_UNUSABLE', node, url, 'current.props',
                  'missing')
      elif not cur.usable():
        self.emit(au, 'CURRENT_PROPS_UNUSABLE', node, url, 'current.props',
                  cur.problem)
      elif cur.version <= 0:
        self.emit(au, 'CURRENT_PROPS_UNUSABLE', node, url, 'current.props',
                  'version %d' % cur.version)

    # current.props version vs. numbered versions
    if numbered and cur is not None and cur.usable():
      expected = numbered[-1] + 1
      if cur.version != expected:
        self.emit(au, 'CURRENT_VERSION_MISMATCH', node, url, 'current.props',
                  'version %d, expected %d (max numbered %d)'
                  % (cur.version, expected, numbered[-1]))

    # Version 0
    if 0 in numbered or '0.props' in names:
      self.emit(au, 'VERSION_ZERO', node, url, '0',
                self.describe_version_zero(cdir, names, numbered, props))

  def verify_version(self, au, node, url, fname, path, size, pinfo):
    alg = expected = None
    if pinfo is not None and pinfo.props and CHECKSUM_KEY in pinfo.props:
      alg, expected = checksum_algorithm(pinfo.props[CHECKSUM_KEY])
    actual, runs = digest_and_zero_blocks(path, alg)
    nul_detail = ', '.join('%d-%d' % r for r in runs)
    if alg:
      if actual != expected:
        self.emit(au, 'CONTENT_CHECKSUM_MISMATCH', node, url, fname,
                  '%d bytes; %s; NUL 4K blocks at %s' % (
                    size, pinfo.props[CHECKSUM_KEY], nul_detail or 'none'))
    elif runs:
      self.emit(au, 'CONTENT_ZERO_BLOCKS', node, url, fname,
                '%d bytes; NUL 4K blocks at %s' % (size, nul_detail))

  def describe_version_zero(self, cdir, names, numbered, props):
    parts = []
    zero = os.path.join(cdir, '0')
    if 0 in numbered:
      size = os.path.getsize(zero)
      nuls = count_nuls(zero)
      parts.append('0: %d bytes, %s, mtime %s' % (
        size,
        'all NUL' if size and nuls == size else '%d NUL bytes' % nuls,
        fmt_mtime(os.path.getmtime(zero))))
      others = [n for n in numbered if n > 0]
      if others:
        top = others[-1]
        same = files_equal(zero, os.path.join(cdir, str(top)))
        parts.append('%s %d' % ('identical to' if same else 'differs from',
                                top))
      if 'current' in names:
        same = files_equal(zero, os.path.join(cdir, 'current'))
        parts.append('%s current' % ('identical to' if same
                                     else 'differs from'))
    else:
      parts.append('no content file 0')
    zp = props.get('0.props')
    if zp is None:
      parts.append('0.props missing')
    else:
      parts.append('0.props: %s, mtime %s' % (zp.describe(),
                                              fmt_mtime(zp.mtime)))
    cur = props.get('current.props')
    if cur is not None:
      parts.append('current.props: %s, mtime %s' % (cur.describe(),
                                                    fmt_mtime(cur.mtime)))
    if (zp is not None and zp.problem == 'PROPS_REWRITTEN_ZEROED'
        and zp.props.get(WAS_INACTIVE_KEY) == 'true'
        and cur is not None and cur.version == 1):
      parts.append('matches inactive-node clobber (zeroed current.props)')
    elif zp is None and 0 in numbered and cur is not None \
         and cur.version == 1:
      parts.append('matches inactive-node clobber (missing current.props)')
    return '; '.join(parts)

  def print_summary(self, seed):
    total = collections.Counter()
    total_nodes = collections.Counter()
    elapsed = time.monotonic() - self.start
    print('Random order seed: %d' % seed)
    if self.timeout is not None:
      if not self.timed_out:
        outcome = 'finished'
      elif self.ran_past_timeout:
        outcome = 'TIMEOUT EXPIRED, continued to reach min AUs; stopped'
      else:
        outcome = 'STOPPED BY TIMEOUT'
      print('Timeout: %g s, min AUs: %d; %s after %.0f s' % (
        self.timeout, self.min_aus, outcome, elapsed))
    for au in self.au_summaries:
      print()
      print('AU %s%s' % (au['dir'],
                         '   INCOMPLETE (timeout)' if au['incomplete'] else ''))
      print('   id: %s' % (au['id'] or '(no readable au.id)'))
      print('   %d nodes, %d content versions, %d nodes with damage' % (
        au['n_nodes'], au['n_versions'], au['n_damaged']))
      if au['zeroed_range']:
        print('   zeroed files mtime range: %s .. %s' % (
          fmt_mtime(au['zeroed_range'][0]),
          fmt_mtime(au['zeroed_range'][1])))
      for code in SEVERITY:
        if au['counts'][code]:
          print('   %-6s %-24s %6d files/items in %6d nodes' % (
            SEVERITY[code], code, au['counts'][code], au['n_nodes_with'][code]))
          total[code] += au['counts'][code]
          total_nodes[code] += au['n_nodes_with'][code]
    print()
    n_incomplete = sum(1 for au in self.au_summaries if au['incomplete'])
    if self.discovery_incomplete:
      not_scanned = ('unknown number not scanned '
                     '(timeout expired while finding AUs)')
    else:
      not_scanned = '%d not scanned' % self.n_au_not_scanned
    print('TOTAL: %d AUs scanned%s, %s, %d nodes, %d nodes with damage' % (
      len(self.au_summaries),
      ' (%d incomplete)' % n_incomplete if n_incomplete else '',
      not_scanned,
      sum(au['n_nodes'] for au in self.au_summaries),
      sum(au['n_damaged'] for au in self.au_summaries)))
    for code in SEVERITY:
      if total[code]:
        print('   %-6s %-24s %6d files/items in %6d nodes' % (
          SEVERITY[code], code, total[code], total_nodes[code]))


def main():
  parser = argparse.ArgumentParser(
    description=__doc__,
    epilog=__tutorial__,
    formatter_class=argparse.RawDescriptionHelpFormatter)
  parser.add_argument('repo_dirs', nargs='+', metavar='REPO_DIR')
  parser.add_argument('-v', '--verbose', action='store_true',
                      help='print every finding')
  parser.add_argument('--tsv', metavar='FILE',
                      help='write every finding to FILE as TSV')
  parser.add_argument('--seed', type=int,
                      help='seed for the random scan order (default: random)')
  parser.add_argument('--timeout', type=parse_duration, metavar='DURATION',
                      help='stop after DURATION (e.g. 900, 30m, 2h), '
                      'finishing the current directory')
  parser.add_argument('--verify-content', action='store_true',
                      help='read all content: check x-lockss-checksum and '
                      'look for all-NUL 4 KB blocks')
  parser.add_argument('--min-aus', type=int, default=10, metavar='N',
                      help='with --timeout, scan at least N AUs completely '
                      '(default: %(default)s)')
  args = parser.parse_args()
  if args.min_aus < 0:
    parser.error('--min-aus must be >= 0')
  for root in args.repo_dirs:
    if not os.path.isdir(root):
      sys.stderr.write('Not a directory: %s\n' % root)
      return 2
  seed = args.seed
  if seed is None:
    seed = random.SystemRandom().randrange(1 << 32)
  tsv = None
  if args.tsv:
    tsv = open(args.tsv, 'w')
    tsv.write('au_dir\tau_id\tseverity\tcode\tnode\turl\tfile\tdetail\n')
  try:
    scanner = Scanner(args.verbose, tsv, random.Random(seed), args.timeout,
                      args.min_aus, args.verify_content)
    scanner.scan_roots(args.repo_dirs)
    scanner.print_summary(seed)
  finally:
    if tsv:
      tsv.close()
  return 0


if __name__ == '__main__':
  sys.exit(main())
