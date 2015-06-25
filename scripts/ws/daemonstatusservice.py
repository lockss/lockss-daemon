#!/usr/bin/env python

# $Id$

__copyright__ = '''\
Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

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

__version__ = '0.1.6'

import DaemonStatusServiceImplService_client
from ZSI.auth import AUTH

import datetime
import getpass
import optparse
import sys

#
# External (module)
#

class DaemonStatusServiceOptions(object):
  def __init__(self):
    super(DaemonStatusServiceOptions, self).__init__()
    self.__auid = None
    self.__auth = None
    self.__host = None
    self.__port = None
    self.__query = None
  def get_auid(self): return self.__auid
  def set_auid(self, auid): self.__auid = auid
  def get_auth(self): return self.__auth
  def set_auth(self, usern, passw): self.__auth = (AUTH.httpbasic, usern, passw)
  def get_host(self): return self.__host
  def set_host(self, host): self.__host = host
  def get_port(self):
    if self.__port is None: self.__setup_port()
    return self.__port
  def __setup_port(self):
    url = 'http://%s/ws/DaemonStatusService' % (self.get_host(),)
    auth = self.get_auth()
    self.__port = DaemonStatusServiceImplService_client.DaemonStatusServiceImplServiceLocator().getDaemonStatusServiceImplPort(url=url, auth=auth)
#    self.__port = DaemonStatusServiceImplService_client.DaemonStatusServiceImplServiceLocator().getDaemonStatusServiceImplPort(url=url, auth=auth, tracefile=sys.stdout)
  def get_query(self): return self.__query
  def set_query(self, query): self.__query = query

def datetime_from_ms(ms):
  if ms is None: return None
  return datetime.datetime.fromtimestamp(ms / 1000)

def duration_from_ms(ms):
  s, ms = divmod(ms, 1000)
  if s == 0: return '%dms' % (ms,)
  m, s = divmod(s, 60)
  if m == 0: return '%ds' % (s,)
  h, m = divmod(m, 60)
  if h == 0: return '%dm%ds' % (m, s)
  d, h = divmod(h, 24)
  if d == 0: return '%dh%dm%ds' % (h, m, s)
  w, d = divmod(d, 7)
  if w == 0: return '%dd%dh%dm' % (d, h, m)
  return '%dw%dd%dh' % (w, d, h)

def get_au_status(options):
  '''Queries the options for this input:
  - get_auid() (string) (the desired AUID)
  Returns a record with these fields:
  - AccessType (string)
  - AvailableFromPublisher (boolean)
  - ContentSize (numeric)
  - CrawlPool (string)
  - CrawlProxy (string)
  - CrawlWindow (string)
  - CreationTime (numeric)
  - CurrentlyCrawling (boolean)
  - CurrentlyPolling (boolean)
  - DiskUsage (numeric)
  - JournalTitle (string)
  - LastCompletedCrawl (numeric)
  - LastCompletedPoll (numeric)
  - LastCrawl (numeric)
  - LastCrawlResult (string)
  - LastPoll (numeric)
  - LastPollResult (string)
  - PluginName (string)
  - Publisher (string)
  - PublishingPlatform (string)
  - RecentPollAgreement (numeric)
  - Repository (string)
  - Status (string)
  - SubscriptionStatus (string)
  - SubstanceState (string)
  - Volume (string) (the AU name)
  - Year (string)'''
  req = DaemonStatusServiceImplService_client.getAuStatus()
  req.AuId = options.get_auid()
  return options.get_port().getAuStatus(req).Return

def get_auids(options):
  '''Convenience call to get_auids_names(). Returns a list of AUIDs.'''
  return [r.Id for r in get_auids_names(options)]

def get_auids_names(options):
  '''Returns a list of records with these fields:
  - Id (string)
  - Name (string)'''
  return options.get_port().getAuIds(DaemonStatusServiceImplService_client.getAuIds()).Return

def get_peer_agreements(options):
  '''Convenience call to get_platform_configuration(). Queries the options for
  this input:
  - get_auid() (string) (the desired AUID)
  Returns None if no such AUID, or the the PeerAgreements list from the single
  record returned by get_platform_configuration(), which is a list of records
  with these fields:
  - Agreements, a record with these fields:
      - Entry, a list of records with these fields:
          - Key, a string among:
              - "POR"
              - "POP"
              - "SYMMETRIC_POR"
              - "SYMMETRIC_POP"
              - "POR_HINT"
              - "POP_HINT"
              - "SYMMETRIC_POR_HINT"
              - "SYMMETRIC_POP_HINT"
          - Value, a record with these fields:
              - HighestPercentAgreement (floating point)
              - HighestPercentAgreementTimestamp (numeric)
              - PercentAgreement (floating point)
              - PercentAgreementTimestamp (numeric)
  - PeerId (string)'''
  saved_query = options.get_query()
  try:
    options.set_query('SELECT peerAgreements WHERE auId = "%s"' % (options.get_auid(),))
    resp = query_aus(options)
    if len(resp) == 0: return None
    else: return resp[0].PeerAgreements
  finally:
    options.set_query(saved_query)

def get_platform_configuration(options):
  '''Returns a record with these fields:
  - AdminEmail (string)
  - BuildHost (string)
  - BuildTimestamp (numeric)
  - CurrentTime (numeric)
  - CurrentWorkingDirectory (string)
  - DaemonVersion, a record with these fields:
      - BuildVersion (numeric)
      - FullVersion (string)
      - MajorVersion (numeric)
      - MinorVersion (numeric)
  - Disks (list of strings)
  - Groups (list of strings)
  - HostName (string)
  - IpAddress (string)
  - JavaVersion, a record with these fields:
      - RuntimeName (string)
      - RuntimeVersion (string)
      - SpecificationVersion (string)
      - Version (string)
  - MailRelay (string)
  - Platform, a record with these fields:
      - Name (string)
      - Suffix (string)
      - Version (string)
  - Project (string)
  - Properties (list of strings)
  - Uptime (numeric)
  - V3Identity (string)'''
  return options.get_port().getPlatformConfiguration(DaemonStatusServiceImplService_client.getPlatformConfiguration()).Return

def is_daemon_ready(options):
  '''Returns True of False'''
  return options.get_port().isDaemonReady(DaemonStatusServiceImplService_client.isDaemonReady()).Return

def query_aus(options):
  '''Queries the options for this input:
  - get_query() (string) (the desired query)
  Returns a list of records with these fields:
  - AccessType (string)
  - AuConfiguration, a record with these fields:
      - DefParams, a list of records with these fields:
          - Key (string)
          - Value (string)
      - NonDefParams, a list of records with these fields:
          - Key (string)
          - Value (string)
  - AuId (string)
  - AvailableFromPublisher (boolean)
  - ContentSize (numeric)
  - CrawlPool (string)
  - CrawlProxy (string)
  - CrawlWindow (string)
  - CreationTime (numeric)
  - CurrentlyCrawling (boolean)
  - CurrentlyPolling (boolean)
  - DiskUsage (numeric)
  - IsBulkContent (boolean)
  - LastCompletedCrawl (numeric)
  - LastCompletedPoll (numeric)
  - LastCrawl (numeric)
  - LastCrawlResult (string)
  - LastPoll (numeric)
  - LastPollResult (string)
  - Name (string)
  - NewContentCrawlUrls (list of strings)
  - PeerAgreements, a list of records with these fields:
      - Agreements, a record with these fields:
          - Entry, a list of records with these fields:
              - Key, a string among:
                  - "POR"
                  - "POP"
                  - "SYMMETRIC_POR"
                  - "SYMMETRIC_POP"
                  - "POR_HINT"
                  - "POP_HINT"
                  - "SYMMETRIC_POR_HINT"
                  - "SYMMETRIC_POP_HINT"
              - Value, a record with these fields:
                  - HighestPercentAgreement (floating point)
                  - HighestPercentAgreementTimestamp (numeric)
                  - PercentAgreement (floating point)
                  - PercentAgreementTimestamp (numeric)
      - PeerId (string)
  - PluginName (string)
  - PublishingPlatform (string)
  - RecentPollAgreement (numeric)
  - RepositoryPath (string)
  - SubscriptionStatus (string)
  - SubstanceState (string)
  - TdbPublisher (string)
  - TdbYear (string)
  - UrlStems (list of strings)
  - Volume (string)'''
  req = DaemonStatusServiceImplService_client.queryAus()
  req.AuQuery = options.get_query()
  return options.get_port().queryAus(req).Return

#
# Internal (command line tool)
#

class _DaemonStatusServiceToolOptions(DaemonStatusServiceOptions):
  def __init__(self, parser, opts, args):
    super(_DaemonStatusServiceToolOptions, self).__init__()
    if len(args) != 1: parser.error('A host is required')
    self.set_host(args[0])
    if len(filter(None, [opts.get_au_status, opts.get_auids, opts.get_auids_names, opts.get_peer_agreements, opts.get_platform_configuration, opts.is_daemon_ready, opts.is_daemon_ready_quiet, opts.query_aus])) != 1:
      parser.error('Exactly one of --get-au-status, --get-auids, --get-auids-names, --get-platform-configuration, --get-peer-agreements, --is-daemon-ready, --is-daemon-ready-quiet, --query_aus is required')
    if opts.where and not any([opts.query_aus]):
      parser.error('--where can only be applied to --query-aus')
    self.__fields = None
    self.__get_au_status = opts.get_au_status is not None
    if self.__get_au_status: self.set_auid(opts.get_au_status)
    self.__get_auids = opts.get_auids
    self.__get_auids_names = opts.get_auids_names
    self.__get_peer_agreements = opts.get_peer_agreements is not None
    if self.__get_peer_agreements: self.set_auid(opts.get_peer_agreements)
    self.__get_platform_configuration = opts.get_platform_configuration
    self.__is_daemon_ready = opts.is_daemon_ready
    self.__is_daemon_ready_quiet = opts.is_daemon_ready_quiet
    self.__query_aus = opts.query_aus is not None
    if self.__query_aus:
      fields = [s.strip() for s in opts.query_aus.split(',')]
      for f in fields:
        if f not in _QUERY_AUS: parser.error('Unknown field: %s' % (f,))
      self.__fields = fields
    self.__where = opts.where or None
    u = opts.username or raw_input('UI username: ')
    p = opts.password or getpass.getpass('UI password: ')
    self.set_auth(u, p)
  def get_fields(self): return self.__fields
  def is_get_au_status(self): return self.__get_au_status
  def is_get_peer_agreements(self): return self.__get_peer_agreements
  def is_get_platform_configuration(self): return self.__get_platform_configuration
  def is_get_auids(self): return self.__get_auids
  def is_get_auids_names(self): return self.__get_auids_names
  def is_is_daemon_ready(self): return self.__is_daemon_ready
  def is_is_daemon_ready_quiet(self): return self.__is_daemon_ready_quiet
  def is_query_aus(self): return self.__query_aus
  def get_where(self): return self.__where
  @staticmethod
  def make_parser():
    parser = optparse.OptionParser(version=__version__, usage='%prog [OPTIONS] HOST')
    parser.add_option('--get-au-status', metavar='AUID', help='Outputs all status information for the AUID')
    parser.add_option('--get-auids', action='store_true', default=False, help='Outputs all AUIDs')
    parser.add_option('--get-auids-names', action='store_true', default=False, help='Outputs all AUIDs and names')
    parser.add_option('--get-peer-agreements', metavar='AUID', help='Outputs the peer agreements for the AUID')
    parser.add_option('--get-platform-configuration', action='store_true', default=False, help='Outputs the platform configuration')
    parser.add_option('--is-daemon-ready', action='store_true', default=False, help='Outputs True or False, always exits with 0')
    parser.add_option('--is-daemon-ready-quiet', action='store_true', default=False, help='Outputs nothing, exits with 0 for True and 1 for False')
    parser.add_option('--password', metavar='PASS', help='UI password')
    parser.add_option('--query-aus', metavar='CSFIELDS', help='Comma-separated fields to be used in the SELECT clause of an AU query; chosen among %s ' % (sorted(_QUERY_AUS.keys()),))
    parser.add_option('--username', metavar='USER', help='UI username')
    parser.add_option('--where', help='optional WHERE clause for --query-aus')
    return parser

def _output_record(tooloptions, lst):
  print '\t'.join([str(x) for x in lst])

def _do_get_au_status(tooloptions):
  r = get_au_status(tooloptions)
  print 'AUID: %s' % (tooloptions.get_auid(),)
  print
  print 'Access type: %s' % (r.AccessType,)
  print 'Available from publisher: %s' % (r.AvailableFromPublisher,)
  print 'Content size: %s' % (r.ContentSize,)
  print 'Crawl pool: %s' % (r.CrawlPool,)
  print 'Crawl proxy: %s' % (r.CrawlProxy,)
  print 'Crawl window: %s' % (r.CrawlWindow,)
  print 'Creation time: %s' % (datetime_from_ms(r.CreationTime),)
  print 'Currently crawling: %s' % (r.CurrentlyCrawling,)
  print 'Currently polling: %s' % (r.CurrentlyPolling,)
  print 'Disk usage: %s' % (r.DiskUsage,)
  print 'Journal title: %s' % (r.JournalTitle,)
  print 'Last completed crawl: %s' % (datetime_from_ms(r.LastCompletedCrawl),)
  print 'Last completed poll: %s' % (datetime_from_ms(r.LastCompletedPoll),)
  print 'Last crawl: %s' % (datetime_from_ms(r.LastCrawl),)
  print 'Last crawl result: %s' % (r.LastCrawlResult,)
  print 'Last poll: %s' % (datetime_from_ms(r.LastPoll),)
  print 'Last poll result: %s' % (r.LastPollResult,)
  print 'Plugin name: %s' % (r.PluginName,)
  print 'Publisher: %s' % (r.Publisher,)
  print 'Publishing platform: %s' % (r.PublishingPlatform,)
  print 'Recent poll agreement: %s' % (r.RecentPollAgreement,)
  print 'Repository: %s' % (r.Repository,)
  print 'Status: %s' % (r.Status,)
  print 'Subscription status: %s' % (r.SubscriptionStatus,)
  print 'Substance state: %s' % (r.SubstanceState,)
  print 'Volume: %s' % (r.Volume,)
  print 'Year: %s' % (r.Year,)

def _do_get_auids(tooloptions):
  for auid in get_auids(tooloptions): print auid

def _do_get_auids_names(tooloptions):
  for r in get_auids_names(tooloptions): _output_record(tooloptions, [r.Id, r.Name])

def _do_get_peer_agreements(tooloptions):
  pa = get_peer_agreements(tooloptions)
  if pa is None:
    print 'No such AUID'
    return
  for pae in pa:
    for ae in pae.Agreements.Entry:
      _output_record(tooloptions, [pae.PeerId, ae.Key, ae.Value.PercentAgreement, datetime_from_ms(ae.Value.PercentAgreementTimestamp), ae.Value.HighestPercentAgreement, datetime_from_ms(ae.Value.HighestPercentAgreementTimestamp)])

def _do_get_platform_configuration(tooloptions):
  r = get_platform_configuration(tooloptions)
  print 'Admin e-mail: %s' % (r.AdminEmail,)
  print 'Build host: %s' % (r.BuildHost,)
  print 'Build timestamp: %s' % (datetime_from_ms(r.BuildTimestamp),)
  print 'Current time: %s' % (datetime_from_ms(r.CurrentTime),)
  print 'Current working directory: %s' % (r.CurrentWorkingDirectory,)
  print 'Daemon version:'
  print '\tBuild version: %s' % (r.DaemonVersion.BuildVersion,)
  print '\tFull version: %s' % (r.DaemonVersion.FullVersion,)
  print '\tMajor version: %s' % (r.DaemonVersion.MajorVersion,)
  print '\tMinor version: %s' % (r.DaemonVersion.MinorVersion,)
  print 'Disks: %s' % (', '.join(r.Disks),)
  print 'Groups: %s' % (', '.join(r.Groups),)
  print 'Host name: %s' % (r.HostName,)
  print 'IP address: %s' % (r.IpAddress,)
  print 'Java version:'
  print '\tRuntimeName: %s' % (r.JavaVersion.RuntimeName,)
  print '\tRuntimeVersion: %s' % (r.JavaVersion.RuntimeVersion,)
  print '\tSpecificationVersion: %s' % (r.JavaVersion.SpecificationVersion,)
  print '\tVersion: %s' % (r.JavaVersion.Version,)
  print 'Mail relay: %s' % (r.MailRelay,)
  print 'Platform:'
  print '\tName: %s' % (r.Platform.Name,)
  print '\tSuffix: %s' % (r.Platform.Suffix,)
  print '\tVersion: %s' % (r.Platform.Version,)
  print 'Project: %s' % (r.Project,)
  print 'Properties: %s' % (', '.join(r.Properties),)
  print 'Uptime: %s' % (duration_from_ms(r.Uptime),)
  print 'V3 identity: %s' % (r.V3Identity,)

def _do_is_daemon_ready(tooloptions):
  '''Outputs "True" or "False", then returns.'''
  print is_daemon_ready(tooloptions)

def _do_is_daemon_ready_quiet(tooloptions):
  '''Outputs nothing, exits with 1 for "False" and returns for "True".'''
  if not is_daemon_ready(tooloptions): sys.exit(1)

_QUERY_AUS = {
  'accessType': lambda r: r.AccessType,
  'auConfiguration': lambda r: '<AuConfiguration>',
  'auId': lambda r: r.AuId,
  'availableFromPublisher': lambda r: r.AvailableFromPublisher,
  'contentSize': lambda r: r.ContentSize,
  'crawlPool': lambda r: r.CrawlPool,
  'crawlProxy': lambda r: r.CrawlProxy,
  'crawlWindow': lambda r: r.CrawlWindow,
  'creationTime': lambda r: datetime_from_ms(r.CreationTime),
  'currentlyCrawling': lambda r: r.CurrentlyCrawling,
  'currentlyPolling': lambda r: r.CurrentlyPolling,
  'diskUsage': lambda r: r.DiskUsage,
  'isBulkContent': lambda r: r.IsBulkContent,
  'lastCompletedCrawl': lambda r: datetime_from_ms(r.LastCompletedCrawl),
  'lastCompletedPoll': lambda r: datetime_from_ms(r.LastCompletedPoll),
  'lastCrawl': lambda r: datetime_from_ms(r.LastCrawl),
  'lastCrawlResult': lambda r: r.LastCrawlResult,
  'lastPoll': lambda r: datetime_from_ms(r.LastPoll),
  'lastPollResult': lambda r: r.LastPollResult,
  'name': lambda r: r.Name,
  'newContentCrawlUrls': lambda r: r.NewContentCrawlUrls,
  'peerAgreements': lambda r: '<PeerAgreements>',
  'pluginName': lambda r: r.PluginName,
  'publishingPlatform': lambda r: r.PublishingPlatform,
  'recentPollAgreement': lambda r: r.RecentPollAgreement,
  'repositoryPath': lambda r: r.RepositoryPath,
  'subscriptionStatus': lambda r: r.SubscriptionStatus,
  'substanceState': lambda r: r.SubstanceState,
  'tdbPublisher': lambda r: r.TdbPublisher,
  'tdbYear': lambda r: r.TdbYear,
  'urlStems': lambda r: r.UrlStems,
  'volume': lambda r: r.Volume
}

def _do_query_aus(tooloptions):
  query = 'SELECT %s' % (', '.join(tooloptions.get_fields()),)
  where = tooloptions.get_where()
  if where: query = '%s WHERE %s' % (query, where)
  tooloptions.set_query(query)
  recs = query_aus(tooloptions)
  fieldfuncs = [_QUERY_AUS[f] for f in tooloptions.get_fields()]
  for r in recs: _output_record(tooloptions, [f(r) for f in fieldfuncs])

if __name__ == '__main__':
  parser = _DaemonStatusServiceToolOptions.make_parser()
  (opts, args) = parser.parse_args()
  tooloptions = _DaemonStatusServiceToolOptions(parser, opts, args)
  if tooloptions.is_get_au_status(): _do_get_au_status(tooloptions)
  elif tooloptions.is_get_auids(): _do_get_auids(tooloptions)
  elif tooloptions.is_get_auids_names(): _do_get_auids_names(tooloptions)
  elif tooloptions.is_get_peer_agreements(): _do_get_peer_agreements(tooloptions)
  elif tooloptions.is_get_platform_configuration(): _do_get_platform_configuration(tooloptions)
  elif tooloptions.is_is_daemon_ready(): _do_is_daemon_ready(tooloptions)
  elif tooloptions.is_is_daemon_ready_quiet(): _do_is_daemon_ready_quiet(tooloptions)
  elif tooloptions.is_query_aus(): _do_query_aus(tooloptions)

