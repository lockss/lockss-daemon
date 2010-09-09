#!/usr/bin/env python

# $Id: slurpdb.py,v 1.2 2010-09-09 11:33:04 thib_gc Exp $

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

import MySQLdb
from optparse import OptionParser, Values
import os
import sys

class Slurpdb:

    class Length:
        CONCISE = 127
        SHORT = 255
        BRIEF = 511
        FULL = 1027
        LONG = 2047
        EXTENSIVE = 4095 

    # Related to the articles table
    ARTICLES = 'articles'
    ARTICLES_ARTICLE_MAX = Length.BRIEF

    # Related to the AUIDs table
    AUIDS = 'auids'
    AUIDS_AUID_MAX = Length.BRIEF     
    
    # Related to the AUs table
    AUS = 'aus'
    AUS_NAME_MAX = Length.BRIEF
    AUS_PUBLISHER_MAX = Length.SHORT
    AUS_REPOSITORY_MAX = Length.SHORT
    AUS_STATUS_MAX = Length.CONCISE
    AUS_CRAWL_RESULT_MAX = Length.CONCISE
    AUS_POLL_RESULT_MAX = Length.CONCISE
    
    # Related to the sessions table
    SESSIONS = 'sessions'
    SESSIONS_UI_MAX = Length.SHORT

    # A strftime/strptime specification describing the format of values of type DATEIME or TIMESTAMP
    STRFTIME = '%Y-%m-%d %H:%M:%S'
    
class SlurpdbConstants:
    
    VERSION = '0.3.0'
    
    DESCRIPTION = '''Creates tables in a MySQL database suitable to
store data scraped from LOCKSS Web user interfaces by the slurp
program. The database must already exist on the database server and
the database user must have sufficient privileges. This program always
produces output to standard output.'''

    OPTION_DBCONNECT = 'dbconnect'
    OPTION_DBCONNECT_SHORT = 'C'
    OPTION_DBCONNECT_DEFAULT_HOST = 'localhost'
    OPTION_DBCONNECT_DEFAULT_PORT = 3306
    OPTION_DBCONNECT_DEFAULT = '%s:%d' % (OPTION_DBCONNECT_DEFAULT_HOST, OPTION_DBCONNECT_DEFAULT_PORT)
    OPTION_DBCONNECT_HELP = 'Host name and port of the database server. If no port is specified, %d is used. Default: %%default' % (OPTION_DBCONNECT_DEFAULT_PORT,)
    
    OPTION_DBNAME = 'dbname'
    OPTION_DBNAME_SHORT = 'D'
    OPTION_DBNAME_DEFAULT = 'slurp'
    OPTION_DBNAME_HELP = 'Name of the database being created. Default: %default'
    
    OPTION_DBPASSWORD = 'dbpassword'
    OPTION_DBPASSWORD_SHORT = 'P'
    OPTION_DBPASSWORD_HELP = 'Password for the database.'
    
    OPTION_DBUSER = 'dbuser'
    OPTION_DBUSER_SHORT = 'U'
    OPTION_DBUSER_DEFAULT = 'slurp'
    OPTION_DBUSER_HELP = 'User name for the database. Default: %default'
    
    REQUIRED_OPTIONS = [OPTION_DBCONNECT,
                        OPTION_DBNAME,
                        OPTION_DBPASSWORD,
                        OPTION_DBUSER]

def __option_parser():
    parser = OptionParser(description=SlurpdbConstants.DESCRIPTION,
                          version=SlurpdbConstants.VERSION)
    parser.add_option('-' + SlurpdbConstants.OPTION_DBCONNECT_SHORT,
                      '--' + SlurpdbConstants.OPTION_DBCONNECT,
                      default=SlurpdbConstants.OPTION_DBCONNECT_DEFAULT,
                      help=SlurpdbConstants.OPTION_DBCONNECT_HELP)
    parser.add_option('-' + SlurpdbConstants.OPTION_DBUSER_SHORT,
                      '--' + SlurpdbConstants.OPTION_DBUSER,
                      default=SlurpdbConstants.OPTION_DBUSER_DEFAULT,
                      help=SlurpdbConstants.OPTION_DBUSER_HELP)
    parser.add_option('-' + SlurpdbConstants.OPTION_DBPASSWORD_SHORT,
                      '--' + SlurpdbConstants.OPTION_DBPASSWORD,
                      help=SlurpdbConstants.OPTION_DBPASSWORD_HELP)
    parser.add_option('-' + SlurpdbConstants.OPTION_DBNAME_SHORT,
                      '--' + SlurpdbConstants.OPTION_DBNAME,
                      default=SlurpdbConstants.OPTION_DBNAME_DEFAULT,
                      help=SlurpdbConstants.OPTION_DBNAME_HELP)
    return parser

