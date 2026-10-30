#!/usr/bin/env python3

'''Read-only survey for sul-dlss/lockss-engineering-tasks#753: lists the AUs
that are configured and active in a LOCKSS V1 au.txt but already exist in
V2 (migrated, deleted, then added again), and says which of them have
content in the V1 repository. Nothing in the config directory or the
repository is written, and V2, when asked for its AUIDs, is only read.'''

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
Usage: readded-migrated-aus.py --au-txt AU_TXT
                               (--v2-auids FILE | --host HOST --user USER[:PASS])
                               --repo DIR [--repo DIR ...] --out OUT_DIR
                               [--rs-port PORT] [--cfg-port PORT]
                               [--namespace NS] [--timeout SECONDS]
                               [--delete-with-content]

AU_TXT is the daemon's au.txt (in the config directory, e.g.
/cache0/gamma/config/au.txt). An AU is active unless its reserved.disabled
parameter is true.

With --host, the AUIDs V2 knows are fetched from HOST: the repository
service's AU list (GET /aus?namespace=NS, paginated) and the configuration
service's AU configurations (GET /aus), united, because an AU migrated
without content is listed only by the configuration service. The ports and
the namespace default to the daemon's migration settings,
org.lockss.v2.migrate.rs.port (24611), org.lockss.v2.migrate.cfg.port
(24612) and the namespace lockss; --rs-port, --cfg-port and --namespace
override them, and --timeout is the time allowed for each connection and
each read. --user names the V2 user and, after a colon, the password;
without the colon the password is prompted for. A password on the command
line is visible to the other users of the box in ps. The fetched list is
written to OUT_DIR/v2-auids.txt, to review it and to rerun the survey with
--v2-auids without fetching again. A failure of either service is an
error, a 404 included: the daemon takes a 404 from the repository as a
namespace with no AUs yet, but here a wrong host or port must not read as
nothing to clean up, and the list of one service alone would miss
candidates.

With --v2-auids, FILE lists the AUIDs V2 knows, one per line; blank lines
and lines starting with # are ignored. To make it by hand, ask the
configuration service's REST port on the migration host, GET /aus, which
is paginated: each page has "auConfigs", objects with an "auId" field, and
"pageInfo.continuationToken"; request ?continuationToken=TOKEN until it is
null. For the first page:

  curl -s -u USER:PASS http://HOST:24612/aus | jq -r '.auConfigs[].auId'

To be thorough, also append the repository service's list, GET
/aus?namespace=lockss on its REST port, paginated the same way with "auids"
in each page. Pass the union of the two lists; duplicates are harmless.

Plugin registry AUs (plugin org.lockss.plugin.RegistryPlugin) are internal
to each daemon, created from org.lockss.plugin.registries rather than
au.txt, and are never candidates; any found in au.txt or among the V2
AUIDs are counted and left out.

Each --repo DIR is a V1 repository root as listed in
org.lockss.platform.diskSpacePaths (e.g. /cache0/gamma). The AU directories
are the immediate subdirectories of DIR/cache; if DIR has no cache
subdirectory, DIR itself is taken as the cache directory. An AU directory is
identified by its #au_id_file; subdirectories without a readable one are
counted and skipped. DIR/MIGRATED, where deleted AUs are moved before
removal, is not scanned.

An AU has content if its AU directory has an entry whose name doesn't start
with # (the host directories). An AU directory that can't be listed is
reported as having content, with -1 entries, so that it is not put in the
journal file unless asked.

The candidates with content are left out of the journal file, for a
decision: V1 may have crawled them since they were migrated. With
--delete-with-content every candidate is put in it, content or not. The
journal only takes the AU out of au.txt. Unlike a migration deletion, it
leaves the AU directory in the repository, where nothing refers to it any
more; with-content.tsv says which directories those are, to remove by hand
once V2 is known to have what they hold.

