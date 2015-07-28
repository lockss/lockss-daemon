#!/usr/bin/env python

'''A module to interact with the LOCKSS daemon status service via its Web
Services API.'''

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

__version__ = '0.2.2'

import DaemonStatusServiceImplService_client
from ZSI.auth import AUTH

import datetime
import getpass
import optparse
import sys

#
# External
#

def auth(u, p):
  '''Makes a ZSI authentication object suitable for the methods in this module.
  Parameters:
  - u (string): a UI username
  - p (string): a UI password
  '''
  return (AUTH.httpbasic, u, p)

def datetime_from_ms(ms):
  '''Returns a datetime instance from a date and time expressed in milliseconds
  since epoch (or None if the input is None).
  Parameters:
  - ms (numeric): a number of milliseconds since epoch
  '''
  if ms is None: return None
  return datetime.datetime.fromtimestamp(ms / 1000)

def duration_from_ms(ms):
  '''Returns an approximate text representation of the number of milliseconds
  given. The result is of one of the following forms:
  - 123ms (milliseconds)
  - 12s (seconds)
  - 12m34s (minutes and seconds)
  - 12h34m56s (hours, minutes and seconds)
  - 1d23h45m (days, hours and minutes)
  - 4w3d21h (weeks, days and hours)
  Parameters:
  ms (numeric): a number of milliseconds
  '''
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

