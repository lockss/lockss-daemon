#! /usr/bin/env python

# This program contains routines that examine the database for 
# potential problems.

from datetime import date, timedelta, datetime

import MySQLdb
import optparse
import urllib2

# Constants

OPTION_BURPCHECK_VERSION          = '0.0.13'

OPTION_SHORT                    = '-'
OPTION_LONG                     = '--'

OPTION_DATABASE_PASSWORD               = 'dbpassword'
OPTION_DATABASE_PASSWORD_SHORT         = 'D'

OPTION_DATE_REPORT              = 'reportdatestart'
OPTION_DATE_REPORT_SHORT        = 'r'
# A date report of 'None' means the most recent execution 
OPTION_DATE_REPORT_DEFAULT             = None

OPTION_DATE_REPORT_END          = 'reportdateend'
OPTION_DATE_REPORT_END_SHORT    = 'e'
# A date report of 'None' means the most recent execution 
OPTION_DATE_REPORT_END_DEFAULT         = None

OPTION_DATE_PREVIOUS_REPORT              = 'previousreportdatestart'
OPTION_DATE_PREVIOUS_REPORT_SHORT        = 'R'
# A date report of 'None' means the second most recent execution 
OPTION_DATE_PREVIOUS_REPORT_DEFAULT             = None

OPTION_DATE_PREVIOUS_REPORT_END          = 'previousreportdateend'
OPTION_DATE_PREVIOUS_REPORT_END_SHORT    = 'E'
# A date report of 'None' means the second most recent execution 
OPTION_DATE_PREVIOUS_REPORT_END_DEFAULT         = None

OPTION_FILENAME_DISAPPEARING           = 'disappearing'
OPTION_FILENAME_DISAPPEARING_SHORT     = 'd'
OPTION_FILENAME_DISAPPEARING_DEFAULT   = 'Disappearing.txt'

OPTION_FILENAME_INCONSISTENT           = 'inconsistent'
OPTION_FILENAME_INCONSISTENT_SHORT     = 'i'
OPTION_FILENAME_INCONSISTENT_DEFAULT   = 'Inconsistent.txt'

OPTION_FILENAME_YEAR_ZERO               = 'yearzero'
OPTION_FILENAME_YEAR_ZERO_SHORT         = 'z'
OPTION_FILENAME_YEAR_ZERO_DEFAULT       = 'YearZero.txt'

OPTION_MAX_ARTICLE_DIFF                 = 'maxarticlediff'
OPTION_MAX_ARTICLE_DIFF_SHORT           = 'a'
OPTION_MAX_ARTICLE_DIFF_DEFAULT         = 50

OPTION_DAYS_WITHOUT_CRAWL               = 'dayswithoutcrawl'
OPTION_DAYS_WITHOUT_CRAWL_SHORT         = 'w'
OPTION_DAYS_WITHOUT_CRAWL_DEFAULT       = 30