Output in OUT_DIR, which must not be in a repository or in the directory
holding au.txt:

  candidates.tsv            every AU active in au.txt and present in FILE:
                            auid, repo_dir, has_content, content_entries.
                            repo_dir is the AU directory, several joined
                            with ; if the AU has more than one, empty if
                            none was found.
  with-content.tsv          the candidates with content. They need a
                            decision and are not put in the journal file,
                            unless --delete-with-content is given.
  au-journal-additions.csv  a DELETE record for each candidate without
                            content or without an AU directory, or, with
                            --delete-with-content, for every candidate, in
                            the format of the daemon's au-journal.txt. To
                            apply it: stop the daemon, append the file to
                            au-journal.txt in the config directory, start
                            the daemon. It removes those AUs from au.txt
                            before loading it and logs "Processed N AU
                            config journal entries".
  summary.txt               the counts also printed on stdout
  v2-auids.txt              with --host: the fetched AUIDs, one per line,
                            after two # lines saying where and when they
                            were fetched and how many each service listed.
                            Written as soon as the fetch is done, so that
                            it is kept if the repository scan then fails.

While the AU directories are scanned, a progress bar is drawn on stderr if
stderr is a terminal; it is left out when stderr is redirected.
'''

import argparse
import base64
import collections
import csv
import datetime
import getpass
import http.client
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

# From src/org/lockss/plugin/PluginManager.java: PARAM_AU_TREE is
# Configuration.PREFIX + "au" and AU_PARAM_DISABLED is AU_PARAM_RESERVED +
# ".disabled", where Configuration.PREFIX is "org.lockss." and
# AU_PARAM_RESERVED is "reserved".
AU_TREE_PREFIX = 'org.lockss.au.'
AU_PARAM_DISABLED = 'reserved.disabled'
# From src/org/lockss/repository/LockssRepositoryImpl.java: AU_ID_FILE,
# AU_ID_PROP and CACHE_ROOT_NAME.
AU_ID_FILE = '#au_id_file'
AU_ID_PROP = 'au.id'
CACHE_ROOT_NAME = 'cache'
# The action ConfigManager.processAuTxtJournal() applies as a deletion
JOURNAL_DELETE = 'DELETE'
# RegistryPlugin.PLUGIN_ID in src/org/lockss/plugin/RegistryPlugin.java is
# "org.lockss.plugin.RegistryPlugin"; this is it as a plugin key, with |
# for .  Registry AUs are internal to each daemon
# (PluginManager.initLoadablePluginRegistries() makes them from
# org.lockss.plugin.registries, not from au.txt) and are never candidates.
REGISTRY_PLUGIN_KEY = 'org|lockss|plugin|RegistryPlugin'
# From src/org/lockss/laaws/V2AuMover.java: DEFAULT_RS_PORT (24611) and
# DEFAULT_CFG_PORT (24612) are the defaults of PARAM_RS_PORT and
# PARAM_CFG_PORT, PREFIX + "rs.port" and PREFIX + "cfg.port", where PREFIX
# is MigrationManager.PREFIX, Configuration.PREFIX + "v2.migrate.", so
# org.lockss.v2.migrate.rs.port and org.lockss.v2.migrate.cfg.port;
# V2_DEFAULT_NAMESPACE is "lockss" and DEFAULT_READ_TIMEOUT is 1 hour.  The
# daemon reaches the services as new URL("http", hostName, port, "").
V2_DEFAULT_RS_PORT = 24611
V2_DEFAULT_CFG_PORT = 24612
V2_DEFAULT_NAMESPACE = 'lockss'
V2_DEFAULT_TIMEOUT = 3600
REPO_SERVICE = 'repository service'
CFG_SERVICE = 'configuration service'

# Where and how to fetch the V2 AUIDs; the fields are the parameters of
# fetch_v2_auids()
V2Host = collections.namedtuple(
  'V2Host', 'host user password cfg_port rs_port namespace timeout')

OUT_CANDIDATES = 'candidates.tsv'
OUT_WITH_CONTENT = 'with-content.tsv'
OUT_JOURNAL = 'au-journal-additions.csv'
OUT_SUMMARY = 'summary.txt'
OUT_V2_AUIDS = 'v2-auids.txt'
TSV_COLUMNS = ('auid', 'repo_dir', 'has_content', 'content_entries')
UNKNOWN_ENTRIES = -1


class SurveyError(Exception):
  '''A usage or IO error: the message is printed and the exit status is 2.'''


# parse_java_props() and its three helpers are copied from
# check-v1-repo.py, so that this script is a single file.

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


def parse_au_txt(data):
  '''Return (active, inactive, n_odd): the sets of AUIDs configured in
  au.txt, and the number of keys under org.lockss.au. that don't have the
  three parts.  Keys are org.lockss.au.<pluginKey>.<auKey>.<param>; the
  plugin key has | for . and the AU key is PropKeyEncoder output, so
  neither contains a dot.  The AUID is <pluginKey>&<auKey>.'''
  disabled = {}
  n_odd = 0
  for key, value in parse_java_props(data).items():
    if not key.startswith(AU_TREE_PREFIX):
      continue
    parts = key[len(AU_TREE_PREFIX):].split('.', 2)
    if len(parts) < 3 or not parts[0] or not parts[1]:
      n_odd += 1
      continue
    auid = parts[0] + '&' + parts[1]
    if parts[2] == AU_PARAM_DISABLED:
      disabled[auid] = value.strip().lower() == 'true'
    else:
      disabled.setdefault(auid, False)
  active = set(auid for auid, d in disabled.items() if not d)
  return active, set(disabled) - active, n_odd


def read_v2_auids(path):
  '''The AUIDs in the file, one per line; blank lines and lines starting
  with # are ignored.'''
  auids = set()
  with open(path, 'r', encoding='utf-8', errors='replace') as f:
    for line in f:
      s = line.strip()
      if s and not s.startswith('#'):
        auids.add(s)
  return auids


