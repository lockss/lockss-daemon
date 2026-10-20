#!/usr/bin/env python3

'''Tests for check-v1-repo.py. Run: python3 test/scripts/test_check_v1_repo.py'''

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

import csv
import hashlib
import importlib.util
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                      'check-v1-repo.py')

_spec = importlib.util.spec_from_file_location('check_v1_repo', SCRIPT)
cvr = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cvr)


def props(version=None, url=None, extra=''):
  s = '#HTTP headers for %s\n#Mon Jan 01 00:00:00 PST 2024\n' % (url or 'u')
  if version is not None:
    s += 'org.lockss.version.number=%d\n' % version
  if url:
    s += 'x-lockss-node-url=%s\n' % url.replace(':', '\\:')
  return (s + extra).encode('latin-1')


class TestParseJavaProps(unittest.TestCase):

  def test_basic(self):
    self.assertEqual({'a': 'b', 'c': 'd e', 'f': ''},
                     cvr.parse_java_props(b'a=b\nc : d e\nf\n'))

  def test_comments_and_blank_lines(self):
    self.assertEqual({'a': '1'},
                     cvr.parse_java_props(b'# c\n! c\n\n   \na=1\n'))

  def test_separators(self):
    self.assertEqual({'a': 'b', 'c': 'd', 'e': '=f'},
                     cvr.parse_java_props(b'a b\nc:d\ne = =f\n'))

  def test_continuation(self):
    self.assertEqual({'org.lockss.version.number': '3'},
                     cvr.parse_java_props(b'org.lockss.\\\n   version.number : 3\n'))

  def test_escapes(self):
    self.assertEqual({'a=b': 'x\ty', 'u': '\x00A', 'k': 'http://x'},
                     cvr.parse_java_props(b'a\\=b=x\\ty\nu=\\u0000\\u0041\n'
                                          b'k=http\\://x\n'))

  def test_crlf(self):
    self.assertEqual({'a': '1', 'b': '2'},
                     cvr.parse_java_props(b'a=1\r\nb=2\r'))

  def test_nul_file_is_one_key(self):
    # As java.util.Properties: NULs aren't whitespace or separators
    self.assertEqual({'\x00' * 5: ''}, cvr.parse_java_props(b'\x00' * 5))

  def test_malformed_unicode(self):
    with self.assertRaises(cvr.PropsError):
      cvr.parse_java_props(b'k=\\uZZZZ\n')


class TestParseDuration(unittest.TestCase):

  def test_units(self):
    self.assertEqual([900, 90, 90, 7200],
                     [cvr.parse_duration(s) for s in ('900', '90s', '1.5m', '2h')])

  def test_bad(self):
    for s in ('', '5x', 'm', '-3'):
      with self.assertRaises(Exception):
        cvr.parse_duration(s)


class RepoFixture(object):
  '''Builds a V1 repository under a temporary directory.'''

  def __init__(self):
    self.root = tempfile.mkdtemp(prefix='check-v1-repo-test')
    self.cache = os.path.join(self.root, 'cache')

  def cleanup(self):
    shutil.rmtree(self.root)

  def au(self, name, auid=None, au_id_bytes=None):
    d = os.path.join(self.cache, name)
    os.makedirs(d)
    if au_id_bytes is None:
      au_id_bytes = ('au.id=%s\n' % (auid or 'auid_' + name)).encode('latin-1')
    self.write(os.path.join(d, '#au_id_file'), au_id_bytes)
    self.write(os.path.join(d, '#au_state.xml'), b'<AuState/>')
    return d

  def node(self, au_dir, path, files, node_props=None):
    nd = os.path.join(au_dir, path)
    cd = os.path.join(nd, '#content')
    os.makedirs(cd)
    for name, data in files.items():
      self.write(os.path.join(cd, name), data)
    if node_props is not None:
      self.write(os.path.join(nd, '#node_props'), node_props)
    return cd

  @staticmethod
  def write(path, data):
    with open(path, 'wb') as f:
      f.write(data)


def run_script(args, tsv_path=None):
  cmd = [sys.executable, '-I', SCRIPT]
  if tsv_path:
    cmd += ['--tsv', tsv_path]
  res = subprocess.run(cmd + args, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE, universal_newlines=True)
  rows = []
  if tsv_path and os.path.exists(tsv_path):
    with open(tsv_path, newline='') as f:
      rows = list(csv.DictReader(f, delimiter='\t'))
  return res, rows


def codes_for(rows, node):
  return sorted(set(r['code'] for r in rows if r['node'] == node))