def _make_command_line_parser():
    from optparse import OptionGroup, OptionParser

    parser = OptionParser(version=OPTION_BURPCHECK_VERSION)
    
    parser.add_option(OPTION_SHORT + OPTION_DATABASE_PASSWORD_SHORT,
                      OPTION_LONG + OPTION_DATABASE_PASSWORD,
                      dest=OPTION_DATABASE_PASSWORD,
                      help="The password for the database (required)")

    parser.add_option(OPTION_SHORT + OPTION_FILENAME_INCONSISTENT_SHORT,
                      OPTION_LONG + OPTION_FILENAME_INCONSISTENT,
                      dest = OPTION_FILENAME_INCONSISTENT,
                      default = OPTION_FILENAME_INCONSISTENT_DEFAULT,
                      help = "The filename for the report of AUs with inconsistent data.  (Default: %default)")
    
    parser.add_option(OPTION_SHORT + OPTION_FILENAME_YEAR_ZERO_SHORT,
                      OPTION_LONG + OPTION_FILENAME_YEAR_ZERO,
                      dest = OPTION_FILENAME_YEAR_ZERO,
                      default = OPTION_FILENAME_YEAR_ZERO_DEFAULT,
                      help = "The filename for the report of AUs with a zero year.  (Default: %default)")
    
    parser.add_option(OPTION_SHORT + OPTION_FILENAME_DISAPPEARING_SHORT,
                      OPTION_LONG + OPTION_FILENAME_DISAPPEARING,
                      dest = OPTION_FILENAME_DISAPPEARING,
                      default = OPTION_FILENAME_DISAPPEARING_DEFAULT,
                      help = "The filename for the report of disappearing AUs.  (Default: %default)")
    
    # The date for the report: the date that the 'burp.py' program was run.
    parser.add_option(OPTION_SHORT + OPTION_DATE_REPORT_SHORT,
                      OPTION_LONG + OPTION_DATE_REPORT,
                      dest=OPTION_DATE_REPORT,
                      default=OPTION_DATE_REPORT_DEFAULT,
                      help="The start date when the information was gathered (Default: the previous execution)")

    parser.add_option(OPTION_SHORT + OPTION_DATE_REPORT_END_SHORT,
                      OPTION_LONG + OPTION_DATE_REPORT_END,
                      dest=OPTION_DATE_REPORT_END,
                      default=OPTION_DATE_REPORT_END_DEFAULT,
                      help="The end date when the information was gathered (Default: the previous execution)")

    parser.add_option(OPTION_SHORT + OPTION_DATE_PREVIOUS_REPORT_SHORT,
                      OPTION_LONG + OPTION_DATE_PREVIOUS_REPORT,
                      dest=OPTION_DATE_PREVIOUS_REPORT,
                      default=OPTION_DATE_PREVIOUS_REPORT_DEFAULT,
                      help="The start date when the previous information was gathered (Default: the second previous execution)")

    parser.add_option(OPTION_SHORT + OPTION_DATE_PREVIOUS_REPORT_END_SHORT,
                      OPTION_LONG + OPTION_DATE_PREVIOUS_REPORT_END,
                      dest=OPTION_DATE_PREVIOUS_REPORT_END,
                      default=OPTION_DATE_PREVIOUS_REPORT_END_DEFAULT,
                      help="The end date when the previous information was gathered (Default: the second previous execution)")
    
    parser.add_option(OPTION_SHORT + OPTION_MAX_ARTICLE_DIFF_SHORT,
                      OPTION_LONG + OPTION_MAX_ARTICLE_DIFF,
                      dest=OPTION_MAX_ARTICLE_DIFF,
                      default=OPTION_MAX_ARTICLE_DIFF_DEFAULT,
                      help="If a machine has this many articles fewer than the maximum in an AU, report it.  (Default: %default)")

    parser.add_option(OPTION_SHORT + OPTION_DAYS_WITHOUT_CRAWL_SHORT,
                      OPTION_LONG + OPTION_DAYS_WITHOUT_CRAWL,
                      dest=OPTION_DAYS_WITHOUT_CRAWL,
                      default=OPTION_DAYS_WITHOUT_CRAWL_DEFAULT,
                      help="If an AU has existed for this many days without being crawled, report it.  (Default: %default)")

    return parser


def _check_required_options(parser, options):
    if options.dbpassword is None:
        parser.error('%s%s/%s%s is required' % (OPTION_LONG, OPTION_DATABASE_PASSWORD, OPTION_SHORT, OPTION_DATABASE_PASSWORD_SHORT))


def _update_required_options(db, options):
    cursor = db.cursor()
    cursor.execute("SELECT start, end FROM executions ORDER BY start DESC;")
    
    dates = cursor.fetchone()
    # The following statement fails if all executions.end are None.
    while dates[1] is None:
        dates = cursor.fetchone()
        
    if options.reportdatestart is None:
        options.reportdatestart = dates[0].strftime("%Y-%m-%d %H-%M-%S")
        
    if options.reportdateend is None:
        options.reportdateend = dates[1].strftime("%Y-%m-%d %H-%M-%S")
        
    dates = cursor.fetchone()
    
    if dates is not None:
        if options.previousreportdatestart is None:
            options.previousreportdatestart = dates[0].strftime("%Y-%m-%d %H-%M-%S")
            
        if options.previousreportdateend is None:
            options.previousreportdateend = dates[1].strftime("%Y-%m-%d %H-%M-%S")
        
    cursor.close()    

