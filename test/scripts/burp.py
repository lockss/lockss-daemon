#!/usr/bin/python

'''
Created on Dec 4, 2009

@author: edwardsb, thibgc

This script is part of the toolset to generate the monthly DOI report.
(The other two parts are "BurpReport.py", and "run-burp.py".)  It queries
one other machine (given as the -H parameter), and inserts its results into
a database.

It assumes that there is a MySQL database program running, with the following 
tables created:

create table burp(machinename VARCHAR(127), port INT, rundate DATE, auname VARCHAR(255), 
auid VARCHAR(2047), auyear VARCHAR(32), austatus VARCHAR(255), aulastcrawlresult VARCHAR(255), 
aucontentsize VARCHAR(255), audisksize REAL, aurepository VARCHAR(255), numarticles INT, 
publisher VARCHAR(16));

create table lastcomplete(machinename VARCHAR(127), port INT, completedate DATE);
'''
import datetime
import httplib
import MySQLdb
import optparse
import os
import sys
import urllib2

sys.path.append(os.path.realpath(os.path.join(os.path.dirname(sys.argv[0]), '../frameworks/lib')))
import lockss_daemon


BURP_VERSION = '0.2.1'

OPTION_LONG  = '--'
OPTION_SHORT = '-'

OPTION_HOST                     = 'host'
OPTION_HOST_SHORT               = 'H'
OPTION_PASSWORD                 = 'password'
OPTION_PASSWORD_SHORT           = 'P'
DEFAULT_PASSWORD                = 'lockss-p'
OPTION_USERNAME                 = 'username'
OPTION_USERNAME_SHORT           = 'U'
DEFAULT_USERNAME                = 'lockss-u'

OPTION_DATABASE_PASSWORD        = 'dbpassword'
OPTION_DATABASE_PASSWORD_SHORT  = 'D'

PARAM_READY_TIMEOUT             = 30
PARAM_REPEAT_LIST_ARTICLES      = 5  

def _make_command_line_parser():
    from optparse import OptionGroup, OptionParser
    parser = OptionParser(version=BURP_VERSION)

    parser.add_option(OPTION_SHORT + OPTION_HOST_SHORT,
                      OPTION_LONG + OPTION_HOST,
                      dest=OPTION_HOST,
                      help='daemon hostname:port (required)')
    parser.add_option(OPTION_SHORT + OPTION_PASSWORD_SHORT,
                      OPTION_LONG + OPTION_PASSWORD,
                      dest=OPTION_PASSWORD,
                      default=DEFAULT_PASSWORD,
                      help='daemon UI password (default: %default)')
    parser.add_option(OPTION_SHORT + OPTION_USERNAME_SHORT,
                      OPTION_LONG + OPTION_USERNAME,
                      dest=OPTION_USERNAME,
                      default=DEFAULT_USERNAME,
                      help='daemon UI username (default: %default)')
    
    # The database password
    parser.add_option(OPTION_SHORT + OPTION_DATABASE_PASSWORD_SHORT,
                      OPTION_LONG + OPTION_DATABASE_PASSWORD,
                      dest=OPTION_DATABASE_PASSWORD,
                      help="The password for the database (required)")
    
    return parser
    
def _check_required_options(parser, options):
    if options.host is None:
        parser.error('%s%s/%s%s is required' % (OPTION_LONG, OPTION_HOST, OPTION_SHORT, OPTION_HOST_SHORT))
    if options.dbpassword is None:
        parser.error('%s%s/%s%s is required' % (OPTION_LONG, OPTION_DATABASE_PASSWORD, OPTION_SHORT, OPTION_DATABASE_PASSWORD_SHORT))

def _make_client(options):
    client =  lockss_daemon.Client(options.host.split(':',1)[0],
                                   options.host.split(':',1)[1],
                                   options.username,
                                   options.password)
    if not client.waitForDaemonReady(PARAM_READY_TIMEOUT):
        raise RuntimeError, '%s is not ready after %d seconds' % (options.host, PARAM_READY_TIMEOUT)
    return client
    
def _get_auids(client):
    table = client._getStatusTable('AuIds')[1]
    auids = []
    auname = {}
    for map in table:
        id = map['AuId']
        auids.append(id)
        auname[id] = map['AuName']['value']
    return auids, auname