def _v2_get(service, url, auth, timeout):
  '''GET url with the Authorization header auth and return the decoded JSON
  body.  Any failure, a non-2xx status included, is a SurveyError naming
  the service and the URL; the credentials are in a header, never in the
  URL or in a message.'''
  req = urllib.request.Request(url, headers={'Authorization': 'Basic ' + auth,
                                             'Accept': 'application/json'})
  try:
    with urllib.request.urlopen(req, timeout=timeout) as resp:
      body = resp.read()
  except urllib.error.HTTPError as e:
    raise SurveyError('%s: GET %s: HTTP %d %s' % (service, url, e.code, e.reason))
  except urllib.error.URLError as e:
    raise SurveyError('%s: GET %s: %s' % (service, url, e.reason))
  except (OSError, http.client.HTTPException, ValueError) as e:
    raise SurveyError('%s: GET %s: %s' % (service, url, e))
  try:
    return json.loads(body.decode('utf-8'))
  except (UnicodeDecodeError, ValueError) as e:
    raise SurveyError('%s: GET %s: the response isn\'t JSON: %s'
                      % (service, url, e))


def fetch_v2_auids(host, user, password, cfg_port, rs_port, namespace,
                   timeout):
  '''The AUIDs V2 knows, as (auids, n_repo, n_cfg): the union of the
  repository service's AU list and the configuration service's AU
  configurations, and the number of AUIDs each service listed.  Both are
  asked with HTTP basic auth sent up front (urllib's auth handler would
  wait for a 401 challenge, one more round trip per request) and a
  timeout for each connection and read.  Any failure is a SurveyError:
  unlike the daemon, which takes a 404 from the repository as a namespace
  with no AUs yet (V2AuMover.getV2Aus), a wrong host or port here must not
  read as nothing to clean up, and the list of one service alone would
  under-report the candidates.'''
  auth = base64.b64encode(('%s:%s' % (user, password)).encode('utf-8')) \
    .decode('ascii')
  auids = set()
  # The repository service first, as the daemon does: GET
  # /aus?namespace=NS[&continuationToken=T], the query names of
  # AusApi.getAus() in src/org/lockss/laaws/api/rs/AusApi.java (namespace,
  # limit, continuationToken; the daemon passes no limit).  Each page is an
  # AuidPageInfo (src/org/lockss/laaws/model/rs/AuidPageInfo.java) with
  # "auids" and "pageInfo", whose "continuationToken"
  # (src/org/lockss/laaws/model/rs/PageInfo.java) is null on the last page.
  n_repo = 0
  for url, page in _v2_pages(REPO_SERVICE, 'http://%s:%d/aus' % (host, rs_port),
                             [('namespace', namespace)], 'auids', auth,
                             timeout):
    if not all(isinstance(a, str) for a in page['auids']):
      raise SurveyError('%s: GET %s: expected a list of strings in "auids"'
                        % (REPO_SERVICE, url))
    auids.update(page['auids'])
    n_repo += len(page['auids'])
  # The configuration service: GET /aus[?continuationToken=T], paginated
  # the same way since laaws-configservice issue 569 (the daemon's
  # cfgAusApiClient.getAllAuConfig(limit, continuationToken)): each page is
  # an AuConfigPageInfo with "auConfigs", a list of AuConfiguration
  # (src/org/lockss/laaws/model/cfg/AuConfiguration.java), each with
  # "auId", and "pageInfo".
  n_cfg = 0
  n_bad = 0
  for url, page in _v2_pages(CFG_SERVICE, 'http://%s:%d/aus' % (host, cfg_port),
                             [], 'auConfigs', auth, timeout):
    for auc in page['auConfigs']:
      auid = auc.get('auId') if isinstance(auc, dict) else None
      if isinstance(auid, str) and auid:
        auids.add(auid)
        n_cfg += 1
      else:
        n_bad += 1
  if n_bad:
    sys.stderr.write('%s: %d AU configurations without an auId, ignored\n'
                     % (CFG_SERVICE, n_bad))
  return auids, n_repo, n_cfg


