#!/usr/bin/env python

# $Id$

__copyright__ = '''\
Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

__version__ = '0.5.4'

from datetime import datetime
import optparse
import os
import re
import slurpdb
import sys
import threading
from urllib2 import URLError

sys.path.append(os.path.realpath(os.path.join(os.path.dirname(sys.argv[0]), '../../test/frameworks/lib')))
import lockss_daemon
from lockss_util import LockssError

UI_STRFTIME = '%H:%M:%S %m/%d/%y'

def ui_to_datetime(ui_str):
    if ui_str is None or ui_str.lower() == 'never': return None
    return datetime.strptime(ui_str, UI_STRFTIME)

def slurp_option_parser():
    parser = optparse.OptionParser(version=__version__,
                                   description='Queries a LOCKSS daemon UI and stores results in a Slurp database',
                                   usage='Usage: %prog [options] host1:port1 host2:port2...')
    slurpdb.slurpdb_option_parser(parser)
    parser.add_option('-U', '--daemon-ui-user',
                      metavar='USER',
                      help='Daemon UI user name')
    parser.add_option('-P', '--daemon-ui-pass',
                      metavar='PASS',
                      help='Daemon UI password')
    parser.add_option('-R', '--daemon-ui-retries',
                      metavar='RETR',
                      type='int',
                      default=5,
                      help='Retries daemon UI requests up to RETR times. Default: %default')
    parser.add_option('-T', '--daemon-ui-timeout',
                      metavar='SECS',
                      type='int',
                      default=60,
                      help='Daemon UI requests time out after SECS seconds. Default: %default')
    parser.add_option('-a', '--auids',
                      action='store_true',                     
                      help='Gathers the active AUIDs')
    parser.add_option('--aus',
                      action='store_true',                     
                      help='Gathers data about the active AUs. Implies -a/--auids')
    parser.add_option('--articles',
                      action='store_true',                     
                      help='Gathers the articles for the active AUs. Implies -a/--auids')
    parser.add_option('-c', '--commdata',
                      action='store_true',                     
                      help='Gathers data about peer communication')
    parser.add_option('-g', '--agreement',
                      action='store_true',                     
                      help='Gathers data about peer agreement for the active AUs. Implies -a/--auids')
    parser.add_option('-l', '--auid-list',
                      metavar='FILE',                     
                      help='Only processes AUIDs read from FILE')
    parser.add_option('-r', '--auid-regex',
                      metavar='REGEX',                     
                      help='Only processes AUIDs that match REGEX')
    return parser

class SlurpThread(threading.Thread):

    def __init__(self, options, daemon_ui_host_port):
        threading.Thread.__init__(self)
        self.__options = options
        self.__daemon_ui_host_port = daemon_ui_host_port

    def run(self):
        self.__make_db_connection()
        self.__make_ui_connection()
        self.__dispatch()
        if not self.__options.db_ignore:
            self.__db.end_session(self.__sid)
            self.__db.close_connection()

    def __make_db_connection(self):
        if self.__options.db_ignore: return
        self.__db = slurpdb.SlurpDb()
        db_host, db_port_str = self.__options.db_host_port.split(':')
        self.__db.set_db_host(db_host)
        self.__db.set_db_port(int(db_port_str))
        self.__db.set_db_user(self.__options.db_user)
        self.__db.set_db_pass(self.__options.db_pass)
        self.__db.set_db_name(self.__options.db_name)
        self.__db.open_connection()
        self.__sid = self.__db.make_session(self.__daemon_ui_host_port)

    def __make_ui_connection(self):
        opt = self.__options
        daemon_ui_host, daemon_ui_port_str = self.__daemon_ui_host_port.split(':')
        self.__ui = lockss_daemon.Client(daemon_ui_host,
                                         int(daemon_ui_port_str),
                                         opt.daemon_ui_user,
                                         opt.daemon_ui_pass)
        if not self.__ui.waitForDaemonReady(self.__options.daemon_ui_timeout):
            raise RuntimeError, '%s is not ready after %d seconds' % (self.__daemon_ui_host_port,
                                                                      self.__options.daemon_ui_timeout)

    def __dispatch(self):
        if self.__options.auids: self.__slurp_auids()
        if self.__options.aus: self.__slurp_aus()
        if self.__options.agreement: self.__slurp_agreement()
        if self.__options.articles: self.__slurp_articles()
        if self.__options.commdata: self.__slurp_commdata()

    def __slurp_auids(self):
        flag = slurpdb.SESSIONS_FLAGS_AUIDS
        list_of_auids = self.__ui.getListOfAuids()
        # Maybe narrow down to a list
        fstr = options.auid_list
        if fstr is not None:
            f = open(fstr)
            external_auids = set()
            line = f.readline()
            while line != '':
                if line[-1] == '\n': line = line[0:-1]
                external_auids.add(line)
                line = f.readline()
            list_of_auids = filter(lambda a: a in external_auids, list_of_auids)
            flag = flag | slurpdb.SESSIONS_FLAGS_AUIDS_LIST
        # Maybe narrow down to a regex
        rstr = options.auid_regex
        if rstr is not None:
            r = re.compile(rstr)
            list_of_auids = filter(lambda a: r.search(a), list_of_auids)
            flag = flag | slurpdb.SESSIONS_FLAGS_AUIDS_REGEX
        self.__db.make_many_auids(self.__sid, list_of_auids)
        self.__db.or_session_flags(self.__sid, flag)

    def __slurp_aus(self):
        for aid, auid in self.__db.get_auids_for_session(self.__sid):
            retries = 0
            while retries <= self.__options.daemon_ui_retries:
                try:
                    summary, table = self.__ui._getStatusTable('ArchivalUnitTable', auid)
                    break
                except URLError:
                    retries = retries + 1
            else:
                continue # Go on to the next AUID ###FIXME

            name = summary.get('Volume', None)
            publisher = summary.get('Publisher', None)
            year_str = summary.get('Year', None)
            repository = summary.get('Repository', None)
            creation_date = ui_to_datetime(summary.get('Created', None))
            status = summary.get('Status', None)
            available = summary.get('Available From Publisher', None)
            if available: available = (available.lower() == 'yes') 
            last_crawl = ui_to_datetime(summary.get('Last Crawl', None))
            last_crawl_result = summary.get('Last Crawl Result', None)
            last_completed_crawl = ui_to_datetime(summary.get('Last Completed Crawl', None))
            last_poll = ui_to_datetime(summary.get('Last Poll', None))
            last_poll_result = summary.get('Last Poll Result', None)
            last_completed_poll = ui_to_datetime(summary.get('Last Completed Poll', None))
            content_size = summary.get('Content Size', None)
            if content_size and content_size.lower() == 'awaiting recalc': content_size = None 
            if content_size: content_size = int(content_size.replace(',', ''))
            disk_usage = summary.get('Disk Usage (MB)', None)
            if disk_usage and disk_usage.lower() == 'awaiting recalc': disk_usage = None 
            if disk_usage: disk_usage = float(disk_usage)
            title = summary.get('Journal Title', None)

            self.__db.make_au(aid, name, publisher, year_str,
                repository, creation_date, status, available,
                last_crawl, last_crawl_result, last_completed_crawl, last_poll,
                last_poll_result, last_completed_poll, content_size, disk_usage,
                title)

        self.__db.or_session_flags(self.__sid, slurpdb.SESSIONS_FLAGS_AUS)

    def __slurp_agreement(self):
        for aid, auid in self.__db.get_auids_for_session(self.__sid):
            retries = 0
            while retries <= self.__options.daemon_ui_retries:
                try:
                    agreement_table = self.__ui.getAllAuRepairerInfo(auid)
                    break
                except URLError:
                    retries = retries + 1
            else:
                continue # Go on to the next AUID ###FIXME

            for peer, vals in agreement_table.iteritems():
                self.__db.make_agreement(aid, peer, vals['HighestPercentAgreement'],
                       vals['LastPercentAgreement'], vals['HighestPercentAgreementHint'],
                       vals['LastPercentAgreementHint'], vals['Last'],
                       ui_to_datetime(vals['LastAgree']))
        self.__db.or_session_flags(self.__sid, slurpdb.SESSIONS_FLAGS_AGREEMENT)

    def __slurp_articles(self):
        for aid, auid in self.__db.get_auids_for_session(self.__sid):
            retries = 0
            while retries <= self.__options.daemon_ui_retries:
                try:
                    lst = self.__ui.getListOfArticles(lockss_daemon.AU(auid))
                    break
                except URLError:
                    retries = retries + 1
            else:
                continue # Go on to the next AUID ###FIXME

            self.__db.make_many_articles(aid, lst)

        self.__db.or_session_flags(self.__sid, slurpdb.SESSIONS_FLAGS_ARTICLES)


    def __slurp_commdata(self):
        retries = 0
        while retries <= self.__options.daemon_ui_retries:
            try:
                table = self.__ui.getCommPeerData()
                break
            except URLError:
                retries = retries + 1
        else:
            raise RuntimeError, 'Could not retrieve comm peer data from %s' % (self.__options.daemon_ui_host_port,)
        lot = [(p, v['Orig'], v['Fail'], v['Accept'], v['Sent'],
                v['Rcvd'], v['Chan'], v['SendQ'], v['LastRetry'],
                v['NextRetry']) for p, v in table.iteritems()]
        lot = [(p, v['Orig'], v['Fail'], v['Accept'], v['Sent'],
                v['Rcvd'], v['Chan'], v['SendQ'],
                ui_to_datetime(v['LastRetry']),
                ui_to_datetime(v['NextRetry'])) \
                        for p, v in table.iteritems()]
        if self.__options.db_ignore:
            for tup in lot: print '\t'.join([str(x) for x in tup])
        else:
            self.__db.make_many_commdata(self.__sid, lot)
            self.__db.or_session_flags(self.__sid, slurpdb.SESSIONS_FLAGS_COMMDATA)

def slurp_validate_options(parser, options):
    slurpdb.slurpdb_validate_options(parser, options)
    if options.daemon_ui_user is None: parser.error('-U/--daemon-ui-user is required')
    if options.daemon_ui_pass is None: parser.error('-P/--daemon-ui-pass is required')
    if options.aus is not None: setattr(parser.values, parser.get_option('--auids').dest, True)
    if options.agreement is not None: setattr(parser.values, parser.get_option('--auids').dest, True)
    if options.articles is not None: setattr(parser.values, parser.get_option('--auids').dest, True)
    if options.auid_regex is not None:
        try: r = re.compile(options.auid_regex)
        except: parser.error('-r/--auid-regex regular expression is invalid: %s' % (options.auid_regex,))
    if options.auid_list is not None:
        try:
            f = open(options.auid_list)
            f.close()
        except: parser.error('-l/--auid-list file cannot be opened: %s' % (options.auid_list,))
    if options.auids is None and options.commdata is None: parser.error('No action specified')

def slurp_validate_args(parser, options, args):
    for daemon_ui_host_port in args:
        if ':' not in daemon_ui_host_port: parser.error('No port specified: %s' % (daemon_ui_host_port,))

if __name__ == '__main__':
    parser = slurp_option_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    slurp_validate_options(parser, options)
    slurp_validate_args(parser, options, args)
    for daemon_ui_host_port in args:
        SlurpThread(options, daemon_ui_host_port).start()

