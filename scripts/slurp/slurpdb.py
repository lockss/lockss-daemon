#!/usr/bin/env python

# $Id: slurpdb.py,v 1.4 2012-08-25 00:38:51 thib_gc Exp $

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
import MySQLdb
import optparse
import os
import sys

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
        self.__raise_if_closed()
        cursor = self.__cursor()
        cursor.execute('''\
                INSERT INTO %s
                (%s, %s)
                VALUES (%%s, %%s)
        ''' % (AUIDS,
               AUIDS_SID, AUIDS_AUID),
        (sid, auid))
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
                disk_usage):
        self.__raise_if_closed()
        # Normalize
        name = self.__str_to_db(name, AUS_NAME)
        publisher = self.__str_to_db(publisher, AUS_PUBLISHER)
        repository = self.__str_to_db(repository, AUS_REPOSITORY)
        creation_date = self.__datetime_to_db(creation_date)
        status = self.__str_to_db(status, AUS_STATUS)
        last_crawl = self.__datetime_to_db(last_crawl)
        last_crawl_result = self.__str_to_db(last_crawl_result, AUS_LAST_CRAWL_RESULT)
        last_completed_crawl = self.__datetime_to_db(last_completed_crawl)
        last_poll = self.__datetime_to_db(last_poll)
        last_poll_result = self.__str_to_db(last_poll_result, AUS_LAST_POLL_RESULT)
        last_completed_poll = self.__datetime_to_db(last_completed_poll)
        # Insert
        cursor = self.__cursor()
        cursor.execute('''\
                INSERT INTO %s
                (%s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s, %s, %s, %s)
                VALUES (%%s, %%s, %%s, %%s,
                    %%s, %%s, %%s, %%s,
                    %%s, %%s, %%s, %%s,
                    %%s, %%s, %%s, %%s)
        ''' % (AUS,
               AUS_AID, AUS_NAME, AUS_PUBLISHER, AUS_YEAR,
               AUS_REPOSITORY, AUS_CREATION_DATE, AUS_STATUS, AUS_AVAILABLE,
               AUS_LAST_CRAWL, AUS_LAST_CRAWL_RESULT, AUS_LAST_COMPLETED_CRAWL, AUS_LAST_POLL,
               AUS_LAST_POLL_RESULT, AUS_LAST_COMPLETED_POLL, AUS_CONTENT_SIZE, AUS_DISK_USAGE),
        (aid, name, publisher, year,
         repository, creation_date, status, available,
         last_crawl, last_crawl_result, last_completed_crawl, last_poll,
         last_poll_result, last_completed_poll, content_size, disk_usage))
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
        f = open(os.path.dirname(sys.argv[0]) + os.sep + table.name() +
                '.' + str(table.version()) + '.sql', 'r')
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
            f = open(os.path.dirname(sys.argv[0]) + os.sep + table.name() +
                    '.' + str(current_version) + '.' + str(current_version + 1) +
                    '.sql', 'r')
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

AUIDS = Table('auids', 1)
AUIDS_ID = Column(AUIDS, 'id')
AUIDS_SID = Column(AUIDS, 'sid')
AUIDS_AUID = Column(AUIDS, 'auid', 511)

AUS = Table('aus', 1)
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

ALL_TABLES = [TABLES,
              SESSIONS,
              AUIDS,
              AUS]

def slurpdb_option_parser(parser=None):
    if parser is None:
        container = optparse.OptionParser(version=__version__,
                                          description='Initializes or updates a Slurp database.')
    else:
        container = optparse.OptionGroup(parser,
                                         'slurpdb %s' % (__version__,),
                                         'Database connection and management')
    container.add_option('-d', '--db-host-port',
                         metavar='HOSTPORT',
                         default='localhost:3306',
                         help='Database host and port. Default: %default')
    container.add_option('-u', '--db-user',
                         metavar='USER',
                         help='Database user name.')
    container.add_option('-p', '--db-pass',
                         metavar='PASS',
                         help='Database password.')
    container.add_option('-n', '--db-name',
                         metavar='NAME',
                         help='Database name.')
    if parser is None:
        return container
    else:
        parser.add_option_group(container)
        return parser

def slurpdb_process_options(parser, options):
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
    slurpdb_process_options(parser, options)
    db = slurpdb_connection(options)
    db.open_connection()
    db.close_connection()