def __reprocess_options(parser, opt):
    options = dict([(attr, getattr(opt, attr)) for attr in filter(lambda a: a not in dir(Values()), dir(opt))])
    for req in SlurpdbConstants.REQUIRED_OPTIONS:
        if req not in options or options[req] is None:
            parser.error('%s is required' % (parser.get_option('--' + req),))
    if ':' not in options[SlurpdbConstants.OPTION_DBCONNECT]: options[SlurpdbConstants.OPTION_DBCONNECT] = '%s:%d' % (options[SlurpdbConstants.OPTION_DBCONNECT], SlurpdbConstants.OPTION_DBCONNECT_DEFAULT_PORT)
    return options

def __check_empty(db):
    print 'Checking that the database is empty...'
    cursor = db.cursor()
    if cursor.execute('''
            SHOW TABLES;     
    ''') != 0L:
        print 'Error: the database is not empty'
        raise RuntimeError, 'The database is not empty'
    print 'Done.'

def __make_table(db, table, vals):
    print 'Creating table %s...' % (table,)
    f = open(os.path.dirname(sys.argv[0]) + os.sep + table + '.sql', 'r')
    s = f.read()
    f.close()
    cursor = db.cursor()
    cursor.execute(s % vals)
    cursor.close()
    db.commit()
    print 'Done.'

def __process(options):
    print 'Opening database connection to %s on %s as %s...' % (options[SlurpdbConstants.OPTION_DBNAME], options[SlurpdbConstants.OPTION_DBCONNECT], options[SlurpdbConstants.OPTION_DBUSER])
    db = MySQLdb.connect(host=options[SlurpdbConstants.OPTION_DBCONNECT].split(':')[0],
                         port=int(options[SlurpdbConstants.OPTION_DBCONNECT].split(':')[1]),
                         user=options[SlurpdbConstants.OPTION_DBUSER],
                         passwd=options[SlurpdbConstants.OPTION_DBPASSWORD],
                         db=options[SlurpdbConstants.OPTION_DBNAME])
    print 'Done.'
    try:
        __check_empty(db)
        __make_table(db, Slurpdb.SESSIONS, (Slurpdb.SESSIONS,
                                            Slurpdb.SESSIONS_UI_MAX))
        __make_table(db, Slurpdb.AUIDS, (Slurpdb.AUIDS,
                                         Slurpdb.AUIDS_AUID_MAX,
                                         Slurpdb.SESSIONS))
        __make_table(db, Slurpdb.AUS, (Slurpdb.AUS,
                                       Slurpdb.AUS_NAME_MAX,
                                       Slurpdb.AUS_PUBLISHER_MAX,
                                       Slurpdb.AUS_REPOSITORY_MAX,
                                       Slurpdb.AUS_STATUS_MAX,
                                       Slurpdb.AUS_CRAWL_RESULT_MAX,
                                       Slurpdb.AUS_POLL_RESULT_MAX,
                                       Slurpdb.AUIDS))
        __make_table(db, Slurpdb.ARTICLES, (Slurpdb.ARTICLES,
                                            Slurpdb.ARTICLES_ARTICLE_MAX,
                                            Slurpdb.AUIDS))
    finally:
        db.close()

if __name__ == '__main__':
    parser = __option_parser()
    (opt, args) = parser.parse_args(values=parser.get_default_values())
    options = __reprocess_options(parser, opt)
    try:
        print 'Beginning'
        __process(options)
    except:
        print 'Error, exiting'
        raise
    else:
        print 'Success, exiting'
