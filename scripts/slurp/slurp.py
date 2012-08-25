#!/usr/bin/env python

# $Id: slurp.py,v 1.7 2012-08-25 00:38:51 thib_gc Exp $

__copyright__ = '''\
Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

__version__ = '0.4.1'

from datetime import datetime
import optparse
import os
import slurpdb
import sys
from urllib2 import URLError

sys.path.append(os.path.realpath(os.path.join(os.path.dirname(sys.argv[0]), '../../test/frameworks/lib')))
import lockss_daemon
from lockss_util import LockssError

UI_STRFTIME = '%H:%M:%S %m/%d/%y'

def slurp_auids(db, ui, sid):
    for auid in ui.getListOfAuids():
        db.make_auid(sid, auid)
    db.or_session_flags(sid, slurpdb.SESSIONS_FLAGS_AUIDS)

def slurp_aus(db, ui, sid, options):
    for aid, auid in db.get_auids_for_session(sid):
        retries = 0
        while retries <= options.daemon_ui_retries:
            try:
                summary, table = ui._getStatusTable('ArchivalUnitTable', auid)
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

        db.make_au(aid, name, publisher, year_str,
            repository, creation_date, status, available,
            last_crawl, last_crawl_result, last_completed_crawl, last_poll,
            last_poll_result, last_completed_poll, content_size, disk_usage)

    db.or_session_flags(sid, slurpdb.SESSIONS_FLAGS_AUS)

def daemon_ui_connection(options):
    ui_host, ui_port_str = options.daemon_ui_host_port.split(':')
    return lockss_daemon.Client(ui_host,
                                int(ui_port_str),
                                options.daemon_ui_user,
                                options.daemon_ui_pass)

def ui_to_datetime(ui_str):
    if ui_str is None or ui_str.lower() == 'never': return None
    return datetime.strptime(ui_str, UI_STRFTIME)

def slurp_option_parser():
    parser = optparse.OptionParser(version=__version__,
                                   description='Queries a LOCKSS daemon UI and stores results in a Slurp database.')
    slurpdb.slurpdb_option_parser(parser)
    parser.add_option('-D', '--daemon-ui-host-port',
                      metavar='HOSTPORT',
                      help='Daemon UI host and port.')
    parser.add_option('-U', '--daemon-ui-user',
                      metavar='USER',
                      help='Daemon UI user name.')
    parser.add_option('-P', '--daemon-ui-pass',
                      metavar='PASS',
                      help='Daemon UI password.')
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
    parser.add_option('--auids',
                      action='store_true',                     
                      help='Fills the "auids" table with the AUIDs of active AUs.')
    parser.add_option('--aus',
                      action='store_true',                     
                      help='Fills the "aus" table with data from active AUs.')
    return parser

def slurp_process_options(parser, options):
    if options.daemon_ui_host_port is None: parser.error('-D/--daemon-ui-host-port is required')
    if ':' not in options.daemon_ui_host_port: parser.error('-D/--daemon-ui-host-port does not specify a port')
    if options.daemon_ui_user is None: parser.error('-U/--daemon-ui-user is required')
    if options.daemon_ui_pass is None: parser.error('-P/--daemon-ui-pass is required')
    if options.auids is None: parser.error('No action specified')

if __name__ == '__main__':
    parser = slurp_option_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    slurp_process_options(parser, options)

    # Open a database connection
    db = slurpdb.slurpdb_connection(options)
    db.open_connection()
    sid = db.make_session(options.daemon_ui_host_port)
    
    # Open a daemon UI connection
    ui = daemon_ui_connection(options)
    if not ui.waitForDaemonReady(options.daemon_ui_timeout):
        raise RuntimeError, '%s is not ready after %d seconds' % (options.daemon_ui_host_port,
                                                                  options.daemon_ui_timeout)

    if options.auids: slurp_auids(db, ui, sid)
    if options.aus: slurp_aus(db, ui, sid, options)

    db.end_session(sid)
    db.close_connection()

