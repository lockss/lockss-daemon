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
import sys

try:
    import MySQLdb
except ImportError:
    print >> sys.stderr, 'Warning: module MySQLdb not found, database operations will fail'

class SlurpDb(object):

    DB_STRFTIME = '%Y-%m-%d %H:%M:%S'

    def __init__(self):
        object.__init__(self)
        self.__db = None
        self.__db_host = None
        self.__db_port = None
        self.__db_user = None
        self.__db_pass = None
        self.__db_name = None

    def set_db_host(self, db_host):
        self.__raise_if_open()
        self.__db_host = db_host

    def set_db_port(self, db_port):
        self.__raise_if_open()
        self.__db_port = db_port

    def set_db_user(self, db_user):
        self.__raise_if_open()
        self.__db_user = db_user

    def set_db_pass(self, db_pass):
        self.__raise_if_open()
        self.__db_pass = db_pass

    def set_db_name(self, db_name):
        self.__raise_if_open()
        self.__db_name = db_name

    def is_open(self): return not self.is_closed()

    def is_closed(self): return self.__db is None

    def open_connection(self):
        self.__raise_if_open()
        if self.__db_host is None: raise RuntimeError('Host not set')
        if self.__db_port is None: raise RuntimeError('Port not set')
        if self.__db_user is None: raise RuntimeError('User name not set')
        if self.__db_pass is None: raise RuntimeError('Password not set')
        if self.__db_name is None: raise RuntimeError('Database name not set')
        self.__db = MySQLdb.connect(host=self.__db_host,
                                    port=self.__db_port,
                                    user=self.__db_user,
                                    passwd=self.__db_pass,
                                    db=self.__db_name)
        self.__initialize()

    def close_connection(self):
        self.__raise_if_closed()
        try: self.__db.close()
        finally: self.__db = None

    def safe_close_connection(self):
        try: self.close()
        finally: return

    def make_session(self, ui):
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.execute('''\
                INSERT INTO %s
                (%s, %s, %s)
                VALUES (%%s, NOW(), %%s)
        ''' % (SESSIONS,
               SESSIONS_UI, SESSIONS_BEGIN, SESSIONS_FLAGS),
        (ui, SESSIONS_FLAGS_NONE))
        sid = self.__last_insert_id(cursor)
        cursor.close()
        self.__commit()
        return sid

    def get_session_flags(self, sid):
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.execute('''\
                SELECT %s
                FROM %s
                WHERE %s = %%s
        ''' % (SESSIONS_FLAGS,
               SESSIONS,
               SESSIONS_ID),
        (sid,))
        flags = cursor.fetchone()[0]
        cursor.close()
        return flags

    def or_session_flags(self, sid, or_value):
        self.__raise_if_closed()
        flags = self.get_session_flags(sid)
        cursor = self.__cursor()
        cursor.execute('''\
                UPDATE %s
                SET %s = %%s
                WHERE %s = %%s
        ''' % (SESSIONS,
               SESSIONS_FLAGS,
               SESSIONS_ID),
        (flags|or_value,
         sid))
        cursor.close()
        self.__commit()
        return flags

    def end_session(self, sid):
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.execute('''\
                UPDATE %s
                SET %s = NOW()
                WHERE %s = %%s
        ''' % (SESSIONS,
               SESSIONS_END,
               SESSIONS_ID),
        (sid,))
        cursor.close()
        self.__commit()

    def make_auid(self, sid, auid):
        self.make_many_auids(sid, [auid])

    def make_many_auids(self, sid, list_of_auids):
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.executemany('''\
                INSERT INTO %s
                (%s, %s)
                VALUES (%%s, %%s)
        ''' % (AUIDS,
               AUIDS_SID, AUIDS_AUID),
        [(sid, self.__str_to_db(auid, AUIDS_AUID)) for auid in list_of_auids])
        cursor.close()
        self.__commit()

    def get_auids_for_session(self, sid):
        '''Returns (aid, auid) tuples.'''
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.execute('''\
                SELECT %s, %s
                FROM %s
                WHERE %s = %%s
        ''' % (AUIDS_ID, AUIDS_AUID,
               AUIDS,
               AUIDS_SID),
        (sid,))
        ret = cursor.fetchall()
        cursor.close()
        return ret

    def make_au(self,
                aid,
                name,
                publisher,
                year,
                repository,
                creation_date,
                status,
                available,
                last_crawl,
                last_crawl_result,
                last_completed_crawl,
                last_poll,
                last_poll_result,
                last_completed_poll,
                content_size,
                disk_usage,
                title):
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.execute('''\
                INSERT INTO %s
                (%s, %s,
                    %s, %s,
                    %s, %s,
                    %s, %s,
                    %s, %s,
                    %s, %s,
                    %s, %s,
                    %s, %s,
                    %s)
                VALUES (%%s, %%s,
                    %%s, %%s,
                    %%s, %%s,
                    %%s, %%s,
                    %%s, %%s,
                    %%s, %%s,
                    %%s, %%s,
                    %%s, %%s,
                    %%s)
        ''' % (AUS,
               AUS_AID, AUS_NAME,
               AUS_PUBLISHER, AUS_YEAR,
               AUS_REPOSITORY, AUS_CREATION_DATE,
               AUS_STATUS, AUS_AVAILABLE,
               AUS_LAST_CRAWL, AUS_LAST_CRAWL_RESULT,
               AUS_LAST_COMPLETED_CRAWL, AUS_LAST_POLL,
               AUS_LAST_POLL_RESULT, AUS_LAST_COMPLETED_POLL,
               AUS_CONTENT_SIZE, AUS_DISK_USAGE,
               AUS_TITLE),
        (aid, self.__str_to_db(name, AUS_NAME),
         self.__str_to_db(publisher, AUS_PUBLISHER), year,
         self.__str_to_db(repository, AUS_REPOSITORY), self.__datetime_to_db(creation_date),
         self.__str_to_db(status, AUS_STATUS), available,
         self.__datetime_to_db(last_crawl), self.__str_to_db(last_crawl_result, AUS_LAST_CRAWL_RESULT),
         self.__datetime_to_db(last_completed_crawl), self.__datetime_to_db(last_poll),
         self.__str_to_db(last_poll_result, AUS_LAST_POLL_RESULT), self.__datetime_to_db(last_completed_poll),
         content_size, disk_usage,
         title))
        cursor.close()
        self.__commit()
        
    def make_agreement(self,
                       aid,
                       peer,
                       highest_agreement,
                       last_agreement,
                       highest_agreement_hint,
                       last_agreement_hint,
                       consensus,
                       last_consensus):
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.execute('''\
                INSERT INTO %s
                (%s, %s,
                    %s, %s,
                    %s, %s,
                    %s, %s)
                VALUES (%%s, %%s,
                    %%s, %%s,
                    %%s, %%s,
                    %%s, %%s)
        ''' % (AGREEMENT,
               AGREEMENT_AID, AGREEMENT_PEER,
               AGREEMENT_HIGHEST_AGREEMENT, AGREEMENT_LAST_AGREEMENT,
               AGREEMENT_HIGHEST_AGREEMENT_HINT, AGREEMENT_LAST_AGREEMENT_HINT,
               AGREEMENT_CONSENSUS, AGREEMENT_LAST_CONSENSUS),
        (aid, self.__str_to_db(peer, AGREEMENT_PEER),
         highest_agreement, last_agreement,
         highest_agreement_hint, last_agreement_hint,
         bool(consensus.lower() == 'yes'), self.__datetime_to_db(last_consensus)))
        cursor.close()
        self.__commit()

    def make_commdata(self,
                      sid,
                      peer,
                      origin,
                      fail,
                      accept,
                      sent,
                      received,
                      channel,
                      send_queue,
                      last_attempt,
                      next_retry):
        self.make_many_commdata(sid, [(peer, origin, fail, accept, sent,
        received, channel, send_queue, last_attempt, next_retry)])

    def make_many_commdata(self, sid, list_of_commdata):
        '''Tuples in list_of_commdata are expected to have the same
        order as the arguments of make_commdata, namely: (peer, origin,
        fail, accept, sent, received, channel, send_queue, last_attempt,
        next_retry).'''
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.executemany('''\
                INSERT INTO %s
                (%s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s, %s, %s)
                VALUES (%%s, %%s, %%s, %%s,
                    %%s, %%s, %%s, %%s,
                    %%s, %%s, %%s)
        ''' % (COMMDATA,
               COMMDATA_SID, COMMDATA_PEER, COMMDATA_ORIGIN, COMMDATA_FAIL,
               COMMDATA_ACCEPT, COMMDATA_SENT, COMMDATA_RECEIVED, COMMDATA_CHANNEL,
               COMMDATA_SEND_QUEUE, COMMDATA_LAST_ATTEMPT, COMMDATA_NEXT_RETRY),
        [(sid, self.__str_to_db(cd[0], COMMDATA_PEER), cd[1], cd[2],
         cd[3], cd[4], cd[5], self.__str_to_db(cd[6], COMMDATA_CHANNEL),
         cd[7], self.__datetime_to_db(cd[8]), self.__datetime_to_db(cd[9])) \
                 for cd in list_of_commdata])
        cursor.close()
        self.__commit()

    def make_many_articles(self, aid, list_of_articles):
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.executemany('''\
                INSERT INTO %s
                (%s, %s)
                VALUES (%%s, %%s)
        ''' % (ARTICLES,
               ARTICLES_AID, ARTICLES_ARTICLE),
        [(aid, art) for art in list_of_articles])
        cursor.close()
        self.__commit()

    def __datetime_to_db(self, py_datetime):
        if py_datetime is None: return None
        return py_datetime.strftime(SlurpDb.DB_STRFTIME)

    def __str_to_db(self, py_str, table):
        if py_str is None: return None
        enc = table.encoding()
        if enc is not None: py_str = py_str.encode(enc)
        return py_str[0:table.length()]

    def __raise_if_open(self):
        if self.is_open(): raise RuntimeError, 'Connection already open'
        
    def __raise_if_closed(self):
        if self.is_closed(): raise RuntimeError, 'Connection closed'

    def __cursor(self):
        return self.__db.cursor()

    def __commit(self):
        return self.__db.commit()

    def __last_insert_id(self, cursor):
        cursor.execute('''\
                SELECT LAST_INSERT_ID()
        ''')
        return cursor.fetchone()[0]

    def __initialize(self):
        # Create the 'tables' support table if the database is empty
        cursor = self.__cursor()
        cursor.execute('''\
            SHOW TABLES
        ''')
        row = cursor.fetchone()
        if row is None:
            self.__create_table(TABLES)
        cursor.close()
        # Get the recorded version of tables
        tables_present = dict()
        cursor = self.__cursor()
        cursor.execute('''\
            SELECT %s, %s
            FROM %s
        ''' % (TABLES_NAME, TABLES_VERSION,
               TABLES))
        row = cursor.fetchone()
        while row is not None:
            tables_present[row[0]] = row[1]
            row = cursor.fetchone()
        # Create or update tables
        for tab in ALL_TABLES:
            ver = tables_present.get(tab.name())
            if ver is None: self.__create_table(tab)
            else: self.__update_table(tab, ver)

    def __create_table(self, table):
        f = open('%s%s%s.sql' % (os.path.dirname(sys.argv[0]),
                                 os.sep,
                                 table.name()),
                 'r')
        s = f.read()
        f.close()
        cursor = self.__cursor()
        cursor.execute(s)
        cursor.execute('''\
                INSERT INTO %s
                (%s, %s)
                VALUES (%%s, %%s)
        ''' % (TABLES,
               TABLES_NAME, TABLES_VERSION),
        (table.name(), table.version()))
        cursor.close()
        self.__commit()

    def __update_table(self, table, current_version):
        while current_version < table.version():
            f = open('%s%s%s.%d.%d.sql' % (os.path.dirname(sys.argv[0]),
                                           os.sep,
                                           table.name(),
                                           current_version,
                                           current_version + 1),
                     'r')
            s = f.read()
            f.close()
            cursor = self.__cursor()
            cursor.execute(s)
            cursor.execute('''\
                    UPDATE %s
                    SET %s = %%s
                    WHERE %s = %%s
            ''' % (TABLES,
                   TABLES_VERSION,
                   TABLES_NAME),
            (current_version + 1,
             table.name()))
            cursor.close()
            self.__commit()
            current_version = current_version + 1

