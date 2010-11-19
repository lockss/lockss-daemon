#!/usr/bin/env python

# $Id: slurp.py,v 1.4 2010-11-19 09:45:01 thib_gc Exp $

# Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
# all rights reserved.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
# STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
# WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
# IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# Except as contained in this notice, the name of Stanford University shall not
# be used in advertising or otherwise to promote the sale, use or other dealings
# in this Software without prior written authorization from Stanford University.

from datetime import datetime
import MySQLdb
from optparse import OptionParser, Values
import os
from slurpdb import Slurpdb
import sys
from urllib2 import URLError

sys.path.append(os.path.realpath(os.path.join(os.path.dirname(sys.argv[0]), '../../test/frameworks/lib')))
import lockss_daemon
from lockss_util import LockssError

class SlurpConstants:

    VERSION = '0.3.0'    
    
    DESCRIPTION = '''Connects to a LOCKSS Web user interface, scrapes
information about the LOCKSS daemon from it, and stores the data in a
MySQL database. The database must have been properly initialized to
receive the data, for instance using Slurpdb. To run this program,
Python needs to have access to the MySQLdb module, which may not be 
installed by default. In many popular package repositories, it is
known as "python-mysqldb".'''
    
    OPTION_DBCONNECT = 'dbconnect'
    OPTION_DBCONNECT_SHORT = 'C'
    OPTION_DBCONNECT_DEFAULT_HOST = 'localhost'
    OPTION_DBCONNECT_DEFAULT_PORT = 3306
    OPTION_DBCONNECT_DEFAULT = '%s:%d' % (OPTION_DBCONNECT_DEFAULT_HOST, OPTION_DBCONNECT_DEFAULT_PORT)
    OPTION_DBCONNECT_HELP = 'Host name and port of the database server. If no port is specified, %d is used. Default: %%default' % (OPTION_DBCONNECT_DEFAULT_PORT,)
    
    OPTION_DBNAME = 'dbname'
    OPTION_DBNAME_SHORT = 'D'
    OPTION_DBNAME_DEFAULT = 'slurp'
    OPTION_DBNAME_HELP = 'Name of the database. Default: %default'
    
    OPTION_DBPASSWORD = 'dbpassword'
    OPTION_DBPASSWORD_SHORT = 'P'
    OPTION_DBPASSWORD_HELP = 'Password for the database.'
    
    OPTION_DBUSER = 'dbuser'
    OPTION_DBUSER_SHORT = 'U'
    OPTION_DBUSER_DEFAULT = 'slurp'
    OPTION_DBUSER_HELP = 'User name for the database. Default: %default'
    
    OPTION_LOGFILE = 'logfile'
    OPTION_LOGFILE_SHORT = 'l'
    OPTION_LOGFILE_HELP = 'Append logging information to a file.'
    
    OPTION_UICONNECT = 'uiconnect'
    OPTION_UICONNECT_SHORT = 'c'
    OPTION_UICONNECT_DEFAULT_HOST = 'localhost'
    OPTION_UICONNECT_DEFAULT_PORT = 8081
    OPTION_UICONNECT_DEFAULT = '%s:%d' % (OPTION_UICONNECT_DEFAULT_HOST, OPTION_UICONNECT_DEFAULT_PORT)
    OPTION_UICONNECT_HELP = 'Host name and port of a LOCKSS Web user interface. If no port is specified, %d is used. Default: %%default' % (OPTION_UICONNECT_DEFAULT_PORT,)
    
    OPTION_UIPASSWORD = 'uipassword'
    OPTION_UIPASSWORD_SHORT = 'p'
    OPTION_UIPASSWORD_DEFAULT = 'lockss-p'
    OPTION_UIPASSWORD_HELP = 'Password for the LOCKSS Web user interface. Default: %default'

    OPTION_UIRETRIES = 'uiretries'
    OPTION_UIRETRIES_SHORT = 'r'
    OPTION_UIRETRIES_DEFAULT = 3
    OPTION_UIRETRIES_HELP = 'Number of retries for failed UI requests. Default: %default'
    
    OPTION_UITIMEOUT = 'uitimeout'
    OPTION_UITIMEOUT_SHORT = 't'
    OPTION_UITIMEOUT_DEFAULT = 30
    OPTION_UITIMEOUT_HELP = 'Timeout (seconds) before giving up on UI. Default: %default'
    
    OPTION_UIUSER = 'uiuser'
    OPTION_UIUSER_SHORT = 'u'
    OPTION_UIUSER_DEFAULT = 'lockss-u'
    OPTION_UIUSER_HELP = 'User name for the LOCKSS Web user interface. Default: %default'
    
    OPTION_VERBOSE = 'verbose'
    OPTION_VERBOSE_SHORT = 'v'
    OPTION_VERBOSE_HELP = 'Output logging information to standard error'

    REQUIRED_OPTIONS = [OPTION_DBCONNECT,
                        OPTION_DBNAME,
                        OPTION_DBPASSWORD,
                        OPTION_DBUSER,
                        OPTION_UICONNECT,
                        OPTION_UIPASSWORD,
                        OPTION_UIUSER]
    
    # A strftime/strptime specification describing the format of timestamps in the UI
    UI_STRFTIME = '%H:%M:%S %m/%d/%y'