def get_au_status(host, auth, auid):
  '''Performs a getAuStatus operation on the given host for the given AUID, and
  returns a record with these fields:
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
  - Year (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  req = DaemonStatusServiceImplService_client.getAuStatus()
  req.AuId = auid
  return _ws_port(host, auth).getAuStatus(req).Return

def get_auids(host, auth):
  '''Convenience call to get_auids_names() that returns a list of only AUIDs.
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  '''
  return [r.Id for r in get_auids_names(host, auth)]

def get_auids_names(host, auth):
  '''Performs a getAuids operation on the given host, which really produces a
  sequence of all AUIDs with the AU names, and returns a list of records with
  these fields:
  - Id (string)
  - Name (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  '''
  req = DaemonStatusServiceImplService_client.getAuIds()
  return _ws_port(host, auth).getAuIds(req).Return

def get_peer_agreements(host, auth, auid):
  '''Convenience call to query_aus() that returns the PeerAgreements list for
  the given AUID (or None if there is no such AUID). The PeerAgreements list is
  a list of records with these fields:
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
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - auid (string): an AUID
  '''
  res = query_aus(host, auth, 'peerAgreements', 'auId = "%s"' % (auid,))
  if len(res) == 0: return None
  else: return res[0].PeerAgreements

def get_platform_configuration(host, auth):
  '''Performs a getPlatformConfiguration operation on the given host and returns
  a record with these fields:
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
  - V3Identity (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  '''
  req = DaemonStatusServiceImplService_client.getPlatformConfiguration()
  return _ws_port(host, auth).getPlatformConfiguration(req).Return

def is_daemon_ready(host, auth):
  '''Performs an isDaemonReady operation on the given host and returns True or
  False.
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  '''
  req = DaemonStatusServiceImplService_client.isDaemonReady()
  return _ws_port(host, auth).isDaemonReady(req).Return

# Special value for query-type operations
STAR = '*'

def query_aus(host, auth, select, where=None):
  '''Performs a queryAus operation on the given host, using the given field
  name(s) to build a SELECT clause (or the special value
  daemonstatusservice.STAR to mean all fields), optionally using the given
  string to build a WHERE clause, and returns a list of records with these
  fields (populated or not depending on the SELECT clause):
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
  - Volume (string)
  Parameters:
  - host (string): a host:port pair
  - auth (ZSI authentication object): an authentication object
  - select (string or list of strings): if a list of strings, the field names to
  be used in the SELECT clause; if a string, the single field name to be used in
  the SELECT clause; if the special value daemonstatusservice.STAR, all fields
  - where (string): optional statement for the WHERE clause (default: None)
  Raises:
  - ValueError if select is not of the right types
  '''
  if type(select) is list: query = 'SELECT %s' % (', '.join(select))
  elif type(select) is str: query = 'SELECT %s' % (select,)
  else: raise ValueError, 'invalid type for select parameter: %s' % (type(select),)
  if where is not None: query = '%s WHERE %s' % (query, where)
  req = DaemonStatusServiceImplService_client.queryAus()
  req.AuQuery = query
  return _ws_port(host, auth).queryAus(req).Return

#
# Internal
#

def _ws_port(host, auth, tracefile=None):
  url = 'http://%s/ws/DaemonStatusService' % (host,)
  locator = DaemonStatusServiceImplService_client.DaemonStatusServiceImplServiceLocator()
  if tracefile is None: return locator.getDaemonStatusServiceImplPort(url=url, auth=auth)
  else: return locator.getDaemonStatusServiceImplPort(url=url, auth=auth, tracefile=tracefile)

#
# Command line tool
#

class _DaemonStatusServiceOptions(object):
  def __init__(self, parser, opts, args):
    super(_DaemonStatusServiceOptions, self).__init__()
    if len(filter(None, [opts.get_au_status, opts.get_auids, opts.get_auids_names, opts.get_peer_agreements, opts.get_platform_configuration, opts.is_daemon_ready, opts.is_daemon_ready_quiet, opts.query_aus])) != 1:
      parser.error('exactly one of --get-au-status, --get-auids, --get-auids-names, --get-peer-agreements, --get-platform-configuration, --is-daemon-ready, --is-daemon-ready-quiet, --query_aus is required')
    if opts.where and not any([opts.query_aus]):
      parser.error('--where can only be applied to --query-aus')
    # host
    if len(args) != 1: parser.error('exactly one host is required')
    self.host = args[0]
    # get_auids/get_auids_names/get_platform_configuration/is_daemon_ready/is_daemon_ready_quiet
    self.get_auids = opts.get_auids
    self.get_auids_names = opts.get_auids_names
    self.get_platform_configuration = opts.get_platform_configuration
    self.is_daemon_ready = opts.is_daemon_ready
    self.is_daemon_ready_quiet = opts.is_daemon_ready_quiet
    # auid/get_au_status/get_peer_agreements
    self.auid = None
    self.get_au_status = opts.get_au_status is not None
    if self.get_au_status: self.auid = opts.get_au_status
    self.get_peer_agreements = opts.get_peer_agreements is not None
    if self.get_peer_agreements: self.auid = opts.get_peer_agreements
    # fields/query_aus/where
    self.fields = None
    self.where = opts.where or None
    self.query_aus = opts.query_aus is not None
    if self.query_aus:
      self.fields = [s.strip() for s in opts.query_aus.split(',')]
      errfields = filter(lambda f: f not in _QUERY_AUS, self.fields)
      if len(errfields) == 1: parser.error('unknown field: %s' % (errfields[0],))
      elif len(errfields) > 1: parser.error('unknown fields: %s' % (', '.join(errfields),))
    # auth
    u = opts.username or getpass.getpass('UI username: ')
    p = opts.password or getpass.getpass('UI password: ')
    self.auth = auth(u, p)

  @staticmethod
  def make_parser():
    usage = '%prog [OPTIONS] HOST'
    parser = optparse.OptionParser(version=__version__, description=__doc__, usage=usage)
    group = optparse.OptionGroup(parser, 'Host', 'The host:port pair HOST is specified as the single argument on the command line')
    group.add_option('--password', metavar='PASS', help='UI password (default: interactive prompt)')
    group.add_option('--username', metavar='USER', help='UI username (default: interactive prompt)')
    parser.add_option_group(group)
    group = optparse.OptionGroup(parser, 'Daemon operations')
    group.add_option('--get-auids', action='store_true', default=False, help='Outputs all AUIDs')
    group.add_option('--get-auids-names', action='store_true', default=False, help='Outputs all AUIDs and names')
    group.add_option('--get-platform-configuration', action='store_true', default=False, help='Outputs the platform configuration')
    group.add_option('--is-daemon-ready', action='store_true', default=False, help='Outputs True or False, always exits with 0')
    group.add_option('--is-daemon-ready-quiet', action='store_true', default=False, help='Outputs nothing, exits with 0 for True and 1 for False')
    parser.add_option_group(group)
    group = optparse.OptionGroup(parser, 'AU operations')
    group.add_option('--get-au-status', metavar='AUID', help='Outputs all status information for the AUID')
    group.add_option('--get-peer-agreements', metavar='AUID', help='Outputs the peer agreements for the AUID')
    parser.add_option_group(group)
    group = optparse.OptionGroup(parser, 'Query operations')
    group.add_option('--query-aus', metavar='CSFIELDS', help='Comma-separated fields for the SELECT clause of an AU query; chosen among %s ' % (sorted(_QUERY_AUS),))
    group.add_option('--where', help='optional WHERE clause for query operations')
    parser.add_option_group(group)
    return parser

def _output_record(options, lst):
  print '\t'.join([str(x) for x in lst])

def _do_get_au_status(options):
  r = get_au_status(options)
  print 'AUID: %s' % (options.auid,)
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

def _do_get_auids(options):
  for auid in get_auids(options.host, options.auth): print auid

def _do_get_auids_names(options):
  for r in get_auids_names(options.host, options.auth): _output_record(options, [r.Id, r.Name])

def _do_get_peer_agreements(options):
  pa = get_peer_agreements(options.host, options.auth, options.auid)
  if pa is None:
    print 'No such AUID'
    return
  for pae in pa:
    for ae in pae.Agreements.Entry:
      _output_record(options, [pae.PeerId, ae.Key, ae.Value.PercentAgreement, datetime_from_ms(ae.Value.PercentAgreementTimestamp), ae.Value.HighestPercentAgreement, datetime_from_ms(ae.Value.HighestPercentAgreementTimestamp)])

def _do_get_platform_configuration(options):
  r = get_platform_configuration(options.host, options.auth)
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

def _do_is_daemon_ready(options):
  '''Outputs "True" or "False", then returns.'''
  print is_daemon_ready(options.host, options.auth)

def _do_is_daemon_ready_quiet(options):
  '''Outputs nothing, exits with 1 for "False" and returns for "True".'''
  if not is_daemon_ready(options.host, options.auth): sys.exit(1)

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

def _do_query_aus(options):
  recs = query_aus(options.host, options.auth, options.fields, options.where)
  fieldfuncs = [_QUERY_AUS[f] for f in options.fields]
  for r in recs: _output_record(options, [f(r) for f in fieldfuncs])

def _main():
  '''Main method.'''
  # Parse command line
  parser = _DaemonStatusServiceOptions.make_parser()
  (opts, args) = parser.parse_args()
  options = _DaemonStatusServiceOptions(parser, opts, args)
  # Dispatch
  if options.get_au_status: _do_get_au_status(options)
  elif options.get_auids: _do_get_auids(options)
  elif options.get_auids_names: _do_get_auids_names(options)
  elif options.get_peer_agreements: _do_get_peer_agreements(options)
  elif options.get_platform_configuration: _do_get_platform_configuration(options)
  elif options.is_daemon_ready: _do_is_daemon_ready(options)
  elif options.is_daemon_ready_quiet: _do_is_daemon_ready_quiet(options)
  elif options.query_aus: _do_query_aus(options)

# Main entry point
if __name__ == '__main__': _main()