def _v2_pages(service, base, query, items, auth, timeout):
  '''Iterate over the pages of a paginated V2 AU listing, yielding (url,
  page) for each: GET base with the query pairs, then with
  continuationToken added, until a page's "pageInfo" has no token (null,
  empty, absent, or no pageInfo at all).  Each page must be an object with
  a list under items; a token that repeats is an error, against an endless
  loop.  A progress line goes to stderr every 10 pages.'''
  n_items = 0
  pages = 0
  token = None
  while True:
    pairs = list(query)
    if token:
      pairs.append(('continuationToken', token))
    url = base + ('?' + urllib.parse.urlencode(pairs) if pairs else '')
    page = _v2_get(service, url, auth, timeout)
    if not isinstance(page, dict) or not isinstance(page.get(items), list):
      raise SurveyError('%s: GET %s: expected an object with a list in "%s"'
                        % (service, url, items))
    yield url, page
    n_items += len(page[items])
    pages += 1
    if pages % 10 == 0:
      sys.stderr.write('%s: %d AUs in %d pages\n' % (service, n_items, pages))
    info = page.get('pageInfo')
    prev = token
    token = info.get('continuationToken') if isinstance(info, dict) else None
    if not token:
      return
    if token == prev:
      raise SurveyError('%s: GET %s: the continuation token repeats'
                        % (service, url))


def write_v2_auids(path, auids, v2_host, n_repo, n_cfg):
  '''Write the fetched AUIDs, sorted, one per line, after two comment lines
  saying where and when they were fetched; read_v2_auids() reads it
  back.'''
  stamp = datetime.datetime.now(datetime.timezone.utc) \
    .strftime('%Y-%m-%dT%H:%M:%SZ')
  with open(path, 'w', encoding='utf-8') as f:
    f.write('# V2 AUIDs fetched from %s (configuration service port %d, '
            'repository service port %d, namespace %s) at %s\n'
            % (v2_host.host, v2_host.cfg_port, v2_host.rs_port,
               v2_host.namespace, stamp))
    f.write('# repository service: %d, configuration service: %d\n'
            % (n_repo, n_cfg))
    for auid in sorted(auids):
      f.write(auid + '\n')


def is_registry_auid(auid):
  '''True if the AUID's plugin, the part before the first &, is the plugin
  registry plugin.'''
  return auid.split('&', 1)[0] == REGISTRY_PLUGIN_KEY


def cache_dir(repo):
  '''The directory whose subdirectories are the AU directories.'''
  c = os.path.join(repo, CACHE_ROOT_NAME)
  return c if os.path.isdir(c) else repo


def count_content_entries(au_dir):
  '''The number of entries not starting with #, or UNKNOWN_ENTRIES if the
  directory can't be listed.'''
  try:
    return sum(1 for name in os.listdir(au_dir) if not name.startswith('#'))
  except OSError as e:
    sys.stderr.write('Can\'t list %s: %s\n' % (au_dir, e.strerror))
    return UNKNOWN_ENTRIES