def __option_parser():
    parser = OptionParser(description=SlurpConstants.DESCRIPTION,
                          version=SlurpConstants.VERSION)
    parser.add_option('-' + SlurpConstants.OPTION_LOGFILE_SHORT,
                      '--' + SlurpConstants.OPTION_LOGFILE,
                      help=SlurpConstants.OPTION_LOGFILE_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_VERBOSE_SHORT,
                      '--' + SlurpConstants.OPTION_VERBOSE,
                      action='store_true',
                      help=SlurpConstants.OPTION_VERBOSE_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_DBCONNECT_SHORT,
                      '--' + SlurpConstants.OPTION_DBCONNECT,
                      default=SlurpConstants.OPTION_DBCONNECT_DEFAULT,
                      help=SlurpConstants.OPTION_DBCONNECT_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_DBUSER_SHORT,
                      '--' + SlurpConstants.OPTION_DBUSER,
                      default=SlurpConstants.OPTION_DBUSER_DEFAULT,
                      help=SlurpConstants.OPTION_DBUSER_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_DBPASSWORD_SHORT,
                      '--' + SlurpConstants.OPTION_DBPASSWORD,
                      help=SlurpConstants.OPTION_DBPASSWORD_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_DBNAME_SHORT,
                      '--' + SlurpConstants.OPTION_DBNAME,
                      default=SlurpConstants.OPTION_DBNAME_DEFAULT,
                      help=SlurpConstants.OPTION_DBNAME_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_UICONNECT_SHORT,
                      '--' + SlurpConstants.OPTION_UICONNECT,
                      default=SlurpConstants.OPTION_UICONNECT_DEFAULT,
                      help=SlurpConstants.OPTION_UICONNECT_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_UIUSER_SHORT,
                      '--' + SlurpConstants.OPTION_UIUSER,
                      default=SlurpConstants.OPTION_UIUSER_DEFAULT,
                      help=SlurpConstants.OPTION_UIUSER_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_UIPASSWORD_SHORT,
                      '--' + SlurpConstants.OPTION_UIPASSWORD,
                      default=SlurpConstants.OPTION_UIPASSWORD_DEFAULT,
                      help=SlurpConstants.OPTION_UIPASSWORD_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_UITIMEOUT_SHORT,
                      '--' + SlurpConstants.OPTION_UITIMEOUT,
                      type='int',
                      default=SlurpConstants.OPTION_UITIMEOUT_DEFAULT,
                      help=SlurpConstants.OPTION_UITIMEOUT_HELP)
    parser.add_option('-' + SlurpConstants.OPTION_UIRETRIES_SHORT,
                      '--' + SlurpConstants.OPTION_UIRETRIES,
                      type='int',
                      default=SlurpConstants.OPTION_UIRETRIES_DEFAULT,
                      help=SlurpConstants.OPTION_UIRETRIES_HELP)
    return parser

def __reprocess_options(parser, opt):
    options = dict([(attr, getattr(opt, attr)) for attr in filter(lambda a: a not in dir(Values()), dir(opt))])
    for req in SlurpConstants.REQUIRED_OPTIONS:
        if req not in options or options[req] is None:
            parser.error('%s is required' % (parser.get_option('--' + req),))
    if ':' not in options[SlurpConstants.OPTION_DBCONNECT]: options[SlurpConstants.OPTION_DBCONNECT] = '%s:%d' % (options[SlurpConstants.OPTION_DBCONNECT], SlurpConstants.OPTION_DBCONNECT_DEFAULT_PORT)
    if ':' not in options[SlurpConstants.OPTION_UICONNECT]: options[SlurpConstants.OPTION_UICONNECT] = '%s:%d' % (options[SlurpConstants.OPTION_UICONNECT], SlurpConstants.OPTION_UICONNECT_DEFAULT_PORT)
    if options[SlurpConstants.OPTION_LOGFILE]: options[SlurpConstants.OPTION_LOGFILE] = open(options[SlurpConstants.OPTION_LOGFILE], 'a')
    return options