class Table(object):

    def __init__(self, name, version):
        object.__init__(self)
        self.__name = name
        self.__version = version
        self.__columns = list()

    def name(self): return self.__name

    def version(self): return self.__version

    def add_column(self, column): self.__columns.append(column)

    def columns(self): return self.__columns

    def __str__(self): return self.name()

class Column(object):

    def __init__(self, table, name, length=None, encoding=None):
        object.__init__(self)
        self.__table = table
        self.__name = name
        self.__length = length
        self.__encoding = encoding
        table.add_column(self)

    def table(self): return self.__table

    def name(self): return self.__name

    def length(self): return self.__length

    def encoding(self): return self.__encoding

    def __str__(self): return self.name()

TABLES = Table('tables', 1)
TABLES_ID = Column(TABLES, 'id')
TABLES_NAME = Column(TABLES, 'name', 63)
TABLES_VERSION = Column(TABLES, 'version')

SESSIONS = Table('sessions', 1)
SESSIONS_ID = Column(SESSIONS, 'id')
SESSIONS_UI = Column(SESSIONS, 'ui', 127)
SESSIONS_BEGIN = Column(SESSIONS, 'begin')
SESSIONS_END = Column(SESSIONS, 'end')
SESSIONS_FLAGS = Column(SESSIONS, 'flags')
SESSIONS_FLAGS_NONE = 0x0
SESSIONS_FLAGS_AUIDS = 0x1
SESSIONS_FLAGS_AUS = 0x2
SESSIONS_FLAGS_AGREEMENT = 0x4
SESSIONS_FLAGS_AUIDS_REGEX = 0x8
SESSIONS_FLAGS_COMMDATA = 0x10
SESSIONS_FLAGS_ARTICLES = 0x20
SESSIONS_FLAGS_AUIDS_LIST = 0x40