# Get the list of AUIDs.
# IMPORTANT: When the database of AUIDs exists, use that database!
def _get_auids(db):
    cursor = db.cursor()
    
    auids = []
    
    cursor.execute("SELECT distinct(auid) from burp;")
    arAUID = cursor.fetchone()
    while arAUID is not None:
        auids.append(arAUID[0])
        arAUID = cursor.fetchone()
        
    cursor.close()
        
    return auids


# Test whether the AUID would have been reported.
def _is_reported(auid):
    # If you change this list, be sure to change the equivalent list
    # in BurpReport.py.
    
    return  ( 
             ((auid.find("ClockssHighWirePlugin") != -1) and (auid.find("aappublications") != -1)) or            
             ((auid.find("ClockssHighWirePlugin") != -1) and (auid.find("ama-assn") != -1)) or
             ((auid.find("ClockssHighWirePlugin") != -1) and (auid.find("physiology%2Eorg") != -1)) or            
             ((auid.find("ClockssBerkeleyElectronicPressPlugin") != -1)) or            
             ((auid.find("ClockssNaturePublishingGroupPlugin") != -1)) or            
             ((auid.find("HighWire") != -1) and (auid.find("oxfordjournals") != -1)) or            
             ((auid.find("ClockssRoyalSocietyOfChemistryPlugin") != -1)) or            
             ((auid.find("HighWire") != -1) and (auid.find("sagepub") != -1))
            )
  
    
# List all AUs with year zero
def _find_year_zeros(db, options):
    fileZero = open(options.yearzero, 'w')
    
    isBlankReport = True
    
    cursor = db.cursor()    
    cursor2 = db.cursor()
    cursorMaxDate = db.cursor()
    
    cursor.execute("SELECT auname, auid, machinename, port, rundate FROM burp WHERE auyear = '0' AND rundate >=  '%s' AND rundate <= '%s' GROUP BY auid;" %
                   (str(options.reportdatestart), str(options.reportdateend)))
    arYearZero = cursor.fetchone()
    while arYearZero is not None:
        cursorMaxDate.execute("SELECT MAX(rundate) FROM burp WHERE auid = '%s' AND machinename = '%s' AND port = %d;" % 
                              (arYearZero[1], arYearZero[2], arYearZero[3]))
        arMaxDate = cursorMaxDate.fetchone()
        
        if arMaxDate[0] == arYearZero[4] and _is_reported(arYearZero[1]):
            cursor2.execute("SELECT auyear, machinename, port, rundate FROM burp WHERE auid = '%s' AND auyear > 0 AND rundate < '%s';" %
                            (arYearZero[1], str(options.reportdatestart)))
            year = cursor2.fetchone()            
            if year is not None:
                fileZero.write("********* '%s' is now year 0 on %s:%d, but it had been reported as year %s on %s:%d in the run of %s.\n" %
                               (arYearZero[0], arYearZero[2], arYearZero[3], year[0], year[1], year[2], year[3].strftime("%d-%b-%Y")))
            else:
                fileZero.write("'%s' is year zero on %s:%d.\n" % (arYearZero[0], arYearZero[2], arYearZero[3]))
                
            isBlankReport = False
            
        arYearZero = cursor.fetchone()
        
    if isBlankReport:
        fileZero.write("Congratulations: every AU has an associated year!")
        
    fileZero.close()
    cursor2.close()
    cursor.close()


