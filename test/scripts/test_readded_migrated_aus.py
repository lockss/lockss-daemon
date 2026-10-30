#!/usr/bin/env python3

'''Tests for readded-migrated-aus.py.
Run: python3 -I test/scripts/test_readded_migrated_aus.py'''

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

import base64
import contextlib
import csv
import http.server
import importlib.util
import io
import json
import os
import shutil
import socket
import stat
import subprocess
import sys
import tempfile
import threading
import unittest

SCRIPT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                      'readded-migrated-aus.py')

_spec = importlib.util.spec_from_file_location('readded_migrated_aus', SCRIPT)
rma = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(rma)

PLUGIN = 'org|lockss|plugin|highwire|HighWirePressH20Plugin'
AUID_A = PLUGIN + '&base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~1'
AUID_B = PLUGIN + '&base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~2'
AUID_C = 'org|lockss|plugin|pensoft|PensoftPlugin&base_url~http%3A%2F%2Fzookeys%2Epensoft%2Enet%2F&journal_id~zookeys&year~2013'
AUID_D = PLUGIN + '&base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~4'
AUID_E = PLUGIN + '&base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~5'
AUID_F = PLUGIN + '&base_url~http%3A%2F%2Fwww%2Eexample%2Ecom%2F&volume_name~6'
REGISTRY = 'org|lockss|plugin|RegistryPlugin'
AUID_R1 = REGISTRY + '&base_url~http%3A%2F%2Fprops%2Elockss%2Eorg%3A8001%2Fplugins%2F'
AUID_R2 = REGISTRY + '&base_url~http%3A%2F%2Fprops%2Elockss%2Eorg%3A8001%2Fv2plugins%2F'


def au_txt_entry(auid, params):
  '''The au.txt lines for one AU: the AUID's first & becomes a dot
  (PluginManager.configKeyFromAuId).'''
  key = 'org.lockss.au.' + auid.replace('&', '.', 1)
  return ''.join('%s.%s=%s\n' % (key, p, v) for p, v in params)


def au_txt(entries):
  return ('#AU Configuration\n#Mon Jan 01 00:00:00 PST 2026\n'
          + ''.join(entries)).encode('latin-1')


def make_au_dir(cache, name, auid=None, hosts=(), id_file=True):
  d = os.path.join(cache, name)
  os.makedirs(d)
  if id_file and auid is not None:
    with open(os.path.join(d, '#au_id_file'), 'w') as f:
      f.write('#AU id\n#Mon Jan 01 00:00:00 PST 2026\nau.id=%s\n' % auid)
  for aux in ('#au_state.xml', '#node_props'):
    with open(os.path.join(d, aux), 'w') as f:
      f.write('x\n')
  for host in hosts:
    os.makedirs(os.path.join(d, host, 'index.html', '#content'))
  return d


def run_script(args):
  res = subprocess.run([sys.executable, '-I', SCRIPT] + args,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                       universal_newlines=True)
  return res


def read_tsv(path):
  with open(path, newline='') as f:
    return list(csv.DictReader(f, delimiter='\t'))


class FakeService(object):
  '''An HTTP server on 127.0.0.1 standing in for a V2 service: answers each
  GET with the next of its responses, (status, body), and records each
  request as (path, headers), the header names in lower case.'''

  def __init__(self):
    self.responses = []
    self.requests = []
    service = self

    class Handler(http.server.BaseHTTPRequestHandler):

      def do_GET(self):
        service.requests.append(
          (self.path, dict((k.lower(), v) for k, v in self.headers.items())))
        if service.responses:
          status, body = service.responses.pop(0)
        else:
          status, body = 500, 'no response set'
        if not isinstance(body, bytes):
          body = body.encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

      def log_message(self, format, *args):
        pass

    self.server = http.server.HTTPServer(('127.0.0.1', 0), Handler)
    self.port = self.server.server_address[1]
    # A short poll interval, so that shutdown() doesn't wait the default
    # half second per server
    self.thread = threading.Thread(target=self.server.serve_forever,
                                   kwargs={'poll_interval': 0.05})
    self.thread.daemon = True
    self.thread.start()

  def pages(self, *pages):
    '''Queue repository service pages, each (auids, continuation token).'''
    for auids, token in pages:
      self.responses.append((200, json.dumps(
        {'auids': auids, 'pageInfo': {'continuationToken': token}})))

  def aus(self, *pages):
    '''Queue configuration service pages, each (auids, continuation
    token), of AU configurations for auids.'''
    for auids, token in pages:
      self.responses.append((200, json.dumps(
        {'auConfigs': [{'auId': auid, 'auConfig': {}} for auid in auids],
         'pageInfo': {'continuationToken': token}})))

  def close(self):
    self.server.shutdown()
    self.server.server_close()
    self.thread.join()