class ProgressBar(object):
  '''A progress bar on stream, redrawn in place at most every interval
  seconds and completed with a newline by finish().  Nothing is drawn
  unless the stream is a terminal, so that redirected stderr only gets
  the messages.'''
  WIDTH = 40

  def __init__(self, label, total, stream=None, interval=0.1,
               clock=time.monotonic):
    self.label = label
    self.total = total
    self.stream = sys.stderr if stream is None else stream
    self.interval = interval
    self.clock = clock
    self.done = 0
    self.drawn = False
    self.last_draw = None
    isatty = getattr(self.stream, 'isatty', None)
    self.enabled = total > 0 and isatty is not None and isatty()

  def advance(self, n=1):
    self.done += n
    if not self.enabled:
      return
    now = self.clock()
    if self.last_draw is None or now - self.last_draw >= self.interval \
       or self.done >= self.total:
      self.draw()
      self.last_draw = now

  def draw(self):
    done = min(self.done, self.total)
    filled = self.WIDTH * done // self.total
    self.stream.write('\r[%s%s] %d/%d %s' % ('#' * filled,
                                            '.' * (self.WIDTH - filled),
                                            done, self.total, self.label))
    self.stream.flush()
    self.drawn = True

  def finish(self):
    if self.drawn:
      self.stream.write('\n')
      self.stream.flush()


def list_au_dirs(cache):
  '''The sorted subdirectories of cache, symbolic links left out.'''
  try:
    return sorted(e.path for e in os.scandir(cache)
                  if e.is_dir(follow_symlinks=False))
  except OSError as e:
    raise SurveyError('Can\'t read %s: %s' % (cache, e.strerror))


def scan_repo(cache, index, stats, progress=None, subdirs=None):
  '''Add the AU directories under cache to index: auid -> list of
  (au_dir, content_entries).  Updates stats['au_dirs'] and
  stats['dirs_skipped'].  subdirs is list_au_dirs(cache) if already
  listed; progress, a ProgressBar, is advanced once per directory.'''
  if subdirs is None:
    subdirs = list_au_dirs(cache)
  for au_dir in subdirs:
    try:
      auid = parse_java_props(read_bytes(os.path.join(au_dir, AU_ID_FILE))) \
        .get(AU_ID_PROP, '').strip()
    except (OSError, PropsError):
      auid = ''
    if auid:
      stats['au_dirs'] += 1
      index.setdefault(auid, []).append((au_dir,
                                         count_content_entries(au_dir)))
    else:
      stats['dirs_skipped'] += 1
    if progress is not None:
      progress.advance()


def candidate_rows(active, v2, index):
  '''One row per AU active in V1 and known to V2, sorted by AUID: a dict
  with the TSV_COLUMNS keys.'''
  rows = []
  for auid in sorted(active & v2):
    dirs = index.get(auid, [])
    counts = [n for _, n in dirs]
    if any(n == UNKNOWN_ENTRIES for n in counts):
      entries = UNKNOWN_ENTRIES
      has_content = 1
    else:
      entries = sum(counts)
      has_content = 1 if entries > 0 else 0
    rows.append({'auid': auid,
                 'repo_dir': ';'.join(d for d, _ in dirs),
                 'has_content': has_content,
                 'content_entries': entries})
  return rows


def write_tsv(path, rows):
  with open(path, 'w', newline='') as f:
    w = csv.DictWriter(f, TSV_COLUMNS, delimiter='\t', lineterminator='\n')
    w.writeheader()
    w.writerows(rows)


def write_journal(path, auids):
  '''DELETE records in the format of au-journal.txt: the csv module's
  default dialect matches commons-csv CSVFormat.DEFAULT.'''
  with open(path, 'w', newline='') as f:
    w = csv.writer(f)
    for auid in auids:
      w.writerow([JOURNAL_DELETE, auid])


def summary_lines(counts, delete_with_content=False):
  '''One line per count; the two per-service lines only when the V2
  AUIDs were fetched (counts has v2_repo and v2_cfg).  With
  delete_with_content the candidates with content are in the journal file
  too, and their line says so.'''
  labels = [
    ('active', 'AUs active in au.txt'),
    ('inactive', 'AUs inactive in au.txt'),
    ('v2', 'V2 AUIDs read'),
  ]
  if 'v2_repo' in counts:
    labels += [
      ('v2_repo', '  from the ' + REPO_SERVICE),
      ('v2_cfg', '  from the ' + CFG_SERVICE),
    ]
  labels += [
    ('candidates', 'Candidates (active in V1, present in V2)'),
    ('to_delete', '  without content, to delete'),
    ('no_dir', '    of which without an AU directory'),
    ('with_content', '  with content, to delete too' if delete_with_content
                     else '  with content, to decide'),
    ('registry', 'Plugin registry AUs left out'),
    ('au_dirs', 'AU directories scanned'),
    ('dirs_skipped', 'Directories skipped (no readable %s)' % AU_ID_FILE),
    ('multi_dir', 'AUIDs with more than one directory'),
  ]
  width = max(len(label) for _, label in labels) + 1
  return ['%-*s %d' % (width, label + ':', counts[key])
          for key, label in labels]