def __log(message, options):
    outlets = []
    if options[SlurpConstants.OPTION_LOGFILE]: outlets.append(options[SlurpConstants.OPTION_LOGFILE])
    if options[SlurpConstants.OPTION_VERBOSE]: outlets.append(sys.stderr)
    for outlet in outlets:
        outlet.write(datetime.now().strftime(Slurpdb.STRFTIME))
        outlet.write(': ')
        outlet.write(message)
        outlet.write('\n')

def __begin(db, options):
    __log('Creating Slurp session...', options)
    cursor = db.cursor()
    cursor.execute('''
            INSERT INTO ''' + Slurpdb.SESSIONS + '''
            (ui, begin)
            VALUES (%s, NOW())
    ''', (options[SlurpConstants.OPTION_UICONNECT]),)
    cursor.execute('''
            SELECT LAST_INSERT_ID()
    ''')
    id = cursor.fetchone()[0]
    __log('Slurp session: %d' % (id,), options)
    cursor.close()
    db.commit()
    __log('Done.', options)
    return id

def __end(db, id, options):
    __log('Ending Slurp session...', options)
    cursor = db.cursor()
    cursor.execute('''
            UPDATE ''' + Slurpdb.SESSIONS + '''
            SET end = NOW()
            WHERE id = %s
    ''', (id,))
    cursor.close()
    db.commit()
    __log('Done.', options)

def __auids(db, ui, id, options):
    __log('Collecting list of AUIDs...', options)
    ret = {}
    for auid in ui.getListOfAuids():
        cursor = db.cursor()
        cursor.execute('''
                INSERT INTO ''' + Slurpdb.AUIDS + '''
                (sid, auid)
                VALUES (%s, %s)
        ''', (id, auid))
        cursor.execute('''
                SELECT LAST_INSERT_ID()
        ''')
        ret[auid] = cursor.fetchone()[0]
        cursor.close()
        db.commit()
    __log('Done.', options)
    return ret
    
def __aus(db, ui, id, auids_aids, options):
    __log('Collecting AU data...', options)
    for auid, aid in auids_aids.iteritems():
        __log('Collecting AU data for %s' % (auid,), options)
        retries = 0
        while retries <= options[SlurpConstants.OPTION_UIRETRIES]:
            try:
                if retries > 0: __log('Warning: retry #%d' % (retries,), options)
                summary, table = ui._getStatusTable('ArchivalUnitTable', auid)
                break
            except URLError:
                retries = retries + 1
        else:
            __log('Error: failed to retrieve AU data for %s' % (auid,), options)
            continue # Go on to the next AUID

        def ui_to_db(uistr):
            if uistr is None or uistr.lower() == 'never': return None
            return datetime.strptime(uistr, SlurpConstants.UI_STRFTIME).strftime(Slurpdb.STRFTIME)
        
        def db_len(uistr, maxlen):
            if uistr is None: return None
            if len(uistr) > maxlen:
                __log('Warning: String is over %d characters long: %s' % (maxlen, uistr), options)
            return uistr[0:maxlen]

        name = db_len(summary.get('Volume', None), Slurpdb.AUS_NAME_MAX)
        publisher = db_len(summary.get('Publisher', None), Slurpdb.AUS_PUBLISHER_MAX)
        year_str = summary.get('Year', None)
        repository = db_len(summary.get('Repository', None), Slurpdb.AUS_REPOSITORY_MAX)
        creation_date = ui_to_db(summary.get('Created', None))
        status = db_len(summary.get('Status', None), Slurpdb.AUS_STATUS_MAX)
        available = summary.get('Available From Publisher', None)
        if available: available = (available.lower() == 'yes') 
        last_crawl = ui_to_db(summary.get('Last Crawl', None))
        last_crawl_result = db_len(summary.get('Last Crawl Result', None), Slurpdb.AUS_CRAWL_RESULT_MAX) 
        last_completed_crawl = ui_to_db(summary.get('Last Completed Crawl', None))
        last_poll = ui_to_db(summary.get('Last Poll', None))
        last_poll_result = db_len(summary.get('Last Poll Result', None), Slurpdb.AUS_POLL_RESULT_MAX)
        last_completed_poll = ui_to_db(summary.get('Last Completed Poll', None))
        content_size = summary.get('Content Size', None)
        if content_size and content_size.lower() == 'awaiting recalc': content_size = None 
        if content_size: content_size = int(content_size.replace(',', ''))
        disk_usage = summary.get('Disk Usage (MB)', None)
        if disk_usage and disk_usage.lower() == 'awaiting recalc': disk_usage = None 
        if disk_usage: disk_usage = float(disk_usage)
        
        cursor = db.cursor()
        cursor.execute('''
                INSERT INTO ''' + Slurpdb.AUS + '''
                (aid, name, publisher, year, repository, creation_date, status, available,
                last_crawl, last_crawl_result, last_completed_crawl,
                last_poll, last_poll_result, last_completed_poll,
                content_size, disk_usage)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ''', (aid, name, publisher, year_str, repository, creation_date, status, available,
              last_crawl, last_crawl_result, last_completed_crawl,
              last_poll, last_poll_result, last_completed_poll,
              content_size, disk_usage))
        cursor.close()
        db.commit()
        
    __log('Done.', options)
    