AUIDS = Table('auids', 1)
AUIDS_ID = Column(AUIDS, 'id')
AUIDS_SID = Column(AUIDS, 'sid')
AUIDS_AUID = Column(AUIDS, 'auid', 511)

AGREEMENT = Table('agreement', 1)
AGREEMENT_ID = Column(AGREEMENT, 'id')
AGREEMENT_AID = Column(AGREEMENT, 'aid')
AGREEMENT_PEER = Column(AGREEMENT, 'peer', 31)
AGREEMENT_HIGHEST_AGREEMENT = Column(AGREEMENT, 'highest_agreement')
AGREEMENT_LAST_AGREEMENT = Column(AGREEMENT, 'last_agreement')
AGREEMENT_HIGHEST_AGREEMENT_HINT = Column(AGREEMENT, 'highest_agreement_hint')
AGREEMENT_LAST_AGREEMENT_HINT = Column(AGREEMENT, 'last_agreement_hint')
AGREEMENT_CONSENSUS = Column(AGREEMENT, 'consensus')
AGREEMENT_LAST_CONSENSUS = Column(AGREEMENT, 'last_consensus')

ARTICLES = Table('articles', 1)
ARTICLES_ID = Column(ARTICLES, 'id')
ARTICLES_AID = Column(ARTICLES, 'aid')
ARTICLES_ARTICLE = Column(ARTICLES, 'article', 511)