class TestScanFindings(unittest.TestCase):

  @classmethod
  def setUpClass(cls):
    fx = cls.fx = RepoFixture()
    a = fx.au('a')
    good = b'good content'
    fx.node(a, 'h/healthy', {
      '1': b'one', '1.props': props(1),
      'current': b'two', 'current.props': props(2)})
    # renumbered: old current became 0, new current says 1
    fx.node(a, 'h/v0', {
      '0': b'old current', '0.props': props(None, extra='\\u0000\\u0000=\n'
                                            'org.lockss.node.was.inactive=true\n'),
      '1': b'one', '1.props': props(1),
      '2': b'two', '2.props': props(2),
      'current': b'new', 'current.props': props(1)})
    # identical-content variant: wrong number, no 0
    fx.node(a, 'h/ident', {
      '1': b'one', '1.props': props(1),
      '2': b'two', '2.props': props(2),
      'current': b'two', 'current.props': props(1)})
    # latent: zeroed current.props, deactivated
    fx.node(a, 'h/latent', {
      '1': b'one', '1.props': props(1),
      'current': b'two', 'current.props': b'\x00' * 600},
            node_props=b'node.content.isInactive=true\n')
    fx.node(a, 'h/missing', {
      '1': b'one',
      'current': b'two', 'current.props': props(2)})
    fx.node(a, 'h/props', {
      '1': b'1', '1.props': b'',
      '2': b'2', '2.props': b'k=\\uZZZZ\n',
      '3': b'3', '3.props': b'a=b\n',
      '4': b'4', '4.props': props(7),
      '5': b'5', '5.props': b'org.lockss.version.number=5\n\x00\x00',
      'current': b'c', 'current.props': props(6)})
    fx.node(a, 'h/misc', {
      '9.props': props(9), 'temp': b't', 'inactive': b'i',
      'inactive.props': props(1), 'x.ERROR': b'e',
      'current': b'c', 'current.props': props(1)})
    fx.node(a, 'h/empty', {
      'current': b'', 'current.props': props(1)})
    fx.node(a, 'h/zeroed', {
      'current': b'\x00' * 5000, 'current.props': props(1)})
    sha1 = hashlib.sha1(b'a' * 4096 + good).hexdigest().upper()
    fx.node(a, 'h/partial', {
      'current': b'\x00' * 4096 + good,
      'current.props': props(1, extra='x-lockss-checksum=SHA-1\\:%s\n' % sha1)})
    fx.node(a, 'h/okck', {
      'current': good,
      'current.props': props(1, extra='x-lockss-checksum=SHA-1\\:%s\n' %
                             hashlib.sha1(good).hexdigest())})
    fx.node(a, 'h/zeroblocks', {
      'current': b'a' * 4096 + b'\x00' * 4096,
      'current.props': props(1)})
    fx.node(a, 'h/zeronodeprops', {
      'current': b'c', 'current.props': props(1)},
            node_props=b'\x00' * 100)
    fx.au('b', au_id_bytes=b'\x00' * 200)
    cls.tsv = os.path.join(fx.root, 'out.tsv')
    cls.res, cls.rows = run_script([fx.cache], cls.tsv)
    cls.tsv_v = os.path.join(fx.root, 'out-v.tsv')
    cls.res_v, cls.rows_v = run_script(['--verify-content', fx.cache],
                                       cls.tsv_v)

  @classmethod
  def tearDownClass(cls):
    cls.fx.cleanup()

  def test_runs(self):
    self.assertEqual(0, self.res.returncode, self.res.stderr)
    self.assertIn('TOTAL: 2 AUs scanned', self.res.stdout)

  def test_healthy(self):
    self.assertEqual([], codes_for(self.rows, 'h/healthy'))

  def test_version_zero(self):
    self.assertEqual(['CURRENT_VERSION_MISMATCH', 'PROPS_REWRITTEN_ZEROED',
                      'VERSION_ZERO'],
                     codes_for(self.rows, 'h/v0'))
    detail = [r['detail'] for r in self.rows
              if r['node'] == 'h/v0' and r['code'] == 'VERSION_ZERO'][0]
    self.assertIn('matches inactive-node clobber (zeroed current.props)',
                  detail)
    mismatch = [r['detail'] for r in self.rows
                if r['node'] == 'h/v0' and r['code'] == 'CURRENT_VERSION_MISMATCH'][0]
    self.assertIn('version 1, expected 3', mismatch)

  def test_wrong_number_without_version_zero(self):
    self.assertEqual(['CURRENT_VERSION_MISMATCH'],
                     codes_for(self.rows, 'h/ident'))

  def test_latent(self):
    self.assertEqual(['CURRENT_PROPS_UNUSABLE', 'NODE_MARKED_INACTIVE',
                      'PROPS_ZEROED'],
                     codes_for(self.rows, 'h/latent'))

  def test_missing_props(self):
    self.assertEqual(['PROPS_MISSING'], codes_for(self.rows, 'h/missing'))

  def test_bad_props(self):
    by_file = dict((r['file'], r['code']) for r in self.rows
                   if r['node'] == 'h/props')
    self.assertEqual({'1.props': 'PROPS_EMPTY',
                      '2.props': 'PROPS_UNPARSEABLE',
                      '3.props': 'PROPS_NO_VERSION',
                      '4.props': 'PROPS_VERSION_MISMATCH',
                      '5.props': 'PROPS_NUL_BYTES'},
                     by_file)

  def test_misc(self):
    self.assertEqual(['FAULTY_RENAMED', 'INACTIVE_FILES', 'LEFTOVER_TEMP',
                      'ORPHAN_PROPS'],
                     codes_for(self.rows, 'h/misc'))

  def test_content(self):
    self.assertEqual(['CONTENT_EMPTY'], codes_for(self.rows, 'h/empty'))
    self.assertEqual(['CONTENT_ZEROED'], codes_for(self.rows, 'h/zeroed'))
    # partial zeroing only found with --verify-content
    self.assertEqual([], codes_for(self.rows, 'h/partial'))
    self.assertEqual([], codes_for(self.rows, 'h/zeroblocks'))

  def test_verify_content(self):
    self.assertEqual(0, self.res_v.returncode, self.res_v.stderr)
    self.assertEqual(['CONTENT_CHECKSUM_MISMATCH'],
                     codes_for(self.rows_v, 'h/partial'))
    detail = [r['detail'] for r in self.rows_v
              if r['node'] == 'h/partial'][0]
    self.assertIn('NUL 4K blocks at 0-4096', detail)
    self.assertEqual([], codes_for(self.rows_v, 'h/okck'))
    self.assertEqual(['CONTENT_ZERO_BLOCKS'],
                     codes_for(self.rows_v, 'h/zeroblocks'))
    self.assertEqual([], codes_for(self.rows_v, 'h/healthy'))

  def test_aux_files(self):
    self.assertEqual(['AUX_ZEROED'], codes_for(self.rows, 'h/zeronodeprops'))
    au_b = [r for r in self.rows if r['au_dir'].endswith(os.sep + 'b')]
    self.assertEqual([('AUX_ZEROED', '#au_id_file')],
                     [(r['code'], r['file']) for r in au_b])
    self.assertIn('(no readable au.id)', self.res.stdout)