def _get_list_articles(client, auid, auarticles):
    reps = 0

    while (reps < PARAM_REPEAT_LIST_ARTICLES):
        try:
            lst = client.getListOfArticles(lockss_daemon.AU(auid))
            val = []
            if lst is not None and len(lst) > 0: val = lst.splitlines()[2:]
            auarticles[auid] = val
            break
        except urllib2.URLError:
            reps = reps + 1
            print "_get_list_articles has URLError.  This is repeat %d." % (reps,)
    else:
        raise RuntimeError, '%s did not give the list of articles after %d tries' % (options.host, PARAM_REPEAT_LIST_ARTICLES)

def _get_list_urls(client, auid, auarticles):
    lst = client.getListOfUrls(lockss_daemon.AU(auid))
    val = []
    if lst is not None and len(lst) > 0:
        for art in lst.splitlines()[2:]:
            if art.startswith('http://www.rsc.org/publishing/journals/') and art.find('/article.asp?doi=') >= 0:
                val.append(art)
    auarticles[auid] = val


def _need_report(db, options):
    hostname = options.host.split(':',1)[0]
    port = options.host.split(':',1)[1]
    
    cursorQuery = db.cursor()
    cursorQuery.execute("SELECT MAX(completedate) FROM lastcomplete WHERE machinename = \"" + str(hostname) + "\" AND port = " + port + ";")
    lastComplete = cursorQuery.fetchone()
    
    if lastComplete[0] is None:
        return True
    
    # lastComplete[0] seems to be a datetime.
    difference = datetime.timedelta(days=2)
    twodaysago = datetime.date.today() - difference
        
    return lastComplete[0] < twodaysago

    
def _article_report(client, db, options):
    auids, auname = _get_auids(client)
    host, port = options.host.split(':',1)
    auyear = {}
    austatus = {}
    aulastcrawlresult = {}
    aucontentsize = {}
    audisksize = {}
    aurepository = {}
    auarticles = {}
    cursor = db.cursor()
    for auid in auids:
        summary, table = client._getStatusTable('ArchivalUnitTable', auid)
        auyear[auid] = summary.get('Year', 0)
        austatus[auid] = summary.get('Status')
        aulastcrawlresult[auid] = summary.get('Last Crawl Result', 'n/a')
        aucontentsize[auid] = summary.get('Content Size')
        if aucontentsize[auid] <> None:
            aucontentsize[auid].replace(",", "")
        else:
            aucontentsize[auid] = ""
        audisksize[auid] = summary.get('Disk Usage (MB)', 'n/a')
        aurepository[auid] = summary.get('Repository')
        _get_list_articles(client, auid, auarticles)
      
        # Because it's hard to know if the Burp is running without SOME feedback...
        print options.host + ":" + auname[auid]
        
        # Standard article...
        cursor.execute("""INSERT INTO burp(machinename, port, rundate, 
auname, auid, auyear, austatus, aulastcrawlresult, aucontentsize, audisksize, 
aurepository, numarticles, publisher)
VALUES ("%s", "%s", CURRENT_DATE(), "%s", "%s", "%s", "%s", "%s", "%s", "%s", 
"%s", "%s", "default")"""  % \
                       (host, port, auname[auid], auid, auyear[auid], austatus[auid], aulastcrawlresult[auid], aucontentsize[auid], audisksize[auid], aurepository[auid], int(len(auarticles[auid]))))
        # Note: There is no article iterator for RSC.  This is a work-around.
        if auid.find('ClockssRoyalSocietyOfChemistryPlugin') >= 0 and (options.host.find("ingest") >= 0):
            _get_list_urls(client, auid, auarticles)
            cursor.execute("""INSERT INTO burp(machinename, port, rundate, 
auname, auid, auyear, numarticles, publisher)
VALUES ("%s", %d, CURRENT_DATE(), "%s", "%s", "%s", %d, "rsc")""" % \
                            (host, port, auname[auid], auid, auyear[auid], len(auarticles[auid])))
    
    cursor.execute("INSERT INTO lastcomplete(machinename, port, completedate) VALUES (\"%s\", %d, CURRENT_DATE())" %
                   (host, int(port)))
    
    print "****** " + options.host + " finished ******"

            
def _main_procedure():
    parser = _make_command_line_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    _check_required_options(parser, options)

    try:
        db = MySQLdb.connect(host="localhost", user="edwardsb", passwd=options.dbpassword, db="burp")
    
# Send the reports
        if _need_report(db, options):
            client = _make_client(options)
            _article_report(client, db, options)

    except:
        print "****** " + options.host + ": Error."
        raise
    finally:
        db.commit()
        db.close()

if __name__ == '__main__':    
    _main_procedure()