AUS = Table('aus', 2)
AUS_ID = Column(AUS, 'id')
AUS_AID = Column(AUS, 'aid')
AUS_NAME = Column(AUS, 'name', 511, 'utf-8')
AUS_PUBLISHER = Column(AUS, 'publisher', 255, 'utf-8')
AUS_YEAR = Column(AUS, 'year', 9)
AUS_REPOSITORY = Column(AUS, 'repository', 255)
AUS_CREATION_DATE = Column(AUS, 'creation_date')
AUS_STATUS = Column(AUS, 'status', 127)
AUS_AVAILABLE = Column(AUS, 'available')
AUS_LAST_CRAWL = Column(AUS, 'last_crawl')
AUS_LAST_CRAWL_RESULT = Column(AUS, 'last_crawl_result', 127)
AUS_LAST_COMPLETED_CRAWL = Column(AUS, 'last_completed_crawl')
AUS_LAST_POLL = Column(AUS, 'last_poll')
AUS_LAST_POLL_RESULT = Column(AUS, 'last_poll_result', 127)
AUS_LAST_COMPLETED_POLL = Column(AUS, 'last_completed_poll')
AUS_CONTENT_SIZE = Column(AUS, 'content_size')
AUS_DISK_USAGE = Column(AUS, 'disk_usage')
AUS_TITLE = Column(AUS, 'title', 511, 'utf-8')