class TestScanOrderAndTimeout(unittest.TestCase):

  @classmethod
  def setUpClass(cls):
    fx = cls.fx = RepoFixture()
    for ix in range(6):
      a = fx.au('a%d' % ix)
      for jx in range(4):
        fx.node(a, 'h/n%d' % jx, {
          '1': b'one', '1.props': props(1),
          'current': b'two', 'current.props': props(1)})

  @classmethod
  def tearDownClass(cls):
    cls.fx.cleanup()

  def tsv(self, name):
    return os.path.join(self.fx.root, name)

  def test_same_seed_same_order(self):
    _, rows1 = run_script(['--seed', '5', self.fx.cache], self.tsv('s5a.tsv'))
    _, rows2 = run_script(['--seed', '5', self.fx.cache], self.tsv('s5b.tsv'))
    self.assertEqual(24, len(rows1))
    self.assertEqual(rows1, rows2)

  def test_seed_changes_order(self):
    orders = set()
    for seed in range(5):
      _, rows = run_script(['--seed', str(seed), self.fx.cache],
                           self.tsv('o%d.tsv' % seed))
      orders.add(tuple((r['au_dir'], r['node']) for r in rows))
      self.assertEqual(24, len(rows))
    self.assertGreater(len(orders), 1)

  def test_seed_reported(self):
    res, _ = run_script(['--seed', '42', self.fx.cache])
    self.assertIn('Random order seed: 42', res.stdout)

  def test_timeout_zero_min_aus(self):
    res, rows = run_script(['--timeout', '0', '--min-aus', '2',
                            self.fx.cache], self.tsv('t2.tsv'))
    self.assertEqual(0, res.returncode, res.stderr)
    self.assertIn('TOTAL: 2 AUs scanned', res.stdout)
    self.assertIn('unknown number not scanned', res.stdout)
    self.assertIn('continued to reach min AUs', res.stdout)
    self.assertEqual(2, len(set(r['au_dir'] for r in rows)))
    # AUs within the minimum are scanned completely
    self.assertEqual(8, len(rows))

  def test_timeout_zero_no_min(self):
    res, rows = run_script(['--timeout', '0', '--min-aus', '0',
                            self.fx.cache], self.tsv('t0.tsv'))
    self.assertIn('TOTAL: 0 AUs scanned', res.stdout)
    self.assertEqual([], rows)

  def test_min_aus_above_total(self):
    res, rows = run_script(['--timeout', '0', self.fx.cache],
                           self.tsv('t10.tsv'))
    self.assertIn('TOTAL: 6 AUs scanned, 0 not scanned', res.stdout)
    self.assertEqual(24, len(rows))

  def test_no_timeout_scans_all(self):
    res, rows = run_script([self.fx.cache], self.tsv('all.tsv'))
    self.assertIn('TOTAL: 6 AUs scanned, 0 not scanned', res.stdout)

  def test_bad_args(self):
    res, _ = run_script(['--min-aus', '-1', self.fx.cache])
    self.assertNotEqual(0, res.returncode)
    res, _ = run_script([os.path.join(self.fx.root, 'nonexistent')])
    self.assertEqual(2, res.returncode)


if __name__ == '__main__':
  unittest.main()