def free_port():
  '''A port nothing listens on.'''
  s = socket.socket()
  s.bind(('127.0.0.1', 0))
  port = s.getsockname()[1]
  s.close()
  return port


class TestParseAuTxt(unittest.TestCase):

  def test_active_and_disabled(self):
    data = au_txt([
      au_txt_entry(AUID_A, [('base_url', 'http\\://www.example.com/'),
                            ('volume_name', '1'),
                            ('reserved.disabled', 'false'),
                            ('reserved.repository', 'local\\:/cache0')]),
      au_txt_entry(AUID_B, [('base_url', 'http\\://www.example.com/'),
                            ('reserved.disabled', 'true')]),
      au_txt_entry(AUID_C, [('reserved.disabled', 'True'),
                            ('journal_id', 'zookeys')]),
      au_txt_entry(AUID_D, [('base_url', 'http\\://www.example.com/')]),
    ])
    active, inactive, n_odd = rma.parse_au_txt(data)
    self.assertEqual({AUID_A, AUID_D}, active)
    self.assertEqual({AUID_B, AUID_C}, inactive)
    self.assertEqual(0, n_odd)

  def test_disabled_key_before_other_keys(self):
    data = au_txt([au_txt_entry(AUID_A, [('reserved.disabled', 'true'),
                                         ('volume_name', '1')])])
    active, inactive, n_odd = rma.parse_au_txt(data)
    self.assertEqual(set(), active)
    self.assertEqual({AUID_A}, inactive)

  def test_other_keys_and_odd_keys_ignored(self):
    data = au_txt([
      'org.lockss.foo=1\n',
      'org.lockss.au.nokey=1\n',
      'org.lockss.au.%s=1\n' % AUID_A.replace('&', '.', 1),
      au_txt_entry(AUID_A, [('volume_name', '1')]),
    ])
    active, inactive, n_odd = rma.parse_au_txt(data)
    self.assertEqual({AUID_A}, active)
    self.assertEqual(set(), inactive)
    self.assertEqual(2, n_odd)

  def test_empty(self):
    self.assertEqual((set(), set(), 0), rma.parse_au_txt(b''))


class TestReadV2Auids(unittest.TestCase):

  def setUp(self):
    self.tmp = tempfile.mkdtemp()

  def tearDown(self):
    shutil.rmtree(self.tmp)

  def test_blank_and_comment_lines(self):
    path = os.path.join(self.tmp, 'v2.txt')
    with open(path, 'w') as f:
      f.write('# from curl\n\n  %s  \n%s\n%s\n   \n' % (AUID_A, AUID_B, AUID_A))
    self.assertEqual({AUID_A, AUID_B}, rma.read_v2_auids(path))


