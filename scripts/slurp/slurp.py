#!/usr/bin/env python

# $Id: slurp.py,v 1.6 2012-08-24 06:40:59 thib_gc Exp $

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

__version__ = '0.4.0'

import optparse
import os
import slurpdb
import sys

sys.path.append(os.path.realpath(os.path.join(os.path.dirname(sys.argv[0]), '../../test/frameworks/lib')))
import lockss_daemon
from lockss_util import LockssError

def slurp_auids(db, ui, sid):
    for auid in ui.getListOfAuids():
        db.make_auid(sid, auid)
    db.or_session_flags(sid, slurpdb.SESSIONS_FLAGS_AUIDS)

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
    parser.add_option('-a', '--auids',
                      action='store_true',                     
                      help='Fills the auids tables with the AUIDs of active AUs.')
    return parser

def slurp_process_options(parser, options):
    if options.daemon_ui_host_port is None: parser.error('-D/--daemon-ui-host-port is required')
    if ':' not in options.daemon_ui_host_port: parser.error('-D/--daemon-ui-host-port does not specify a port')
    if options.daemon_ui_user is None: parser.error('-U/--daemon-ui-user is required')
    if options.daemon_ui_pass is None: parser.error('-P/--daemon-ui-pass is required')
    if options.auids is None: parser.error('No action specified')

def daemon_ui_connection(options):
    ui_host, ui_port_str = options.daemon_ui_host_port.split(':')
    return lockss_daemon.Client(ui_host,
                                int(ui_port_str),
                                options.daemon_ui_user,
                                options.daemon_ui_pass)

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

    db.end_session(sid)
    db.close_connection()

