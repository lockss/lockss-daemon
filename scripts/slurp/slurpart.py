#!/usr/bin/env python

# $Id: slurpart.py,v 1.2 2010-10-08 22:07:53 thib_gc Exp $

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

import csv
from datetime import datetime
import MySQLdb
from optparse import OptionParser, Values
from slurpdb import Slurpdb
import sys

class SlurpartConstants:
    
    VERSION = '0.3.0'
    
    DESCRIPTION = '''Produces an article report from data collected by
Slurp and stored in a Slurp database, arranged by publisher and by
year. Displays progress information to standard error and produces
comma-separated output to standard output.'''

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
    
    OPTION_OUTPUT = 'output'
    OPTION_OUTPUT_SHORT = 'o'
    OPTION_OUTPUT_HELP = 'Output file prefix (in the current directory).'    
    
    OPTION_SINCE = 'since'
    OPTION_SINCE_SHORT = 's'
    OPTION_SINCE_DEFAULT = '1970-01-01'
    OPTION_SINCE_META = 'YYYY-MM-DD'
    OPTION_SINCE_HELP = 'Consider data collected on or after %s. Default: %%default' % (OPTION_SINCE_META,)
    
    REQUIRED_OPTIONS = [OPTION_DBCONNECT,
                        OPTION_DBNAME,
                        OPTION_DBPASSWORD,
                        OPTION_DBUSER,
                        OPTION_OUTPUT,
                        OPTION_SINCE]

def __option_parser():
    parser = OptionParser(description=SlurpartConstants.DESCRIPTION,
                          version=SlurpartConstants.VERSION)
    parser.add_option('-' + SlurpartConstants.OPTION_DBCONNECT_SHORT,
                      '--' + SlurpartConstants.OPTION_DBCONNECT,
                      default=SlurpartConstants.OPTION_DBCONNECT_DEFAULT,
                      help=SlurpartConstants.OPTION_DBCONNECT_HELP)
    parser.add_option('-' + SlurpartConstants.OPTION_DBUSER_SHORT,
                      '--' + SlurpartConstants.OPTION_DBUSER,
                      default=SlurpartConstants.OPTION_DBUSER_DEFAULT,
                      help=SlurpartConstants.OPTION_DBUSER_HELP)
    parser.add_option('-' + SlurpartConstants.OPTION_DBPASSWORD_SHORT,
                      '--' + SlurpartConstants.OPTION_DBPASSWORD,
                      help=SlurpartConstants.OPTION_DBPASSWORD_HELP)
    parser.add_option('-' + SlurpartConstants.OPTION_DBNAME_SHORT,
                      '--' + SlurpartConstants.OPTION_DBNAME,
                      default=SlurpartConstants.OPTION_DBNAME_DEFAULT,
                      help=SlurpartConstants.OPTION_DBNAME_HELP)
    parser.add_option('-' + SlurpartConstants.OPTION_SINCE_SHORT,
                      '--' + SlurpartConstants.OPTION_SINCE,
                      default=SlurpartConstants.OPTION_SINCE_DEFAULT,
                      metavar=SlurpartConstants.OPTION_SINCE_META,
                      help=SlurpartConstants.OPTION_SINCE_HELP)
    parser.add_option('-' + SlurpartConstants.OPTION_OUTPUT_SHORT,
                      '--' + SlurpartConstants.OPTION_OUTPUT,
                      help=SlurpartConstants.OPTION_OUTPUT_HELP)
    return parser

def __output_summary(results, min_year, max_year, options):
    sys.stderr.write('Writing summary file...\n')
    f = open(options[SlurpartConstants.OPTION_OUTPUT] + '.summary.csv', 'wb')
    writer = csv.writer(f)
    writer.writerow(['Publisher', 'Ingested in %d' % (max_year,), 'Total ingested'])
    pubs = results.keys()
    pubs.sort()
    for p in pubs: writer.writerow([p, results[p][max_year], sum(results[p].values())])
    writer.writerow(['Total', sum([results[p][max_year] for p in pubs]), sum([sum(results[p].values()) for p in pubs])])
    f.close()
    sys.stderr.write('Done.\n')

def __output_details(results, min_year, max_year, options):
    sys.stderr.write('Writing details file...\n')
    f = open(options[SlurpartConstants.OPTION_OUTPUT] + '.details.csv', 'wb')
    writer = csv.writer(f)
    pubs = results.keys()
    pubs.sort()
    writer.writerow(['Year'] + pubs)
    for y in range(max_year, min_year - 1, -1): writer.writerow([y] + [results[p][y] for p in pubs])
    writer.writerow(['unknown'] + [results[p][0] for p in pubs])
    writer.writerow(['Total'] + [sum(results[p].values()) for p in pubs])
    f.close()
    sys.stderr.write('Done.\n')