class TestFetchV2Auids(unittest.TestCase):

  def setUp(self):
    self.repo = FakeService()
    self.cfg = FakeService()

  def tearDown(self):
    self.repo.close()
    self.cfg.close()

  def fetch(self, user='u', password='p', namespace='lockss', rs_port=None):
    return rma.fetch_v2_auids('127.0.0.1', user, password, self.cfg.port,
                              rs_port or self.repo.port, namespace, 5)

  def test_pages_union_and_headers(self):
    self.repo.pages(([AUID_A, AUID_B], 'a b+c/='),
                    ([AUID_B, AUID_C], 't2'),
                    ([AUID_D], None))
    self.cfg.aus(([AUID_A, AUID_E], 'c 2'), ([AUID_C, AUID_A], None))
    auids, n_repo, n_cfg = self.fetch(user='alice', password='s:cr=t',
                                      namespace='ns1')
    self.assertEqual({AUID_A, AUID_B, AUID_C, AUID_D, AUID_E}, auids)
    self.assertEqual((5, 4), (n_repo, n_cfg))
    self.assertEqual(['/aus?namespace=ns1',
                      '/aus?namespace=ns1&continuationToken=a+b%2Bc%2F%3D',
                      '/aus?namespace=ns1&continuationToken=t2'],
                     [p for p, _ in self.repo.requests])
    self.assertEqual(['/aus', '/aus?continuationToken=c+2'],
                     [p for p, _ in self.cfg.requests])
    auth = 'Basic ' + base64.b64encode(b'alice:s:cr=t').decode('ascii')
    for _, headers in self.repo.requests + self.cfg.requests:
      self.assertEqual(auth, headers['authorization'])
      self.assertEqual('application/json', headers['accept'])

  def test_page_without_page_info_is_last(self):
    self.repo.responses.append((200, json.dumps({'auids': [AUID_A]})))
    self.repo.responses.append((200, json.dumps({'auids': [AUID_B],
                                                 'pageInfo': None})))
    self.cfg.aus(([], None))
    self.assertEqual(({AUID_A}, 1, 0), self.fetch())
    self.assertEqual(1, len(self.repo.requests))

  def test_progress_every_ten_pages(self):
    self.repo.pages(*([([AUID_A], 't%d' % i) for i in range(9)]
                      + [([AUID_B], None)]))
    self.cfg.aus(([], None))
    err = io.StringIO()
    with contextlib.redirect_stderr(err):
      auids, n_repo, n_cfg = self.fetch()
    self.assertEqual(({AUID_A, AUID_B}, 10, 0), (auids, n_repo, n_cfg))
    self.assertIn('repository service: 10 AUs in 10 pages', err.getvalue())

  def test_repeated_token(self):
    self.repo.pages(([AUID_A], 't'), ([AUID_B], 't'))
    with self.assertRaises(rma.SurveyError) as cm:
      self.fetch()
    self.assertIn('repeats', str(cm.exception))

  def test_401(self):
    self.repo.responses.append((401, '{"error": "Unauthorized"}'))
    with self.assertRaises(rma.SurveyError) as cm:
      self.fetch(user='alice', password='secret')
    msg = str(cm.exception)
    self.assertIn('repository service', msg)
    self.assertIn('401', msg)
    self.assertIn('http://127.0.0.1:%d/aus?namespace=lockss' % self.repo.port,
                  msg)
    self.assertNotIn('alice', msg)
    self.assertNotIn('secret', msg)
    self.assertEqual([], self.cfg.requests)

  def test_repo_404(self):
    self.repo.responses.append((404, '{"error": "no namespace"}'))
    with self.assertRaises(rma.SurveyError) as cm:
      self.fetch()
    self.assertIn('404', str(cm.exception))
    self.assertEqual([], self.cfg.requests)

  def test_cfg_500_after_good_repo(self):
    self.repo.pages(([AUID_A], None))
    self.cfg.responses.append((500, 'boom'))
    with self.assertRaises(rma.SurveyError) as cm:
      self.fetch()
    msg = str(cm.exception)
    self.assertIn('configuration service', msg)
    self.assertIn('500', msg)
    self.assertIn('http://127.0.0.1:%d/aus' % self.cfg.port, msg)
    self.assertEqual(1, len(self.repo.requests))

  def test_connection_refused(self):
    port = free_port()
    with self.assertRaises(rma.SurveyError) as cm:
      self.fetch(rs_port=port)
    msg = str(cm.exception)
    self.assertIn('repository service', msg)
    self.assertIn(':%d/' % port, msg)
    self.assertEqual([], self.cfg.requests)

  def test_bad_json(self):
    for status, body in ((200, 'not json'), (200, '[]'),
                         (200, '{"auids": "x"}'),
                         (200, '{"auids": [1]}')):
      self.repo.responses.append((status, body))
      with self.assertRaises(rma.SurveyError) as cm:
        self.fetch()
      self.assertIn('repository service', str(cm.exception))
    self.assertEqual([], self.cfg.requests)
    for body in ('[{"auId": "no longer a plain array"}]',
                 '{"auConfigs": {}}'):
      self.repo.pages(([AUID_A], None))
      self.cfg.responses.append((200, body))
      with self.assertRaises(rma.SurveyError) as cm:
        self.fetch()
      self.assertIn('auConfigs', str(cm.exception))
    # A repeated token in the configuration service too
    self.repo.pages(([AUID_A], None))
    self.cfg.aus(([AUID_A], 'c'), ([AUID_B], 'c'))
    with self.assertRaises(rma.SurveyError) as cm:
      self.fetch()
    self.assertIn('configuration service', str(cm.exception))
    self.assertIn('repeats', str(cm.exception))

  def test_cfg_entries_without_auid_skipped(self):
    self.repo.pages(([], None))
    self.cfg.responses.append((200, json.dumps({'auConfigs': [
      {'auId': AUID_A}, {'x': 1}, {'auId': ''}, 'str', {'auId': AUID_B},
      {'auId': 5}]})))
    err = io.StringIO()
    with contextlib.redirect_stderr(err):
      auids, n_repo, n_cfg = self.fetch()
    self.assertEqual(({AUID_A, AUID_B}, 0, 2), (auids, n_repo, n_cfg))
    self.assertIn('4 AU configurations without an auId', err.getvalue())