def __articles(db, ui, id, auids_aids, options):
    __log('Collecting article data...', options)
    for auid, aid in auids_aids.iteritems():
        __log('Collecting article data for %s' % (auid,), options)
        retries = 0
        while retries <= options[SlurpConstants.OPTION_UIRETRIES]:
            try:
                if retries > 0: __log('Warning: retry #%d' % (retries,), options)
                articles = ui.getListOfArticles(lockss_daemon.AU(auid))
                break
            except (URLError, LockssError):
                retries = retries + 1
        else:
            __log('Error: failed to retrieve AU data for %s' % (auid,), options)
            continue # Go on to the next AUID

        cursor = db.cursor()
        cursor.executemany('''
                INSERT INTO ''' + Slurpdb.ARTICLES + '''
                (aid, article)
                VALUES (%s, %s)
        ''', [(aid, art) for art in articles])
        cursor.close()
        db.commit()

    __log('Done.', options)
    
def __process(options):
    __log('Opening database connection to %s on %s as %s...' % (options[SlurpConstants.OPTION_DBNAME], options[SlurpConstants.OPTION_DBCONNECT], options[SlurpConstants.OPTION_DBUSER]), options)
    db = MySQLdb.connect(host=options[SlurpConstants.OPTION_DBCONNECT].split(':')[0],
                         port=int(options[SlurpConstants.OPTION_DBCONNECT].split(':')[1]),
                         user=options[SlurpConstants.OPTION_DBUSER],
                         passwd=options[SlurpConstants.OPTION_DBPASSWORD],
                         db=options[SlurpConstants.OPTION_DBNAME])
    try:
        __log('Done.', options)
        __log('Opening connection to UI on %s as %s...' % (options[SlurpConstants.OPTION_UICONNECT], options[SlurpConstants.OPTION_UIUSER]), options)
        ui = lockss_daemon.Client(options[SlurpConstants.OPTION_UICONNECT].split(':')[0],
                                  options[SlurpConstants.OPTION_UICONNECT].split(':')[1],
                                  options[SlurpConstants.OPTION_UIUSER],
                                  options[SlurpConstants.OPTION_UIPASSWORD])
        if not ui.waitForDaemonReady(options[SlurpConstants.OPTION_UITIMEOUT]):
            __log('Error: %s is not ready after %d seconds' % (options[SlurpConstants.OPTION_UICONNECT], options[SlurpConstants.OPTION_UITIMEOUT]), options)
            raise RuntimeError, '%s is not ready after %d seconds' % (options[SlurpConstants.OPTION_UICONNECT], options[SlurpConstants.OPTION_UITIMEOUT])
        __log('Done.', options)
        id = __begin(db, options)
        auids_aids = __auids(db, ui, id, options)
        __aus(db, ui, id, auids_aids, options)
        __articles(db, ui, id, auids_aids, options)
        __end(db, id, options)
    finally:
        db.close()

if __name__ == '__main__':
    parser = __option_parser()
    (opt, args) = parser.parse_args(values=parser.get_default_values())
    options = __reprocess_options(parser, opt)
    try:
        __log('Beginning', options)
        __process(options)
    except:
        __log('Error, exiting', options)
        raise
    else:
        __log('Success, exiting', options)
        