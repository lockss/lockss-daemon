#!/usr/bin/env python3

'''A library and a command line tool to interact with the LOCKSS daemon status
service via its legacy SOAP Web Services API.'''

__copyright__ = '''\
Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University.
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

__version__ = '0.7.0'

import argparse
import getpass
import itertools
from multiprocessing.dummy import Pool as ThreadPool
import os.path
import sys
from threading import Thread

try: import requests
except ImportError: sys.exit('The Python Requests module must be installed (or on the PYTHONPATH)')
import requests.auth

try: import zeep
except ImportError: sys.exit('The Python Zeep module must be installed (or on the PYTHONPATH)')
import zeep.transports
import zeep.exceptions

from wsutil import datems, datetimems, durationms, requests_basic_auth

#
# Library
#

def get_au_status(host, auth, auid):
    '''Performs a getAuStatus operation on the given host for the given AUID, and
    returns a record with these fields (or None if zeep.exceptions.Fault with
    'No Archival Unit with provided identifier' is raised):
    - accessType (string)
    - availableFromPublisher (boolean)
    - contentSize (numeric)
    - crawlPool (string)
    - crawlProxy (string)
    - crawlWindow (string)
    - creationTime (numeric)
    - currentlyCrawling (boolean)
    - currentlyPolling (boolean)
    - diskUsage (numeric)
    - journalTitle (string)
    - lastCompletedCrawl (numeric)
    - lastCompletedPoll (numeric)
    - lastCrawl (numeric)
    - lastCrawlResult (string)
    - lastCompletedDeepCrawl (numeric)
    - lastDeepCrawl (numeric)
    - lastDeepCrawlResult (string)
    - lastCompletedDeepCrawlDepth (numeric)
    - lastPoll (numeric)
    - lastPollResult (string)
    - lastMetadataIndex (numeric)
    - pluginName (string)
    - provider (string)
    - publisher (string)
    - publishingPlatform (string)
    - recentPollAgreement (floating point)
    - repository (string)
    - status (string)
    - subscriptionStatus (string)
    - substanceState (string)
    - volume (string) (the AU name)
    - year (string)
    Parameters:
    - host (string): a host:port pair
    - auth (requests.auth.AuthBase object): an authentication object
    - auid (string): an AUID
    '''
    client = _make_client(host, auth)
    try:
        return client.service.getAuStatus(auId=auid)
    except zeep.exceptions.Fault as e:
        if e.message == 'No Archival Unit with provided identifier':
            return None
        raise

def get_au_urls(host, auth, auid, prefix=None):
    '''Performs a getAuUrls operation on the given host for the given AUID and
    returns a list of URLs (strings) in the AU. If the optional prefix argument is
    given, limits the results to URLs with that prefix (including the URL itself).

    Parameters:
    - host (string): a host:port pair
    - auth (requests.auth.AuthBase object): an authentication object
    - auid (string): an AUID
    - prefix (string): a URL prefix (default: None)
    '''
    client = _make_client(host, auth)
    return client.service.getAuUrls(auId=auid, url=prefix)

def get_au_type_urls(host, auth, auid, typ):
    '''Performs a queryAus operation on the given host for the given AUID and
    selects only the url list of type  given (articleUrls,substanceUrls) for the AU.  

    Parameters:
    - host (string): a host:port pair
    - auth (requests.auth.AuthBase object): an authentication object
    - auid (string): an AUID
    - typ (string): one of 'articleUrls' or 'substanceUrls'
    '''
    res = query_aus(host, auth, typ, f'auId = "{auid}"')
    if len(res) == 0:
        return None
    elif typ == 'articleUrls':
        return res[0].articleUrls
    elif typ == 'substanceUrls':
        return res[0].substanceUrls
    else:
        raise RuntimeError(f'invalid type argument: {typ}')

def get_auids(host, auth):
    '''Performs a getAuIds operation on the given host, which really produces a
    sequence of all AUIDs with the AU names, and returns a list of records with
    these fields:
    - id (string)
    - name (string)
    Parameters:
    - host (string): a host:port pair
    - auth (requests.auth.AuthBase object): an authentication object
    '''
    client = _make_client(host, auth)
    return client.service.getAuIds()

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
                - "W_POR"
                - "W_POP"
                - "W_SYMMETRIC_POR"
                - "W_SYMMETRIC_POP"
                - "W_POR_HINT"
                - "W_POP_HINT"
                - "W_SYMMETRIC_POR_HINT"
                - "W_SYMMETRIC_POP_HINT"
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
    res = query_aus(host, auth, 'peerAgreements', f'auId = "{auid}"')
    if len(res) == 0: return None
    else: return res[0].peerAgreements

def get_platform_configuration(host, auth):
    '''Performs a getPlatformConfiguration operation on the given host and returns
    a record with these fields:
    - adminEmail (string)
    - buildHost (string)
    - buildTimestamp (numeric)
    - currentTime (numeric)
    - currentWorkingDirectory (string)
    - daemonVersion, a record with these fields:
        - buildVersion (numeric)
        - fullVersion (string)
        - majorVersion (numeric)
        - minorVersion (numeric)
    - disks (list of strings)
    - groups (list of strings)
    - hostName (string)
    - ipAddress (string)
    - javaVersion, a record with these fields:
        - runtimeName (string)
        - runtimeVersion (string)
        - specificationVersion (string)
        - version (string)
    - mailRelay (string)
    - platform, a record with these fields:
        - name (string)
        - suffix (string)
        - version (string)
    - project (string)
    - properties (list of strings)
    - uptime (numeric)
    - v3Identity (string)
    Parameters:
    - host (string): a host:port pair
    - auth (requests.auth.AuthBase object): an authentication object
    '''
    client = _make_client(host, auth)
    return client.service.getPlatformConfiguration()

def is_daemon_ready(host, auth):
    '''Performs an isDaemonReady operation on the given host and returns True or
    False.
    Parameters:
    - host (string): a host:port pair
    - auth (requests.auth.AuthBase object): an authentication object
    '''
    client = _make_client(host, auth)
    return client.service.isDaemonReady()

def query_aus(host, auth, select, where=None):
    '''Performs a queryAus operation on the given host, using the given field
    names to build a SELECT clause, optionally using the given string to build a
    WHERE clause, and returns a list of records with these fields (populated or
    not depending on the SELECT clause):
    - accessType (string)
    - articleUrls (list of strings)
    - auConfiguration, a record with these fields:
        - defParams, a list of records with these fields:
            - key (string)
            - value (string)
        - nonDefParams, a list of records with these fields:
            - key (string)
            - value (string)
    - auId (string)
    - availableFromPublisher (boolean)
    - contentSize (numeric)
    - crawlPool (string)
    - crawlProxy (string)
    - crawlWindow (string)
    - creationTime (numeric)
    - currentlyCrawling (boolean)
    - currentlyPolling (boolean)
    - diskUsage (numeric)
    - highestPollAgreement (numeric)
    - isBulkContent (boolean)
    - journalTitle (string)
    - lastCompletedCrawl (numeric)
    - lastCompletedPoll (numeric)
    - lastCrawl (numeric)
    - lastCrawlResult (string)
    - lastCompletedDeepCrawl (numeric)
    - lastDeepCrawl (numeric)
    - lastDeepCrawlResult (string)
    - lastCompletedDeepCrawlDepth (numeric)
    - lastPoll (numeric)
    - lastPollResult (string)
    - lastMetadataIndex (numeric)
    - name (string)
    - newContentCrawlUrls (list of strings)
    - peerAgreements, a list of records with these fields:
        - agreements, a record with these fields:
            - entry, a list of records with these fields:
                - key, a string among:
                    - "POR"
                    - "POP"
                    - "SYMMETRIC_POR"
                    - "SYMMETRIC_POP"
                    - "POR_HINT"
                    - "POP_HINT"
                    - "SYMMETRIC_POR_HINT"
                    - "SYMMETRIC_POP_HINT"
                    - "W_POR"
                    - "W_POP"
                    - "W_SYMMETRIC_POR"
                    - "W_SYMMETRIC_POP"
                    - "W_POR_HINT"
                    - "W_POP_HINT"
                    - "W_SYMMETRIC_POR_HINT"
                    - "W_SYMMETRIC_POP_HINT"
                - value, a record with these fields:
                    - HighestPercentAgreement (floating point)
                    - HighestPercentAgreementTimestamp (numeric)
                    - PercentAgreement (floating point)
                    - PercentAgreementTimestamp (numeric)
        - peerId (string)
    - pluginName (string)
    - publishingPlatform (string)
    - recentPollAgreement (numeric)
    - repositoryPath (string)
    - subscriptionStatus (string)
    - substanceState (string)
    - tdbProvider (string)
    - tdbPublisher (string)
    - tdbYear (string)
    - urlStems (list of strings)
    - urls, a list of records with these fields:
        - currentVersionSize (numeric)
        - url (string)
        - versionCount (numeric)
    - volume (string)
    Parameters:
    - host (string): a host:port pair
    - auth (requests.auth.AuthBase object): an authentication object
    - select (string or list of strings): if a list of strings, the field names to
    be used in the SELECT clause; if a string, the single field name to be used in
    the SELECT clause
    - where (string): optional statement for the WHERE clause (default: None)
    Raises:
    - ValueError if select is not of the right type
    '''
    if type(select) is list:
        query = f'SELECT {", ".join(select)}'
    elif type(select) is str:
        query = f'SELECT {select}'
    else:
        raise ValueError(f'invalid type for select parameter: {type(select)}')
    if where is not None:
        query = f'{query} WHERE {where}'
    client = _make_client(host, auth)
    return client.service.queryAus(auQuery=query)

def query_crawls(host, auth, select, where=None):
    '''Performs a queryCrawls operation on the given host, using the given field
    names to build a SELECT clause, optionally using the given string to build a
    WHERE clause, and returns a list of records with these fields (populated or
    not depending on the SELECT clause):
    - auId (string)
    - auName (string)
    - bytesFetchedCount (long)
    - crawlKey (string)
    - crawlStatus (string)
    - crawlType (string)
    - duration (long)
    - linkDepth (int)
    - mimeTypeCount (int)
    - mimeTypes (list of strings)
    - offSiteUrlsExcludedCount (int)
    - pagesExcluded (list of strings)
    - pagesExcludedCount (int)
    - pagesFetched (list of strings)
    - pagesFetchedCount (int)
    - pagesNotModified (list of strings)
    - pagesNotModifiedCount (int)
    - pagesParsed (list of strings)
    - pagesParsedCount (int)
    - pagesPending (list of strings)
    - pagesPendingCount (int)
    - pagesWithErrors, a list of records with these fields:
        - message (string)
        - severity (string)
        - url (string)
    - pagesWithErrorsCount (int)
    - refetchDepth (int)
    - sources (list of strings)
    - startTime (long)
    - startingUrls (list of strings)
    '''
    if type(select) is list:
        query = f'SELECT {", ".join(select)}'
    elif type(select) is str:
        query = f'SELECT {select}'
    else:
        raise ValueError(f'invalid type for select parameter: {type(select)}')
    if where is not None:
        query = f'{query} WHERE {where}'
    client = _make_client(host, auth)
    return client.service.queryCrawls(crawlQuery=query)

#
# Command line tool
#

class _DaemonStatusServiceOptions(object):

  @staticmethod
  def make_parser():
    usage = '%(prog)s {--host=HOST|--hosts=HFILE}... [OPTIONS]'
    parser = argparse.ArgumentParser(description=__doc__, usage=usage)
    parser.add_argument('--version', '-V', action='version', version=__version__)
    # Hosts
    group = parser.add_argument_group('Target hosts')
    group.add_argument('--host', action='append', default=list(), help='add host:port pair to list of target hosts')
    group.add_argument('--hosts', action='append', default=list(), metavar='HFILE', help='add host:port pairs in HFILE to list of target hosts')
    group.add_argument('--password', metavar='PASS', help='UI password (default: interactive prompt)')
    group.add_argument('--username', metavar='USER', help='UI username (default: interactive prompt)')
    # AUIDs
    group = parser.add_argument_group('Target AUIDs')
    group.add_argument('--auid', action='append', default=list(), help='add AUID to list of target AUIDs')
    group.add_argument('--auids', action='append', default=list(), metavar='AFILE', help='add AUIDs in AFILE to list of target AUIDs')
    # Daemon operations
    group = parser.add_argument_group('Daemon operations')
    group.add_argument('--get-platform-configuration', action='store_true', help='output platform configuration information for target hosts; narrow down with optional --select list chosen among %s' % (', '.join(sorted(_PLATFORM_CONFIGURATION)),))
    group.add_argument('--is-daemon-ready', action='store_true', help='output True/False table of ready status of target hosts; always exit with 0')
    group.add_argument('--is-daemon-ready-quiet', action='store_true', help='output nothing; exit with 0 if all target hosts are ready, 1 otherwise')
    # AUID operations
    group = parser.add_argument_group('AU operations')
    group.add_argument('--get-au-status', action='store_true', help='output status information about target AUIDs; narrow down output with optional --select list chosen among %s' % (', '.join(sorted(_AU_STATUS)),))
    group.add_argument('--get-au-urls', action='store_true', help='output URLs in one AU on one host')
    group.add_argument('--get-au-article-urls', action='store_true', help='output article URLs in one AU on one host')
    group.add_argument('--get-au-subst-urls', action='store_true', help='output substance URLs in one AU on one host')
    group.add_argument('--get-auids', action='store_true', help='output True/False table of all AUIDs (or target AUIDs if specified) present on target hosts')
    group.add_argument('--get-auids-names', action='store_true', help='output True/False table of all AUIDs (or target AUIDs if specified) and their names present on target hosts')
    group.add_argument('--get-peer-agreements', action='store_true', help='output peer agreements for one AU on one hosts')
    group.add_argument('--query-aus', action='store_true', help='perform AU query (with optional --where clause) with --select list chosen among %s' % (', '.join(sorted(_QUERY_AUS)),))
    # Crawl operations
    group = parser.add_argument_group('Crawl operations')
    group.add_argument('--query-crawls', action='store_true', help='perform crawl query (with optional --where clause) with --select list chosen among %s' % (', '.join(sorted(_QUERY_CRAWLS)),))
    # Other options
    group = parser.add_argument_group('Other options')
    group.add_argument('--group-by-field', action='store_true', help='group results by field instead of host')
    group.add_argument('--no-special-output', action='store_true', help='no special output format for a single target host')
    group.add_argument('--prefix', help='prefix URL for --get-au-urls')
    group.add_argument('--select', metavar='FIELDS', help='comma-separated list of fields for narrower output')
    group.add_argument('--threads', type=int, help='max parallel jobs allowed (default: no limit)')
    group.add_argument('--where', help='optional WHERE clause for query operations')
    return parser

  def __init__(self, parser, args):
    super(_DaemonStatusServiceOptions, self).__init__()
#FIXME    if len(args) > 0: parser.error('extraneous arguments: %s' % (' '.join(args)))
    if len(list(filter(None, [args.get_au_status, args.get_au_urls, args.get_au_article_urls, args.get_au_subst_urls, args.get_auids, args.get_auids_names, args.get_peer_agreements, args.get_platform_configuration, args.is_daemon_ready, args.is_daemon_ready_quiet, args.query_aus, args.query_crawls]))) != 1:
      parser.error('exactly one of --get-au-status, --get-au-urls, --get-au-article-urls, --get-au-subst-urls,--get-auids, --get-auids-names, --get-peer-agreements, --get-platform-configuration, --is-daemon-ready, --is-daemon-ready-quiet, --query-aus --query-crawls is required')
    if len(args.auid) + len(args.auids) > 0 and not any([args.get_au_status, args.get_au_urls, args.get_au_article_urls, args.get_au_subst_urls, args.get_auids, args.get_auids_names, args.get_peer_agreements]):
      parser.error('--auid, --auids can only be applied to --get-au-status, --get-au-urls, --get-au-article-urls, --get-au-subst-urls, --get-auids, --get-auids-names, --get-peer-agreements')
    if args.prefix and not args.get_au_urls:
      parser.error('--prefix can only be applied to --get-au-urls')
    if args.select and not any([args.get_au_status, args.get_platform_configuration, args.query_aus, args.query_crawls]):
      parser.error('--select can only be applied to --get-au-status, --get-platform-configuration, --query-aus, --query-crawls')
    if args.where and not any([args.query_aus, args.query_crawls]):
      parser.error('--where can only be applied to --query-aus, --query-crawls')
    if args.group_by_field and not any([args.get_au_status, args.query_aus]):
      parser.error('--group-by-field can only be applied to --get-au-status, --query-aus')
    # hosts
    self.hosts = args.host[:]
    for f in args.hosts: self.hosts.extend(_file_lines(f))
    if len(self.hosts) == 0: parser.error('at least one target host is required')
    # auids
    self.auids = args.auid[:]
    for f in args.auids: self.auids.extend(_file_lines(f))
    # get_auids/get_auids_names/is_daemon_ready/is_daemon_ready_quiet
    self.get_auids = args.get_auids
    self.get_auids_names = args.get_auids_names
    self.is_daemon_ready = args.is_daemon_ready
    self.is_daemon_ready_quiet = args.is_daemon_ready_quiet
    # get_platform_configuration/select
    self.get_platform_configuration = args.get_platform_configuration
    if self.get_platform_configuration:
      self.select = self.__init_select(parser, args, _PLATFORM_CONFIGURATION)
    # get_au_status/select
    self.get_au_status = args.get_au_status
    if self.get_au_status:
      if len(self.auids) == 0: parser.error('at least one target AUID is required with --get-au-status')
      self.select = self.__init_select(parser, args, _AU_STATUS)
    # get_au_urls
    self.get_au_urls = args.get_au_urls
    if self.get_au_urls:
      if len(self.hosts) != 1: parser.error('only one target host is allowed with --get-au-urls')
      if len(self.auids) != 1: parser.error('only one target AUID is allowed with --get-au-urls')
    self.prefix = args.prefix
    # get article url list
    self.get_au_article_urls = args.get_au_article_urls
    if self.get_au_article_urls:
      if len(self.hosts) != 1: parser.error('only one target host is allowed with --get-au-article-urls')
      if len(self.auids) != 1: parser.error('only one target AUID is allowed with --get-au-article-urls')
    # get substance url list
    self.get_au_subst_urls = args.get_au_subst_urls
    if self.get_au_subst_urls:
      if len(self.hosts) != 1: parser.error('only one target host is allowed with --get-au-subst-urls')
      if len(self.auids) != 1: parser.error('only one target AUID is allowed with --get-au-subst-urls')
    # get_peer_agreements
    self.get_peer_agreements = args.get_peer_agreements
    if self.get_peer_agreements:
      if len(self.hosts) != 1: parser.error('only one target host is allowed with --get-peer-agreements')
      if len(self.auids) != 1: parser.error('only one target AUID is allowed with --get-peer-agreements')
    # query_aus/select/where
    self.query_aus = args.query_aus
    if self.query_aus:
      self.select = self.__init_select(parser, args, _QUERY_AUS)
      self.where = args.where
    # query_crawls/select/where
    self.query_crawls = args.query_crawls
    if self.query_crawls:
      if len(self.hosts) != 1: parser.error('only one target host is allowed with --query-crawls')
      self.select = self.__init_select(parser, args, _QUERY_CRAWLS)
      self.where = args.where
    # group_by_field/no_special_output
    self.group_by_field = args.group_by_field
    self.no_special_output = args.no_special_output
    # threads
    self.threads = args.threads or len(self.hosts)
    # auth
    u = args.username or getpass.getpass('UI username: ')
    p = args.password or getpass.getpass('UI password: ')
    self.auth = requests_basic_auth(u, p)

  def __init_select(self, parser, args, field_dict):
    if args.select is None: return sorted(field_dict)
    fields = [s.strip() for s in args.select.split(',')]
    errfields = list(filter(lambda f: f not in field_dict, fields))
    if len(errfields) == 1: parser.error('unknown field: %s' % (errfields[0],))
    if len(errfields) > 1: parser.error('unknown fields: %s' % (', '.join(errfields),))
    return fields

# Last modified 2018-03-19 for unicode support and boolean False when boolean is None
def _output_record(options, lst):
  print('\t'.join([x.decode('utf-8') if type(x) is bytes else str(x or False) if type(x)==type(True) else str(x or '') for x in lst]))

# Last modified 2015-08-05
def _output_table(options, data, rowheaders, lstcolkeys):
  colkeys = [x for x in itertools.product(*lstcolkeys)]
  for j in range(len(lstcolkeys)):
    if j < len(lstcolkeys) - 1: rowpart = [''] * len(rowheaders)
    else: rowpart = rowheaders
    _output_record(options, rowpart + [x[j] for x in colkeys])
  for rowkey in sorted(set([k[0] for k in data])):
    _output_record(options, list(rowkey) + [data.get((rowkey, colkey)) for colkey in colkeys])

# Last modified 2020-10-01
def _file_lines(fstr):
    with open(os.path.expanduser(fstr)) as f: ret = list(filter(lambda y: len(y) > 0, [x.partition('#')[0].strip() for x in f]))
    if len(ret) == 0: sys.exit(f'Error: {fstr} contains no meaningful lines')
    return ret

_AU_STATUS = {
  'accessType': ('Access type', lambda r: r.accessType),
  'availableFromPublisher': ('Available from publisher', lambda r: r.availableFromPublisher),
  'contentSize': ('Content size', lambda r: r.contentSize),
  'crawlPool': ('Crawl pool', lambda r: r.crawlPool),
  'crawlProxy': ('Crawl proxy', lambda r: r.crawlProxy),
  'crawlWindow': ('Crawl window', lambda r: r.crawlWindow),
  'creationTime': ('Creation time', lambda r: datetimems(r.creationTime)),
  'currentlyCrawling': ('Currently crawling', lambda r: r.currentlyCrawling),
  'currentlyPolling': ('Currently polling', lambda r: r.currentlyPolling),
  'diskUsage': ('Disk usage', lambda r: r.diskUsage),
  'journalTitle': ('Journal title', lambda r: r.journalTitle),
  'lastCompletedCrawl': ('Last completed crawl', lambda r: datetimems(r.lastCompletedCrawl)),
  'lastCompletedPoll': ('Last completed poll', lambda r: datetimems(r.lastCompletedPoll)),
  'lastCrawl': ('Last crawl', lambda r: datetimems(r.lastCrawl)),
  'lastCrawlResult': ('Last crawl result', lambda r: r.lastCrawlResult),
  'lastCompletedDeepCrawl': ('Last completed deep crawl', lambda r: datetimems(r.lastCompletedDeepCrawl)),
  'lastDeepCrawl': ('Last deep crawl', lambda r: datetimems(r.lastDeepCrawl)),
  'lastDeepCrawlResult': ('Last deep crawl result',  lambda r: r.lastDeepCrawlResult),
  'lastCompletedDeepCrawlDepth': ('Last completed deep crawl depth', lambda r: r.lastCompletedDeepCrawlDepth),
  'lastPoll': ('Last poll', lambda r: datetimems(r.lastPoll)),
  'lastPollResult': ('Last poll result', lambda r: r.lastPollResult),
  'lastMetadataIndex': ('Last metadata index', lambda r: datetimems(r.lastMetadataIndex)),
  'pluginName': ('Plugin name', lambda r: r.pluginName),
  'provider': ('Provider', lambda r: r.provider),
  'publisher': ('Publisher', lambda r: r.publisher),
  'publishingPlatform': ('Publishing platform', lambda r: r.publishingPlatform),
  'recentPollAgreement': ('Recent poll agreement', lambda r: r.recentPollAgreement),
  'repository': ('Repository', lambda r: r.repository),
  'status': ('Status', lambda r: r.status),
  'subscriptionStatus': ('Subscription status', lambda r: r.subscriptionStatus),
  'substanceState': ('Substance state', lambda r: r.substanceState),
  'volume': ('Volume name', lambda r: r.volume),
  'year': ('Year', lambda r: r.year)
}

def _do_get_au_status(options):
  headlamb = [_AU_STATUS[x] for x in options.select]
  data = dict()
  for host, auid, result in ThreadPool(options.threads).imap_unordered( \
      lambda _tup: (_tup[1], _tup[0], get_au_status(_tup[1], options.auth, _tup[0])), \
      itertools.product(options.auids, options.hosts)):
    if result is not None:
      for head, lamb in headlamb:
        if options.group_by_field: colkey = (head, host)
        else: colkey = (host, head)
        data[((auid,), colkey)] = lamb(result)
  _output_table(options, data, ['AUID'], [[x[0] for x in headlamb], sorted(options.hosts)] if options.group_by_field else [sorted(options.hosts), [x[0] for x in headlamb]])

def _do_get_au_urls(options):
  # Single request to a single host: unthreaded
  r = get_au_urls(options.hosts[0], options.auth, options.auids[0], options.prefix)
  for url in sorted(r): _output_record(options, [url])

def _do_get_au_article_urls(options):
  # Single request to a single host: unthreaded
  r = get_au_type_urls(options.hosts[0], options.auth, options.auids[0], "articleUrls")
  if r is not None:
    for url in sorted(r): _output_record(options, [url])

def _do_get_au_subst_urls(options):
  # Single request to a single host: unthreaded
  r = get_au_type_urls(options.hosts[0], options.auth, options.auids[0], "substanceUrls")
  if r is not None:
    for url in sorted(r): _output_record(options, [url])


def _do_get_auids(options):
  if len(options.auids) > 0:
    targetauids = set(options.auids)
    shouldskip = lambda a: a not in targetauids
  else: shouldskip = lambda a: False
  data = dict()
  auids = set()
  for host, result in ThreadPool(options.threads).imap_unordered( \
      lambda _host: (_host, get_auids(_host, options.auth)), \
      options.hosts):
    for r in result:
      if shouldskip(r.id): continue
      if options.get_auids_names: rowkey = (r.id, r.name)
      else: rowkey = (r.id,)
      data[(rowkey, (host,))] = True
      if r.id not in auids:
        auids.add(r.id)
        for h in options.hosts: data.setdefault((rowkey, (h,)), False)
  _output_table(options, data, ['AUID', 'Name'] if options.get_auids_names else ['AUID'], [sorted(options.hosts)])

_PLATFORM_CONFIGURATION = {
  'adminEmail': ('Admin e-mail', lambda r: r.adminEmail),
  'buildHost':  ('Build host', lambda r: r.buildHost),
  'buildTimestamp': ('Build timestamp', lambda r: datetimems(r.buildTimestamp)),
  'currentTime': ('Current time', lambda r: datetimems(r.currentTime)),
  'currentWorkingDirectory': ('Current working directory', lambda r: r.currentWorkingDirectory),
  'daemonBuildVersion': ('Daemon build version', lambda r: r.daemonVersion.buildVersion),
  'daemonFullVersion': ('Daemon full version', lambda r: r.daemonVersion.fullVersion),
  'daemonMajorVersion': ('Daemon major version', lambda r: r.daemonVersion.majorVersion),
  'daemonMinorVersion': ('Daemon minor version', lambda r: r.daemonVersion.minorVersion),
  'disks': ('Disks', lambda r: ', '.join(r.disks)),
  'groups': ('Groups', lambda r: ', '.join(r.groups)),
  'hostName': ('Host name', lambda r: r.hostName),
  'ipAddress': ('IP address', lambda r: r.ipAddress),
  'javaRuntimeName': ('Java runtime name', lambda r: r.javaVersion.runtimeName),
  'javaRuntimeVersion': ('Java runtime version', lambda r: r.javaVersion.runtimeVersion),
  'javaSpecificationVersion': ('Java specification version', lambda r: r.javaVersion.specificationVersion),
  'javaVersion': ('Java version', lambda r: r.javaVersion.version),
  'mailRelay': ('Mail relay', lambda r: r.mailRelay),
  'platformName': ('Platform name', lambda r: r.platform.name),
  'platformSuffix': ('Platform suffix', lambda r: r.platform.suffix),
  'platformVersion': ('Platform version', lambda r: r.platform.version),
  'project': ('Project', lambda r: r.project),
  'properties': ('Properties', lambda r: ', '.join(r.properties)),
  'uptime': ('Uptime', lambda r: durationms(r.uptime)),
  'v3Identity': ('V3 identity', lambda r: r.v3Identity)
}

def _do_get_peer_agreements(options):
  # Single request to a single host: unthreaded
  pa = get_peer_agreements(options.hosts[0], options.auth, options.auids[0])
  if pa is None:
    print('No such AUID')
    return
  for pae in pa:
    for ae in pae.Agreements.Entry:
      _output_record(options, [pae.PeerId, ae.Key, ae.Value.PercentAgreement, datetimems(ae.Value.PercentAgreementTimestamp), ae.Value.HighestPercentAgreement, datetimems(ae.Value.HighestPercentAgreementTimestamp)])

def _do_get_platform_configuration(options):
  headlamb = [_PLATFORM_CONFIGURATION[x] for x in options.select]
  data = dict()
  for host, result in ThreadPool(options.threads).imap_unordered( \
      lambda _host: (_host, get_platform_configuration(_host, options.auth)), \
      options.hosts):
    print(f'{result.v3Identity} {result["v3Identity"]}') ###FIXME
    for head, lamb in headlamb:
      data[((head,), (host,))] = lamb(result)
  _output_table(options, data, [''], [sorted(options.hosts)])

def _do_is_daemon_ready(options):
  data = dict()
  for host, result in ThreadPool(options.threads).imap_unordered( \
      lambda _host: (_host, is_daemon_ready(_host, options.auth)), \
      options.hosts):
    if options.is_daemon_ready_quiet and result is False: sys.exit(1)
    else: data[((host,), ('Daemon is ready',))] = result
  if options.is_daemon_ready_quiet: pass
  else: _output_table(options, data, ['Host'], [['Daemon is ready']])

_QUERY_AUS = {
  'accessType': ('Access type', lambda r: r.accessType),
  'articleUrls': ('Article URLs', lambda r: '<ArticleUrls>'),
  'auConfiguration': ('AU configuration', lambda r: '<AuConfiguration>'),
  'auId': ('AUID', lambda r: r.auId),
  'availableFromPublisher': ('Available from publisher', lambda r: r.availableFromPublisher),
  'contentSize': ('Content size', lambda r: r.contentSize),
  'crawlPool': ('Crawl pool', lambda r: r.crawlPool),
  'crawlProxy': ('Crawl proxy', lambda r: r.crawlProxy),
  'crawlWindow': ('Crawl window', lambda r: r.crawlWindow),
  'creationTime': ('Creation time', lambda r: datetimems(r.creationTime)),
  'currentlyCrawling': ('Currently crawling', lambda r: r.currentlyCrawling),
  'currentlyPolling': ('Currently polling', lambda r: r.currentlyPolling),
  'diskUsage': ('Disk usage', lambda r: r.diskUsage),
  'highestPollAgreement': ('Highest poll agreement', lambda r: r.highestPollAgreement),
  'isBulkContent': ('Is bulk content', lambda r: r.isBulkContent),
  'journalTitle': ('Title', lambda r: r.journalTitle),
  'lastCompletedCrawl': ('Last completed crawl', lambda r: datetimems(r.lastCompletedCrawl)),
  'lastCompletedPoll': ('Last completed poll', lambda r: datetimems(r.lastCompletedPoll)),
  'lastCrawl': ('Last crawl', lambda r: datetimems(r.lastCrawl)),
  'lastCrawlResult': ('Last crawl result', lambda r: r.lastCrawlResult),
  'lastCompletedDeepCrawl': ('Last completed deep crawl', lambda r: datetimems(r.lastCompletedDeepCrawl)),
  'lastDeepCrawl': ('Last deep crawl', lambda r: datetimems(r.lastDeepCrawl)),
  'lastDeepCrawlResult': ('Last deep crawl result',  lambda r: r.lastDeepCrawlResult),
  'lastCompletedDeepCrawlDepth': ('Last completed deep crawl depth', lambda r: r.lastCompletedDeepCrawlDepth),
  'lastPoll': ('Last poll', lambda r: datetimems(r.lastPoll)),
  'lastPollResult': ('Last poll result', lambda r: r.lastPollResult),
  'lastMetadataIndex': ('Last metadata index', lambda r: datetimems(r.lastMetadataIndex)),
  'name': ('Name', lambda r: r.name),
  'newContentCrawlUrls': ('New content crawl URLs', lambda r: '<NewContentCrawlUrls>'),
  'peerAgreements': ('Peer agreements', lambda r: '<PeerAgreements>'),
  'pluginName': ('Plugin name', lambda r: r.pluginName),
  'publishingPlatform': ('Publishing platform', lambda r: r.publishingPlatform),
  'recentPollAgreement': ('Recent poll agreement', lambda r: r.recentPollAgreement),
  'repositoryPath': ('Repository path', lambda r: r.repositoryPath),
  'subscriptionStatus': ('Subscription status', lambda r: r.subscriptionStatus),
  'substanceState': ('Substance state', lambda r: r.substanceState),
  'tdbProvider': ('TDB provider', lambda r: r.tdbProvider),
  'tdbPublisher': ('TDB publisher', lambda r: r.tdbPublisher),
  'tdbYear': ('TDB year', lambda r: r.tdbYear),
  'urlStems': ('URL stems', lambda r: '<UrlStems>'),
  'urls': ('URLs', lambda r: '<Urls>'),
  'volume': ('Volume', lambda r: r.volume)
}

def _do_query_aus(options):
  select_minus_auid = list(filter(lambda x: x != 'auId', options.select))
  auid_first_select = ['auId'] + select_minus_auid
  headlamb = [_QUERY_AUS[x] for x in select_minus_auid]
  data = dict()
  for host, result in ThreadPool(options.threads).imap_unordered( \
      lambda _host: (_host, query_aus(_host, options.auth, auid_first_select, options.where)), \
      options.hosts):
    for r in result:
      for head, lamb in headlamb:
        if options.group_by_field: colkey = (head, host)
        else: colkey = (host, head)
        data[((r.auId,), colkey)] = lamb(r)
  _output_table(options, data, ['AUID'], [[x[0] for x in headlamb], sorted(options.hosts)] if options.group_by_field else [sorted(options.hosts), [x[0] for x in headlamb]])

_QUERY_CRAWLS = {
  'auId': ('AUID', lambda r: r.auId),
  'auName': ('AU name', lambda r: r.auName),
  'bytesFetchedCount': ('Bytes Fetched', lambda r: r.bytesFetchedCount),
  'crawlKey': ('Crawl key', lambda r: r.crawlKey),
  'crawlStatus': ('Crawl status', lambda r: r.crawlStatus),
  'crawlType': ('Crawl type', lambda r: r.crawlType),
  'duration': ('Duration', lambda r: durationms(r.duration)),
  'linkDepth': ('Link depth', lambda r: r.linkDepth),
  'mimeTypeCount': ('MIME type count', lambda r: r.mimeTypeCount),
  'mimeTypes': ('MIME types', lambda r: '<MIME types>'),
  'offSiteUrlsExcludedCount': ('Off-site URLs excluded count', lambda r: r.offSiteUrlsExcludedCount),
  'pagesExcluded': ('Pages excluded', lambda r: '<Pages excluded>'),
  'pagesExcludedCount': ('Pages excluded count', lambda r: r.pagesExcludedCount),
  'pagesFetched': ('Pages fetched', lambda r: '<Pages fetched>'),
  'pagesFetchedCount': ('Pages fetched count', lambda r: r.pagesFetchedCount),
  'pagesNotModified': ('Pages not modified', lambda r: '<Pages not modified>'),
  'pagesNotModifiedCount': ('Pages not modified count', lambda r: r.pagesNotModifiedCount),
  'pagesParsed': ('Pages parsed', lambda r: '<Pages parsed>'),
  'pagesParsedCount': ('Pages parsed count', lambda r: r.pagesParsedCount),
  'pagesPending': ('Pages pending', lambda r: '<Pages pending>'),
  'pagesPendingCount': ('Pages pending count', lambda r: r.pagesPendingCount),
  'pagesWithErrors': ('Pages with errors', lambda r: '<Pages with errors>'),
  'pagesWithErrorsCount': ('Pages with errors count', lambda r: r.pagesWithErrorsCount),
  'refetchDepth': ('RefetchDepth', lambda r: r.refetchDepth),
  'sources': ('Sources', lambda r: '<Sources>'),
  'startTime': ('Start time', lambda r: datetimems(r.startTime)),
  'startingUrls': ('Starting URLs', lambda r: '<Starting URLs>')
}

def _do_query_crawls(options):
  # Single request to a single host: unthreaded
  select_minus_auid = list(filter(lambda x: x != 'auId', options.select))
  auid_first_select = ['auId'] + select_minus_auid
  headlamb = [_QUERY_CRAWLS[x] for x in select_minus_auid]
  data = dict()
  for r in query_crawls(options.hosts[0], options.auth, auid_first_select, options.where):
    for head, lamb in headlamb:
      data[((r.auId,), (head,))] = lamb(r)
  _output_table(options, data, ['AUID'], [[x[0] for x in headlamb]])

def _dispatch(options):
  if options.get_au_status: _do_get_au_status(options)
  elif options.get_au_urls: _do_get_au_urls(options)
  elif options.get_au_article_urls: _do_get_au_article_urls(options)
  elif options.get_au_subst_urls: _do_get_au_subst_urls(options)
  elif options.get_auids or options.get_auids_names: _do_get_auids(options)
  elif options.get_peer_agreements: _do_get_peer_agreements(options)
  elif options.get_platform_configuration: _do_get_platform_configuration(options)
  elif options.is_daemon_ready or options.is_daemon_ready_quiet: _do_is_daemon_ready(options)
  elif options.query_aus: _do_query_aus(options)
  elif options.query_crawls: _do_query_crawls(options)
  else: raise RuntimeError('Unreachable')

def _make_client(host, auth):
    session = requests.Session()
    session.auth = auth
    transport = zeep.transports.Transport(session=session)
    wsdl = f'http://{host}/ws/DaemonStatusService?wsdl'
    client = zeep.Client(wsdl, transport=transport)
    return client

def _main():
  '''Main method.'''
  # Parse command line
  parser = _DaemonStatusServiceOptions.make_parser()
  args = parser.parse_args()
  options = _DaemonStatusServiceOptions(parser, args)
  # Dispatch
  t = Thread(target=_dispatch, args=(options,))
  t.daemon = True
  t.start()
  while True:
    t.join(1.5)
    if not t.is_alive(): break

if __name__ == '__main__': _main()