class TestIsRegistryAuid(unittest.TestCase):

  def test_registry(self):
    self.assertTrue(rma.is_registry_auid(AUID_R1))
    self.assertTrue(rma.is_registry_auid(REGISTRY + '&a~1'))

  def test_not_registry(self):
    self.assertFalse(rma.is_registry_auid(AUID_A))
    self.assertFalse(rma.is_registry_auid(REGISTRY + 'X&a~1'))
    self.assertFalse(rma.is_registry_auid('org|lockss|plugin|Registry&a~1'))
    self.assertFalse(rma.is_registry_auid(REGISTRY.replace('|', '.') + '&a~1'))
    self.assertFalse(rma.is_registry_auid(''))


class TestScanRepo(unittest.TestCase):

  def setUp(self):
    self.tmp = tempfile.mkdtemp()

  def tearDown(self):
    for root, dirs, files in os.walk(self.tmp):
      for d in dirs:
        os.chmod(os.path.join(root, d), stat.S_IRWXU)
    shutil.rmtree(self.tmp)

  def test_cache_dir(self):
    repo = os.path.join(self.tmp, 'gamma')
    os.makedirs(os.path.join(repo, 'cache'))
    self.assertEqual(os.path.join(repo, 'cache'), rma.cache_dir(repo))
    bare = os.path.join(self.tmp, 'bare')
    os.makedirs(bare)
    self.assertEqual(bare, rma.cache_dir(bare))

  def test_scan(self):
    cache = os.path.join(self.tmp, 'gamma', 'cache')
    os.makedirs(cache)
    os.makedirs(os.path.join(self.tmp, 'gamma', 'MIGRATED'))
    make_au_dir(os.path.join(self.tmp, 'gamma', 'MIGRATED'), 'z', AUID_D,
                hosts=['www.example.com'])
    a = make_au_dir(cache, 'b', AUID_A)
    b = make_au_dir(cache, 'c', AUID_B, hosts=['www.example.com', 'cdn.example.com'])
    make_au_dir(cache, 'd', AUID_C, id_file=False)
    make_au_dir(cache, 'e', AUID_C, id_file=True)
    with open(os.path.join(cache, 'e', '#au_id_file'), 'w') as f:
      f.write('nothing=here\n')
    with open(os.path.join(cache, 'notadir'), 'w') as f:
      f.write('x')
    index = {}
    stats = {'au_dirs': 0, 'dirs_skipped': 0}
    rma.scan_repo(cache, index, stats)
    self.assertEqual({AUID_A: [(a, 0)], AUID_B: [(b, 2)]}, index)
    self.assertEqual({'au_dirs': 2, 'dirs_skipped': 2}, stats)

  def test_two_repos(self):
    cache1 = os.path.join(self.tmp, 'r1', 'cache')
    cache2 = os.path.join(self.tmp, 'r2', 'cache')
    os.makedirs(cache1)
    os.makedirs(cache2)
    a1 = make_au_dir(cache1, 'b', AUID_A)
    a2 = make_au_dir(cache2, 'b', AUID_A, hosts=['www.example.com'])
    index = {}
    stats = {'au_dirs': 0, 'dirs_skipped': 0}
    rma.scan_repo(cache1, index, stats)
    rma.scan_repo(cache2, index, stats)
    self.assertEqual({AUID_A: [(a1, 0), (a2, 1)]}, index)
    self.assertEqual(2, stats['au_dirs'])
    rows = rma.candidate_rows({AUID_A}, {AUID_A}, index)
    self.assertEqual([{'auid': AUID_A, 'repo_dir': a1 + ';' + a2,
                       'has_content': 1, 'content_entries': 1}], rows)

  def test_unreadable_cache_dir(self):
    with self.assertRaises(rma.SurveyError):
      rma.scan_repo(os.path.join(self.tmp, 'missing'), {}, {})

  @unittest.skipIf(hasattr(os, 'geteuid') and os.geteuid() == 0,
                   'root can list anything')
  def test_unlistable_au_dir_counts_as_content(self):
    cache = os.path.join(self.tmp, 'gamma', 'cache')
    os.makedirs(cache)
    a = make_au_dir(cache, 'b', AUID_A)
    # Search but not read permission: #au_id_file can be opened, the
    # directory can't be listed
    os.chmod(a, stat.S_IXUSR)
    try:
      index = {}
      stats = {'au_dirs': 0, 'dirs_skipped': 0}
      rma.scan_repo(cache, index, stats)
    finally:
      os.chmod(a, stat.S_IRWXU)
    self.assertEqual({AUID_A: [(a, rma.UNKNOWN_ENTRIES)]}, index)
    rows = rma.candidate_rows({AUID_A}, {AUID_A}, index)
    self.assertEqual(1, rows[0]['has_content'])
    self.assertEqual(rma.UNKNOWN_ENTRIES, rows[0]['content_entries'])