def is_within(child, parent):
  child = os.path.realpath(child)
  parent = os.path.realpath(parent)
  return child == parent or child.startswith(parent + os.sep)


def check_out_dir(out, au_txt, repos):
  if is_within(out, os.path.dirname(os.path.abspath(au_txt))):
    raise SurveyError('--out %s is in the directory holding %s'
                      % (out, au_txt))
  for repo in repos:
    if is_within(out, repo):
      raise SurveyError('--out %s is in repository %s' % (out, repo))


def check_inputs(au_txt, v2_file, repos, out):
  '''The local checks, run before any network access so that a typo fails
  at once.  v2_file is None when the V2 AUIDs are to be fetched.'''
  check_out_dir(out, au_txt, repos)
  for path in ([au_txt] if v2_file is None else [au_txt, v2_file]):
    if not os.path.isfile(path):
      raise SurveyError('not a file: %s' % path)
  for repo in repos:
    if not os.path.isdir(repo):
      raise SurveyError('not a directory: %s' % repo)


def survey(au_txt, repos, out, v2_file=None, v2_host=None,
           delete_with_content=False):
  '''Run the survey and return the summary lines.  The V2 AUIDs are read
  from v2_file or fetched from v2_host, a V2Host; exactly one is given.
  The fetched AUIDs are written to OUT_V2_AUIDS in out as soon as they
  arrive, so that a failure of the repository scan doesn't lose them.
  With delete_with_content the journal file lists every candidate, not
  only those without content.'''
  if (v2_file is None) == (v2_host is None):
    raise ValueError('exactly one of v2_file and v2_host is needed')
  check_inputs(au_txt, v2_file, repos, out)
  try:
    active, inactive, n_odd = parse_au_txt(read_bytes(au_txt))
  except (OSError, PropsError) as e:
    raise SurveyError('Can\'t read %s: %s' % (au_txt, e))
  fetched = None
  if v2_file is not None:
    try:
      v2 = read_v2_auids(v2_file)
    except OSError as e:
      raise SurveyError('Can\'t read %s: %s' % (v2_file, e))
  else:
    v2, n_repo, n_cfg = fetch_v2_auids(**v2_host._asdict())
    fetched = (n_repo, n_cfg)
    try:
      os.makedirs(out, exist_ok=True)
      write_v2_auids(os.path.join(out, OUT_V2_AUIDS), v2, v2_host,
                     n_repo, n_cfg)
    except OSError as e:
      raise SurveyError('Can\'t write to %s: %s' % (out, e.strerror))
  if n_odd:
    sys.stderr.write('%d keys under %s in %s not understood, ignored\n'
                     % (n_odd, AU_TREE_PREFIX, au_txt))
  n_active, n_inactive, n_v2 = len(active), len(inactive), len(v2)
  registry = set(auid for auid in active | inactive | v2
                 if is_registry_auid(auid))
  active -= registry
  inactive -= registry
  v2 -= registry
  index = {}
  stats = {'au_dirs': 0, 'dirs_skipped': 0}
  # List every repository first, so that the bar knows its total
  caches = [cache_dir(repo) for repo in repos]
  listed = [(cache, list_au_dirs(cache)) for cache in caches]
  progress = ProgressBar('AU directories scanned',
                         sum(len(subdirs) for _, subdirs in listed))
  try:
    for cache, subdirs in listed:
      scan_repo(cache, index, stats, progress, subdirs)
  finally:
    progress.finish()
  rows = candidate_rows(active, v2, index)
  with_content = [r for r in rows if r['has_content']]
  to_delete = [r for r in rows if not r['has_content']]
  counts = {
    'active': n_active,
    'inactive': n_inactive,
    'v2': n_v2,
    'candidates': len(rows),
    'to_delete': len(to_delete),
    'no_dir': sum(1 for r in to_delete if not r['repo_dir']),
    'with_content': len(with_content),
    'registry': len(registry),
    'au_dirs': stats['au_dirs'],
    'dirs_skipped': stats['dirs_skipped'],
    'multi_dir': sum(1 for dirs in index.values() if len(dirs) > 1),
  }
  if fetched is not None:
    counts['v2_repo'], counts['v2_cfg'] = fetched
  lines = summary_lines(counts, delete_with_content)
  journal = rows if delete_with_content else to_delete
  try:
    os.makedirs(out, exist_ok=True)
    write_tsv(os.path.join(out, OUT_CANDIDATES), rows)
    write_tsv(os.path.join(out, OUT_WITH_CONTENT), with_content)
    write_journal(os.path.join(out, OUT_JOURNAL),
                  [r['auid'] for r in journal])
    with open(os.path.join(out, OUT_SUMMARY), 'w') as f:
      f.write('\n'.join(lines) + '\n')
  except OSError as e:
    raise SurveyError('Can\'t write to %s: %s' % (out, e.strerror))
  return lines