# List all AUs with inconsistent values for data...
def _find_inconsistent_information(db, options):
    fileInconsistent = open(options.inconsistent, 'w')
    
    isBlankReport = True
    
    cursor        = db.cursor()    
    cursorMachine = db.cursor()
    cursor2       = db.cursor()
    cursor3       = db.cursor()

    print("Looking for inconsistent information within one run.\n")
    cursor.execute("SELECT auid, auname, max(numarticles) FROM burp WHERE rundate >= '%s' AND rundate <= '%s' GROUP BY auid, auname ORDER BY auname;" %
                   (str(options.reportdatestart), str(options.reportdateend)))
    arAuid = cursor.fetchone()
    while arAuid is not None:        
        if _is_reported(arAuid[0]):
            # Verify that the years and names remain consistent across AUs.                        
            cursor2.execute("SELECT COUNT(DISTINCT(auyear)), COUNT(DISTINCT(auname)) FROM burp WHERE auid = '" + arAuid[0] + "' AND rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "';")
            countInformation = cursor2.fetchone()            
            if countInformation[0] > 1:                
                fileInconsistent.write("`" + arAuid[1] + "' has inconsistent years: \n")
                
                cursor3.execute("SELECT DISTINCT(auyear), machinename, port FROM burp WHERE auid = '" + arAuid[0]  + "' AND rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "';")
                year = cursor3.fetchone()
                while year is not None:
                    fileInconsistent.write("%s (on %s:%d) " % (year[0], year[1], year[2]))
                    year = cursor3.fetchone()
                
                fileInconsistent.write("\n\n")                
                isBlankReport = False
                
            if countInformation[1] > 1:
                fileInconsistent.write("`" + arAuid[1] + "' has inconsistent AU Names: \n")

                cursor3.execute("SELECT DISTINCT(auname), machinename, port FROM burp WHERE auid = '" + arAuid[0]  + "' AND rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "';")
                name = cursor3.fetchone()
                while name is not None:
                    fileInconsistent.write("`%s' (on %s:%d) " % (name[0], name[1], name[2]))
                    name = cursor3.fetchone()
                
                fileInconsistent.write("\n\n")                
                isBlankReport = False
                
            # Verify that no articles have had a successful crawl, but still have zero DOIs reported.
            cursor2.execute("SELECT machinename, port FROM burp WHERE auid = '%s' AND rundate >= '%s' AND rundate <= '%s' AND numarticles = 0 AND aulastcrawlresult = 'successful' GROUP BY machinename, port;" % 
                            (arAuid[0], str(options.reportdatestart), str(options.reportdateend)))
            crawledbutzero = cursor2.fetchone()
            crawledbutzeroflag = True
            while crawledbutzero is not None:
                if crawledbutzeroflag:
                    fileInconsistent.write("`%s' had a successful crawl, but still has zero DOIs reported.  Machines it occurred on: " % (arAuid[1],))
                    crawledbutzeroflag = False
                isBlankReport = False
                    
                fileInconsistent.write("%s:%d " %(crawledbutzero[0], crawledbutzero[1]))
                crawledbutzero = cursor2.fetchone()
            if not crawledbutzeroflag:
                fileInconsistent.write("\n\n") 
                
            # Verify that zero DOIs have not been waiting for too long.  
            cursor2.execute("SELECT machinename, port, created FROM burp WHERE auid = '%s' AND rundate >= '%s' AND rundate <= '%s' AND numarticles = 0 AND aulastcrawlresult != 'successful' GROUP BY machinename, port;" %
                            (arAuid[0], str(options.reportdatestart), str(options.reportdateend)))
            notcrawled = cursor2.fetchone()
            while notcrawled is not None and notcrawled[2] is not None:            
                timeSinceCreated = datetime.now() - notcrawled[2];
                if timeSinceCreated > timedelta(OPTION_DAYS_WITHOUT_CRAWL_DEFAULT):
                    fileInconsistent.write("`%s' has been waiting too long for a successful crawl on %s:%d.\n" %
                                           (arAuid[1], notcrawled[0], notcrawled[1]))
                    isBlankReport = False
                notcrawled = cursor2.fetchone()
                 
            # Verify that the current article on one machine does not have fewer articles than any previous run.
            cursorMachine.execute("SELECT machinename, port FROM burp WHERE auid = '%s' AND rundate >= '%s' AND rundate <= '%s' GROUP BY machinename, port;" % (arAuid[0], str(options.reportdatestart), str(options.reportdateend)))
            arMachineName = cursorMachine.fetchone()
            while arMachineName is not None:
                cursor2.execute("SELECT numarticles FROM burp WHERE auid = '%s' AND machinename = '%s' AND port = %d AND rundate >= '%s' AND rundate <= '%s';" % (arAuid[0], arMachineName[0], arMachineName[1], str(options.reportdatestart), str(options.reportdateend)))
                currentNumArticles = cursor2.fetchone()
                cursor3.execute("SELECT MAX(numarticles), rundate FROM burp WHERE auid = '%s' AND machinename = '%s' AND port = %d;" % (arAuid[0], arMachineName[0], arMachineName[1]))
                bestNumArticles = cursor3.fetchone()
            
                # This message should only output if we haven't already reported that it's zero.
                if currentNumArticles[0] < bestNumArticles[0] and currentNumArticles[0] > 0:
                    fileInconsistent.write("`%s' (on %s:%d) has seen its number of articles decrease to %d (current run) from %d (on %s).\n" %(arAuid[1], arMachineName[0], arMachineName[1], currentNumArticles[0], bestNumArticles[0], bestNumArticles[1]))
                    isBlankReport = False
                arMachineName = cursorMachine.fetchone()
                
            # Verify that the maximum number of articles is not significantly greater than the number of articles on any machine.
            cursorMachine.execute("SELECT numarticles, machinename, port FROM burp WHERE auid = '%s' AND rundate >= '%s' AND rundate <= '%s' GROUP BY machinename, port;" % (arAuid[0], str(options.reportdatestart), str(options.reportdateend)))
            arNumArticles = cursorMachine.fetchone()
            while arNumArticles is not None:
                if arNumArticles[0] + options.maxarticlediff < arAuid[2] and arNumArticles[0] > 0:
                    fileInconsistent.write("'%s' (on %s:%d) has significantly fewer articles than the maximum.  It has %d articles, and the maximum is %d.\n" % (arAuid[1], arNumArticles[1], arNumArticles[2], arNumArticles[0], arAuid[2]))
                    isBlankReport = False
                arNumArticles = cursorMachine.fetchone()
                
        arAuid = cursor.fetchone()
        
    # Problems within the burp report (comparing reports, report by report.)
    print("Testing problems within report.\n")
    cursor.execute("SELECT publisher, auyear FROM burpreport WHERE auyear != '0' GROUP BY publisher, auyear;")
    arPublisherYear = cursor.fetchone()
    while arPublisherYear is not None:
        cursor2.execute("SELECT MAX(numarticles), rundate FROM burpreport WHERE publisher = '%s' AND auyear = '%s'" % (arPublisherYear[0], arPublisherYear[1]))
        highestInYear = cursor2.fetchone()
        cursor3.execute("SELECT numarticles FROM burpreport WHERE publisher = '%s' AND auyear = '%s' ORDER BY rundate DESC" % (arPublisherYear[0], arPublisherYear[1]))
        mostRecent = cursor3.fetchone()
        
        if mostRecent[0] < highestInYear[0]:
            fileInconsistent.write("Publisher %s in year %s has seen its total number of articles decrease from %d (in report generated on %s) to %d.\n" % 
                                   (arPublisherYear[0], arPublisherYear[1], highestInYear[0], highestInYear[1].strftime("%d-%b-%Y"), mostRecent[0]))
            isBlankReport = False
        
        arPublisherYear = cursor.fetchone()
        
    # List all AUs that have disappeared since the previous run.
    print("Listing disappearing AUs.\n")
    cursor.execute("SELECT auid, auname, machinename, port FROM burp WHERE rundate >= '%s' AND rundate <= '%s' GROUP BY auid, auname ORDER BY auname;" %
                   (str(options.previousreportdatestart), str(options.previousreportdateend)))
    arPreviousAu = cursor.fetchone()
    while arPreviousAu is not None:
        cursor2.execute("SELECT auname FROM burp WHERE auid = '%s' AND machinename = '%s' AND port = %d AND rundate >= '%s' AND rundate <= '%s'" %
                        (arPreviousAu[0], arPreviousAu[2], arPreviousAu[3], str(options.reportdatestart), str(options.reportdateend)))
        arFound = cursor.fetchone
        
        if arFound is None:
            fileInconsistent.write("The AU '%s' was known by %s:%d in the previous run, but not in the current run.\n" % 
                                   (arPreviousAu[1], arPreviousAu[2], arPreviousAu[3]))
            isBlankReport = False
        arPreviousAu = cursor.fetchone()
        
    if isBlankReport:
        fileInconsistent.write("Congratulations: no AU has inconsistent data!\n")
        
    fileInconsistent.close()
    cursor3.close()
    cursor2.close()
    cursor.close()
    

def _main_procedure():
    parser = _make_command_line_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    _check_required_options(parser, options)

    db = MySQLdb.connect(host="localhost", user="edwardsb", passwd=options.dbpassword, db="burp")

    _update_required_options(db, options)

    _find_year_zeros(db, options)
    _find_inconsistent_information(db, options)


if __name__ == '__main__':    
    _main_procedure()
