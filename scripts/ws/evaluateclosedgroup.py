#!/usr/bin/env python

'''An experimental module to evaluate the health of an AU among a closed group
of LOCKSS daemons.'''

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

__version__ = '0.2.0'

import getpass
import optparse
import sys

from daemonstatusservice import auth, datetime_from_ms, \
    get_platform_configuration, is_daemon_ready, query_aus

class _EvaluateClosedGroupOptions(object):
  def __init__(self, parser, opts, args):
    super(_EvaluateClosedGroupOptions, self).__init__()
    # auid
    if len(args) != 1: parser.error('exactly one AUID is required')
    self.auid = args[0]
    # hosts
    self.hosts = opts.host
    for f in opts.hosts: self.hosts.extend(_file_lines(f))
    if len(self.hosts) < 2: parser.error('at least two hosts are required')
    # auth
    u = opts.username or raw_input('UI username: ')
    p = opts.password or getpass.getpass('UI password: ')
    self.auth = auth(u, p)
  @staticmethod
  def make_parser():
    usage = '%prog [OPTIONS] AUID'
    parser = optparse.OptionParser(version=__version__, description=__doc__, usage=usage)
    group = optparse.OptionGroup(parser, 'Hosts')
    group.add_option('--host', action='append', default=list(), help='adds host:port pair to the list of hosts')
    group.add_option('--hosts', action='append', default=list(), metavar='HFILE', help='adds host:port pairs from HFILE to the list of hosts')
    group.add_option('--password', metavar='PASS', help='UI password (default: interactive prompt)')
    group.add_option('--username', metavar='USER', help='UI username (default: interactive prompt)')
    parser.add_option_group(group)
    return parser
   
def _do_evaluate(options):
  auid = options.auid
  hosts = sorted(options.hosts)
  auth = options.auth
  quit = False
  print '%s BEGIN' % (auid,)
  # Check that all hosts are up and ready
  for host in hosts:
    if not is_daemon_ready(host, options.auth): sys.exit('Error: %s is not up and ready' % (host,))
  # Get peer identities
  host_peerid = dict()
  peerid_host = dict()
  for host in hosts:
    peerid = get_platform_configuration(host, auth).V3Identity
    host_peerid[host] = peerid
    peerid_host[peerid] = host
  # Get AU data
  select = ['creationTime', 'lastCompletedCrawl', 'lastCompletedPoll', 'peerAgreements', 'substanceState']
  where = 'auId = "%s"' % (auid,)
  host_audata = dict()
  for host in hosts:
    r = query_aus(host, auth, select, where)
    if r is None or len(r) == 0:
      quit = True
      print '%s not found on %s' % (auid, host)
      continue
    host_audata[host] = r[0]
  if quit: sys.exit(1)
  # Check basic AU data
  lccs = list()
  lcps = list()
  for host in hosts:
    audata = host_audata[host]
    lcc = audata.LastCompletedCrawl
    if lcc is None or lcc == 0:
      quit = True
      print '%s has never crawled successfully on %s (created %s)' % (auid, host, datetime_from_ms(audata.CreationTime))
      continue
    lccs.append(lcc)
    ss = audata.SubstanceState
    if ss is None or ss != 'Yes':
      quit = True
      print '%s does not have substance on %s' % (auid, host)
      continue
    lcp = audata.LastCompletedPoll
    if lcp is None or lcp == 0:
      quit = True
      print '%s has never polled successfully on %s (created %s)' % (auid, host, datetime_from_ms(audata.CreationTime))
      continue
    lcps.append(lcp)
  if quit: sys.exit(1)
  # Compute agreement matrix
  agmat = dict()
  for host1 in hosts:
    d1 = dict()
    for host2 in hosts:
      if host1 == host2: continue
      d1[host_peerid[host2]] = None
    pa = host_audata[host1].PeerAgreements
    for pae in pa:
      if pae.PeerId not in peerid_host: continue # Peer ID outside the closed group
      for ae in pae.Agreements.Entry:
        if not (ae.Key == 'POR' or ae.Key == 'SYMMETRIC_POR'): continue # Not a POR-type agreement
        agcur = d1[pae.PeerId]
        agnew = (ae.Value.PercentAgreementTimestamp, ae.Value.PercentAgreement)
        if agcur is None or agcur < agnew: d1[pae.PeerId] = agnew
    agmat[host_peerid[host1]] = d1
  # Direct agreement
  cand = 0
  mrmin = None
  mrhost = None
  for host1 in hosts:
    ag1 = filter(lambda x: x[1] == 1.0, agmat[host_peerid[host1]].values())
    if len(ag1) == len(hosts) - 1:
      cand = cand + 1
      min1 = min([x[0] for x in ag1])
      print '%s agrees 100%% with all others since %s' % (host1, datetime_from_ms(min1))
      if mrmin is None or mrmin < min1: mrmin, mrhost = min1, host1
  if mrmin is not None:
    print '%s PASS %d (%s %s)' % (auid, cand, mrhost, datetime_from_ms(mrmin))
  else:
    print '%s FAIL' % (auid,)

def _file_lines(filestr):
  ret = [line.strip() for line in open(filestr).readlines() if not (line.isspace() or line.startswith('#'))]
  if len(ret) == 0: sys.exit('Error: %s contains no meaningful lines' % (filestr,))
  return ret

def _main():
  '''Main method.'''
  parser = _EvaluateClosedGroupOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _EvaluateClosedGroupOptions(parser, opts, args)
  _do_evaluate(options)

# Main entry point
if __name__ == '__main__': _main()