class TestCandidateRows(unittest.TestCase):

  def test_selection_and_order(self):
    index = {AUID_B: [('/r/cache/c', 3)], AUID_A: [('/r/cache/b', 0)]}
    rows = rma.candidate_rows({AUID_A, AUID_B, AUID_C, AUID_D},
                              {AUID_A, AUID_B, AUID_C, AUID_E}, index)
    self.assertEqual([AUID_A, AUID_B, AUID_C], [r['auid'] for r in rows])
    self.assertEqual({'auid': AUID_A, 'repo_dir': '/r/cache/b',
                      'has_content': 0, 'content_entries': 0}, rows[0])
    self.assertEqual({'auid': AUID_B, 'repo_dir': '/r/cache/c',
                      'has_content': 1, 'content_entries': 3}, rows[1])
    self.assertEqual({'auid': AUID_C, 'repo_dir': '',
                      'has_content': 0, 'content_entries': 0}, rows[2])


class TestEndToEnd(unittest.TestCase):
  '''A: active, in V2, no content.  B: active, in V2, content.  C: active,
  in V2, no AU dir.  D: active, not in V2.  E: inactive, in V2.  F: active,
  in V2, a dir in each repo, one with content.  R1: a plugin registry AU,
  active in au.txt and in V2.  R2: a plugin registry AU only in V2.'''

  def setUp(self):
    self.tmp = tempfile.mkdtemp()
    self.config = os.path.join(self.tmp, 'config')
    os.makedirs(self.config)
    self.au_txt = os.path.join(self.config, 'au.txt')
    with open(self.au_txt, 'wb') as f:
      f.write(au_txt([
        au_txt_entry(AUID_A, [('volume_name', '1'),
                              ('reserved.disabled', 'false')]),
        au_txt_entry(AUID_B, [('volume_name', '2')]),
        au_txt_entry(AUID_C, [('journal_id', 'zookeys'),
                              ('reserved.repository', 'local\\:/cache0')]),
        au_txt_entry(AUID_D, [('volume_name', '4')]),
        au_txt_entry(AUID_E, [('volume_name', '5'),
                              ('reserved.disabled', 'true')]),
        au_txt_entry(AUID_F, [('volume_name', '6')]),
        au_txt_entry(AUID_R1, [('base_url',
                                'http\\://props.lockss.org\\:8001/plugins/')]),
      ]))
    self.v2 = os.path.join(self.tmp, 'v2-auids.txt')
    with open(self.v2, 'w') as f:
      f.write('\n'.join(['# v2', AUID_A, AUID_B, AUID_C, AUID_E, AUID_F,
                         'org|lockss|plugin|OnlyInV2&a~1', AUID_R1, AUID_R2,
                         '']))
    self.r1 = os.path.join(self.tmp, 'r1')
    self.r2 = os.path.join(self.tmp, 'r2')
    c1 = os.path.join(self.r1, 'cache')
    c2 = os.path.join(self.r2, 'cache')
    os.makedirs(c1)
    os.makedirs(c2)
    self.dir_a = make_au_dir(c1, 'b', AUID_A)
    self.dir_b = make_au_dir(c1, 'c', AUID_B, hosts=['www.example.com'])
    self.dir_e = make_au_dir(c1, 'd', AUID_E, hosts=['www.example.com'])
    self.dir_f1 = make_au_dir(c1, 'e', AUID_F)
    self.dir_f2 = make_au_dir(c2, 'b', AUID_F, hosts=['www.example.com'])
    make_au_dir(c2, 'c', None, id_file=False)
    self.out = os.path.join(self.tmp, 'out', 'sub')

  def tearDown(self):
    shutil.rmtree(self.tmp)

  def args(self, out=None):
    return ['--au-txt', self.au_txt, '--v2-auids', self.v2,
            '--repo', self.r1, '--repo', self.r2, '--out', out or self.out]

  def test_outputs(self):
    res = run_script(self.args())
    self.assertEqual(0, res.returncode, res.stderr)
    self.assertEqual('', res.stderr)
    rows = read_tsv(os.path.join(self.out, 'candidates.tsv'))
    self.assertEqual([
      {'auid': AUID_A, 'repo_dir': self.dir_a, 'has_content': '0',
       'content_entries': '0'},
      {'auid': AUID_B, 'repo_dir': self.dir_b, 'has_content': '1',
       'content_entries': '1'},
      {'auid': AUID_F, 'repo_dir': self.dir_f1 + ';' + self.dir_f2,
       'has_content': '1', 'content_entries': '1'},
      {'auid': AUID_C, 'repo_dir': '', 'has_content': '0',
       'content_entries': '0'},
    ], sorted(rows, key=lambda r: r['auid']))
    self.assertEqual([r['auid'] for r in rows],
                     sorted(r['auid'] for r in rows))
    with_content = read_tsv(os.path.join(self.out, 'with-content.tsv'))
    self.assertEqual([AUID_B, AUID_F], [r['auid'] for r in with_content])
    self.assertEqual(['auid', 'repo_dir', 'has_content', 'content_entries'],
                     list(with_content[0].keys()))
    journal = os.path.join(self.out, 'au-journal-additions.csv')
    with open(journal, 'rb') as f:
      raw = f.read()
    self.assertTrue(raw.endswith(b'\r\n'), raw)
    self.assertNotIn(b'\n\n', raw)
    with open(journal, newline='') as f:
      records = list(csv.reader(f))
    self.assertEqual([['DELETE', AUID_A], ['DELETE', AUID_C]], records)
    for auid in (AUID_B, AUID_D, AUID_E, AUID_F, AUID_R1, AUID_R2):
      self.assertNotIn(auid.encode('latin-1'), raw)
    # The registry AUs are left out of every list and counted
    for auid in (AUID_R1, AUID_R2):
      self.assertNotIn(auid, [r['auid'] for r in rows])
      self.assertNotIn(auid, [r['auid'] for r in with_content])
    with open(os.path.join(self.out, 'summary.txt')) as f:
      summary = f.read()
    self.assertEqual(summary, res.stdout)
    self.assertNotIn('from the repository service', summary)
    self.assertNotIn('from the configuration service', summary)
    self.assertFalse(os.path.exists(os.path.join(self.out, 'v2-auids.txt')))
    expected = [
      ('AUs active in au.txt', 6),
      ('AUs inactive in au.txt', 1),
      ('V2 AUIDs read', 8),
      ('Candidates (active in V1, present in V2)', 4),
      ('without content, to delete', 2),
      ('of which without an AU directory', 1),
      ('with content, to decide', 2),
      ('Plugin registry AUs left out', 2),
      ('AU directories scanned', 5),
      ('Directories skipped (no readable #au_id_file)', 1),
      ('AUIDs with more than one directory', 1),
    ]
    lines = summary.splitlines()
    self.assertEqual(len(expected), len(lines))
    for line, (label, n) in zip(lines, expected):
      self.assertIn(label + ':', line)
      self.assertEqual(str(n), line.split()[-1], line)

  def test_delete_with_content(self):
    '''With --delete-with-content the journal lists every candidate, in
    the order of candidates.tsv; the other outputs are unchanged and the
    summary says the candidates with content are deleted too.'''
    res = run_script(self.args() + ['--delete-with-content'])
    self.assertEqual(0, res.returncode, res.stderr)
    self.assertEqual('', res.stderr)
    rows = read_tsv(os.path.join(self.out, 'candidates.tsv'))
    self.assertEqual([AUID_A, AUID_B, AUID_F, AUID_C],
                     [r['auid'] for r in rows])
    with_content = read_tsv(os.path.join(self.out, 'with-content.tsv'))
    self.assertEqual([AUID_B, AUID_F], [r['auid'] for r in with_content])
    with open(os.path.join(self.out, 'au-journal-additions.csv'),
              newline='') as f:
      self.assertEqual([['DELETE', r['auid']] for r in rows],
                       list(csv.reader(f)))
    lines = res.stdout.splitlines()
    self.assertEqual(11, len(lines))
    self.assertNotIn('to decide', res.stdout)
    labels = [line.rsplit(':', 1)[0].strip() for line in lines]
    i = labels.index('with content, to delete too')
    self.assertEqual('2', lines[i].split()[-1], lines[i])
    self.assertEqual('without content, to delete', labels[i - 2])
    with open(os.path.join(self.out, 'summary.txt')) as f:
      self.assertEqual(res.stdout, f.read())

  def test_out_inside_repo_refused(self):
    res = run_script(self.args(os.path.join(self.r1, 'cache', 'out')))
    self.assertEqual(2, res.returncode)
    self.assertIn('repository', res.stderr)
    self.assertFalse(os.path.exists(os.path.join(self.r1, 'cache', 'out')))
    res = run_script(self.args(self.r2))
    self.assertEqual(2, res.returncode)

  def test_out_in_config_dir_refused(self):
    res = run_script(self.args(self.config))
    self.assertEqual(2, res.returncode)
    self.assertIn('au.txt', res.stderr)
    res = run_script(self.args(os.path.join(self.config, 'survey')))
    self.assertEqual(2, res.returncode)
    self.assertEqual(['au.txt'], os.listdir(self.config))

  def test_missing_inputs(self):
    # The missing au.txt sits in a directory that doesn't hold --out
    res = run_script(['--au-txt', os.path.join(self.tmp, 'other', 'none'),
                      '--v2-auids', self.v2, '--repo', self.r1,
                      '--out', self.out])
    self.assertEqual(2, res.returncode)
    self.assertIn('not a file', res.stderr)
    res = run_script(['--au-txt', self.au_txt,
                      '--v2-auids', os.path.join(self.tmp, 'none'),
                      '--repo', self.r1, '--out', self.out])
    self.assertEqual(2, res.returncode)
    res = run_script(['--au-txt', self.au_txt, '--v2-auids', self.v2,
                      '--repo', os.path.join(self.tmp, 'none'),
                      '--out', self.out])
    self.assertEqual(2, res.returncode)
    self.assertIn('not a directory', res.stderr)
    self.assertFalse(os.path.exists(self.out))

  def test_repo_required(self):
    res = run_script(['--au-txt', self.au_txt, '--v2-auids', self.v2,
                      '--out', self.out])
    self.assertEqual(2, res.returncode)
    self.assertIn('--repo', res.stderr)

  def test_repo_without_cache_subdir(self):
    res = run_script(['--au-txt', self.au_txt, '--v2-auids', self.v2,
                      '--repo', os.path.join(self.r1, 'cache'),
                      '--out', self.out])
    self.assertEqual(0, res.returncode, res.stderr)
    rows = read_tsv(os.path.join(self.out, 'candidates.tsv'))
    self.assertEqual({AUID_A: self.dir_a, AUID_B: self.dir_b, AUID_C: '',
                      AUID_F: self.dir_f1},
                     dict((r['auid'], r['repo_dir']) for r in rows))

  def test_help(self):
    res = run_script(['--help'])
    self.assertEqual(0, res.returncode)
    self.assertIn('jq -r', res.stdout)
    self.assertIn('continuationToken', res.stdout)
    self.assertIn('--host HOST --user USER[:PASS]', res.stdout)
    self.assertIn('v2-auids.txt', res.stdout)
    self.assertIn('org.lockss.v2.migrate.rs.port (24611)', res.stdout)
    self.assertIn('org.lockss.v2.migrate.cfg.port\n(24612)', res.stdout)

  def host_args(self, repo, cfg, user='u:p', out=None):
    return ['--au-txt', self.au_txt, '--host', '127.0.0.1', '--user', user,
            '--rs-port', str(repo.port), '--cfg-port', str(cfg.port),
            '--repo', self.r1, '--repo', self.r2, '--out', out or self.out]

  def test_source_options(self):
    res = run_script(self.args() + ['--host', 'h', '--user', 'u:p'])
    self.assertEqual(2, res.returncode)
    self.assertIn('not allowed with', res.stderr)
    res = run_script(['--au-txt', self.au_txt, '--repo', self.r1,
                      '--out', self.out])
    self.assertEqual(2, res.returncode)
    self.assertIn('required', res.stderr)
    res = run_script(['--au-txt', self.au_txt, '--host', 'h',
                      '--repo', self.r1, '--out', self.out])
    self.assertEqual(2, res.returncode)
    self.assertIn('--host requires --user', res.stderr)
    res = run_script(self.args() + ['--user', 'u:p'])
    self.assertEqual(2, res.returncode)
    self.assertIn('--user requires --host', res.stderr)
    self.assertFalse(os.path.exists(self.out))

  def test_host_outputs(self):
    '''The same AUIDs as the file in setUp, split between the two
    services, give the same candidates; the fetched list is written and
    reads back; the summary says what each service listed.'''
    only_v2 = 'org|lockss|plugin|OnlyInV2&a~1'
    repo, cfg = FakeService(), FakeService()
    try:
      repo.pages(([AUID_A, AUID_B, AUID_R1], 't1'), ([AUID_C, AUID_F], None))
      cfg.aus(([AUID_A, AUID_E], 'c1'), ([only_v2, AUID_R2], None))
      res = run_script(self.host_args(repo, cfg))
    finally:
      repo.close()
      cfg.close()
    self.assertEqual(0, res.returncode, res.stderr)
    self.assertEqual('', res.stderr)
    rows = read_tsv(os.path.join(self.out, 'candidates.tsv'))
    self.assertEqual([AUID_A, AUID_B, AUID_F, AUID_C],
                     sorted(r['auid'] for r in rows))
    with open(os.path.join(self.out, 'au-journal-additions.csv'),
              newline='') as f:
      self.assertEqual([['DELETE', AUID_A], ['DELETE', AUID_C]],
                       list(csv.reader(f)))
    v2_path = os.path.join(self.out, 'v2-auids.txt')
    with open(v2_path) as f:
      lines = f.read().splitlines()
    self.assertTrue(lines[0].startswith(
      '# V2 AUIDs fetched from 127.0.0.1 (configuration service port %d, '
      'repository service port %d, namespace lockss) at 20'
      % (cfg.port, repo.port)), lines[0])
    self.assertEqual('# repository service: 5, configuration service: 4',
                     lines[1])
    self.assertEqual(sorted(lines[2:]), lines[2:])
    fetched = {AUID_A, AUID_B, AUID_C, AUID_E, AUID_F, only_v2, AUID_R1,
               AUID_R2}
    self.assertEqual(fetched, set(lines[2:]))
    self.assertEqual(fetched, rma.read_v2_auids(v2_path))
    lines = res.stdout.splitlines()
    self.assertEqual(13, len(lines))
    labels = [line.rsplit(':', 1)[0].strip() for line in lines]
    i = labels.index('V2 AUIDs read')
    self.assertEqual(['from the repository service',
                      'from the configuration service'], labels[i + 1:i + 3])
    self.assertEqual(['8', '5', '4'],
                     [line.split()[-1] for line in lines[i:i + 3]])
    with open(os.path.join(self.out, 'summary.txt')) as f:
      self.assertEqual(res.stdout, f.read())
    # The written list reproduces the run
    out2 = os.path.join(self.tmp, 'out2')
    res2 = run_script(['--au-txt', self.au_txt, '--v2-auids', v2_path,
                       '--repo', self.r1, '--repo', self.r2, '--out', out2])
    self.assertEqual(0, res2.returncode, res2.stderr)
    self.assertEqual(rows, read_tsv(os.path.join(out2, 'candidates.tsv')))
    self.assertEqual(11, len(res2.stdout.splitlines()))

  def test_host_local_checks_before_fetch(self):
    repo, cfg = FakeService(), FakeService()
    try:
      res = run_script(self.host_args(
        repo, cfg, out=os.path.join(self.r1, 'cache', 'out')))
      self.assertEqual(2, res.returncode)
      self.assertIn('repository', res.stderr)
      self.assertEqual([], repo.requests)
      self.assertEqual([], cfg.requests)
      res = run_script(self.host_args(repo, cfg, user=':p'))
      self.assertEqual(2, res.returncode)
      self.assertIn('user name', res.stderr)
      self.assertEqual([], repo.requests)
    finally:
      repo.close()
      cfg.close()
    self.assertFalse(os.path.exists(os.path.join(self.r1, 'cache', 'out')))

  def test_host_fetch_failure(self):
    repo, cfg = FakeService(), FakeService()
    try:
      repo.responses.append((404, '{"error": "no namespace"}'))
      res = run_script(self.host_args(repo, cfg))
    finally:
      repo.close()
      cfg.close()
    self.assertEqual(2, res.returncode)
    self.assertIn('repository service', res.stderr)
    self.assertIn('404', res.stderr)
    self.assertNotIn('u:p', res.stderr)
    self.assertFalse(os.path.exists(self.out))


if __name__ == '__main__':
  unittest.main()