COMMDATA = Table('commdata', 1)
COMMDATA_ID = Column(COMMDATA, 'id')
COMMDATA_SID = Column(COMMDATA, 'sid')
COMMDATA_SID = Column(COMMDATA, 'sid')
COMMDATA_PEER = Column(COMMDATA, 'peer', 31)
COMMDATA_ORIGIN = Column(COMMDATA, 'origin')
COMMDATA_FAIL = Column(COMMDATA, 'fail')
COMMDATA_ACCEPT = Column(COMMDATA, 'accept')
COMMDATA_SENT = Column(COMMDATA, 'sent')
COMMDATA_RECEIVED = Column(COMMDATA, 'received')
COMMDATA_CHANNEL = Column(COMMDATA, 'channel', 15)
COMMDATA_SEND_QUEUE = Column(COMMDATA, 'send_queue')
COMMDATA_LAST_ATTEMPT = Column(COMMDATA, 'last_attempt')
COMMDATA_NEXT_RETRY = Column(COMMDATA, 'next_retry')

ALL_TABLES = [TABLES,
              SESSIONS,
              AUIDS,
              AGREEMENT,
              ARTICLES,
              AUS,
              COMMDATA]

def slurpdb_option_parser(parser=None):
    if parser is None:
        container = optparse.OptionParser(version=__version__,
                                          description='Initializes or updates a Slurp database',
                                          usage='Usage: %prog [options]')
    else:
        container = optparse.OptionGroup(parser,
                                         'slurpdb %s' % (__version__,),
                                         'Database connection and management')
    container.add_option('-d', '--db-host-port',
                         metavar='HOST:PORT',
                         default='localhost:3306',
                         help='Database host and port. Default: %default')
    container.add_option('-u', '--db-user',
                         metavar='USER',
                         help='Database user name')
    container.add_option('-p', '--db-pass',
                         metavar='PASS',
                         help='Database password')
    container.add_option('-n', '--db-name',
                         metavar='NAME',
                         help='Database name')
    container.add_option('-i', '--db-ignore',
                         action='store_true',                     
                         help='Ignore the database completely')
    if parser is None:
        return container
    else:
        parser.add_option_group(container)
        return parser

def slurpdb_validate_options(parser, options):
    if options.db_ignore: return
    if options.db_host_port is None: parser.error('-d/--db-host-port is required')
    if ':' not in options.db_host_port: parser.error('-d/--db-host-port does not specify a port')
    if options.db_user is None: parser.error('-u/--db-user is required')
    if options.db_pass is None: parser.error('-p/--db-pass is required')
    if options.db_name is None: parser.error('-n/--db-name is required')

def slurpdb_connection(options):
    db = SlurpDb()
    db_host, db_port_str = options.db_host_port.split(':')
    db.set_db_host(db_host)
    db.set_db_port(int(db_port_str))
    db.set_db_user(options.db_user)
    db.set_db_pass(options.db_pass)
    db.set_db_name(options.db_name)
    return db    

if __name__ == '__main__':
    parser = slurpdb_option_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    slurpdb_validate_options(parser, options)
    db = slurpdb_connection(options)
    db.open_connection()
    db.close_connection()