def main():
  parser = argparse.ArgumentParser(
    description=__doc__,
    epilog=__tutorial__,
    formatter_class=argparse.RawDescriptionHelpFormatter)
  parser.add_argument('--au-txt', required=True, metavar='AU_TXT',
                      help='the V1 daemon\'s au.txt')
  source = parser.add_mutually_exclusive_group(required=True)
  source.add_argument('--v2-auids', metavar='FILE',
                      help='file of AUIDs known to V2, one per line')
  source.add_argument('--host', metavar='HOST',
                      help='V2 host to fetch the AUIDs from, asking its '
                      'repository and configuration services')
  parser.add_argument('--user', metavar='USER[:PASS]',
                      help='V2 user and password (with --host); the '
                      'password is prompted for if not given')
  parser.add_argument('--rs-port', type=int, default=V2_DEFAULT_RS_PORT,
                      metavar='PORT',
                      help='repository service REST port (with --host; '
                      'default %(default)s)')
  parser.add_argument('--cfg-port', type=int, default=V2_DEFAULT_CFG_PORT,
                      metavar='PORT',
                      help='configuration service REST port (with --host; '
                      'default %(default)s)')
  parser.add_argument('--namespace', default=V2_DEFAULT_NAMESPACE,
                      metavar='NS',
                      help='V2 namespace (with --host; default %(default)s)')
  parser.add_argument('--timeout', type=float, default=V2_DEFAULT_TIMEOUT,
                      metavar='SECONDS',
                      help='time allowed for each connection and each read '
                      '(with --host; default %(default)s)')
  parser.add_argument('--repo', action='append', default=[], metavar='DIR',
                      dest='repos',
                      help='V1 repository root (repeatable); its cache '
                      'subdirectory holds the AU directories')
  parser.add_argument('--out', required=True, metavar='OUT_DIR',
                      help='output directory, created if missing')
  parser.add_argument('--delete-with-content', action='store_true',
                      help='put the candidates with content in '
                      'au-journal-additions.csv too; their content stays '
                      'in the V1 repository')
  args = parser.parse_args()
  if not args.repos:
    parser.error('at least one --repo is required')
  if args.host and not args.user:
    parser.error('--host requires --user')
  if args.user and not args.host:
    parser.error('--user requires --host')
  try:
    # The local checks before the password prompt and any network access
    check_inputs(args.au_txt, args.v2_auids, args.repos, args.out)
    v2_host = None
    if args.host:
      user, sep, password = args.user.partition(':')
      if not user:
        parser.error('--user needs a user name')
      if not sep:
        try:
          password = getpass.getpass('Password for %s on %s: '
                                     % (user, args.host))
        except EOFError:
          raise SurveyError('No password given for %s' % user)
      v2_host = V2Host(args.host, user, password, args.cfg_port,
                       args.rs_port, args.namespace, args.timeout)
    for line in survey(args.au_txt, args.repos, args.out,
                       v2_file=args.v2_auids, v2_host=v2_host,
                       delete_with_content=args.delete_with_content):
      print(line)
  except SurveyError as e:
    sys.stderr.write('%s\n' % e)
    return 2
  return 0


if __name__ == '__main__':
  sys.exit(main())