def __process_results(query_results, options):
    def year_to_int(str):
        if str is None or str == '0': return 0
        if '-' in str: return int(str[5:9])
        return int(str)
    sys.stderr.write('Processing results...\n')
    min_year = min(map(lambda t: year_to_int(t[1] or '9999'), query_results))
    max_year = datetime.today().year
    ret = {}
    for p, y, a in query_results:
        if p is None: continue
        if p not in ret:
            ret[p] = dict(zip(range(min_year, max_year + 1), [0] * (max_year - min_year + 1)))
            ret[p][0] = 0
        y = year_to_int(y)
        ret[p][y] = ret[p][y] + a
    sys.stderr.write('Done.\n')
    __output_summary(ret, min_year, max_year, options)
    __output_details(ret, min_year, max_year, options)

def __process(options):
    sys.stderr.write('Opening database connection to %s on %s as %s...\n' % (options[SlurpartConstants.OPTION_DBNAME], options[SlurpartConstants.OPTION_DBCONNECT], options[SlurpartConstants.OPTION_DBUSER]))
    db = MySQLdb.connect(host=options[SlurpartConstants.OPTION_DBCONNECT].split(':')[0],
                         port=int(options[SlurpartConstants.OPTION_DBCONNECT].split(':')[1]),
                         user=options[SlurpartConstants.OPTION_DBUSER],
                         passwd=options[SlurpartConstants.OPTION_DBPASSWORD],
                         db=options[SlurpartConstants.OPTION_DBNAME])
    try:
        sys.stderr.write('Done.\n')
        sys.stderr.write('Finding Slurp sessions on or after %s...\n' % (options[SlurpartConstants.OPTION_SINCE],))
        cursor = db.cursor()
        cursor.execute('''
                SELECT id
                FROM ''' + Slurpdb.SESSIONS + '''
                WHERE end IS NOT NULL AND begin >= %s
        ''', (options[SlurpartConstants.OPTION_SINCE],))
        ids = [str(tup[0]) for tup in cursor.fetchall()]
        if len(ids) == 0:
            sys.stderr.write('No suitable Slurp sessions found.\n')
            raise RuntimeError, 'No suitable Slurp sessions found'
        if len(ids) == 1: ids_str = '= ids[0]'
        else: ids_str = 'IN (%s)' % (', '.join(ids),)
        sys.stderr.write('Done.\n')
        sys.stderr.write('Executing per publisher per year query...\n')
        cursor.execute('''
                SELECT Aus.publisher, Aus.year, count(distinct Articles.article)
                FROM ''' + Slurpdb.SESSIONS + ''' AS Sessions, ''' + Slurpdb.AUIDS + ''' AS Auids, ''' + Slurpdb.AUS + ''' AS Aus, ''' + Slurpdb.ARTICLES + ''' AS Articles
                WHERE Sessions.id ''' + ids_str + ''' AND Sessions.id = Auids.sid AND Aus.aid = Auids.id AND Articles.aid = Auids.id
                GROUP BY Aus.publisher, Aus.year
        ''')
        query_results = cursor.fetchall()
        cursor.close()
        sys.stderr.write('Done.\n')
        __process_results(query_results, options)
    finally:
        db.close()

def __reprocess_options(parser, opt):
    options = dict([(attr, getattr(opt, attr)) for attr in filter(lambda a: a not in dir(Values()), dir(opt))])
    for req in SlurpartConstants.REQUIRED_OPTIONS:
        if req not in options or options[req] is None:
            parser.error('%s is required' % (parser.get_option('--' + req),))
    if ':' not in options[SlurpartConstants.OPTION_DBCONNECT]: options[SlurpartConstants.OPTION_DBCONNECT] = '%s:%d' % (options[SlurpartConstants.OPTION_DBCONNECT], SlurpartConstants.OPTION_DBCONNECT_DEFAULT_PORT)
    return options

if __name__ == '__main__':
    parser = __option_parser()
    (opt, args) = parser.parse_args(values=parser.get_default_values())
    options = __reprocess_options(parser, opt)
    try:
        sys.stderr.write('Beginning\n')
        __process(options)
    except:
        sys.stderr.write('Error, exiting\n')
        raise
    else:
        sys.stderr.write('Success, exiting\n')
