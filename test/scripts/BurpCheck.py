#! /usr/bin/env python

# This program contains routines that examine the database for 
# potential problems.

from datetime import date, timedelta

import MySQLdb
import optparse

# Constants

OPTION_BURPCHECK_VERSION          = '0.0.0'

OPTION_SHORT                    = '-'
OPTION_LONG                     = '--'

OPTION_DATABASE_PASSWORD               = 'dbpassword'
OPTION_DATABASE_PASSWORD_SHORT         = 'D'

OPTION_DATE_REPORT              = 'reportdatestart'
OPTION_DATE_REPORT_SHORT        = 'r'
# A date report of 'None' means the most recent execution 
DEFAULT_DATE_REPORT             = None

OPTION_DATE_REPORT_END          = 'reportdateend'
OPTION_DATE_REPORT_END_SHORT    = 'e'
# A date report of 'None' means the most recent execution 
DEFAULT_DATE_REPORT_END         = None

OPTION_DATE_PREVIOUS_REPORT              = 'previousreportdatestart'
OPTION_DATE_PREVIOUS_REPORT_SHORT        = 'R'
# A date report of 'None' means the second most recent execution 
DEFAULT_DATE_PREVIOUS_REPORT             = None

OPTION_DATE_PREVIOUS_REPORT_END          = 'previousreportdateend'
OPTION_DATE_PREVIOUS_REPORT_END_SHORT    = 'E'
# A date report of 'None' means the second most recent execution 
DEFAULT_DATE_PREVIOUS_REPORT_END         = None



OPTION_FILENAME_DISAPPEARING           = 'disappearing'
OPTION_FILENAME_DISAPPEARING_SHORT     = 'd'
OPTION_FILENAME_DISAPPEARING_DEFAULT   = 'Disappearing.txt'

OPTION_FILENAME_INCONSISTENT           = 'inconsistent'
OPTION_FILENAME_INCONSISTENT_SHORT     = 'i'
OPTION_FILENAME_INCONSISTENT_DEFAULT   = 'Inconsistent.txt'

OPTION_FILENAME_YEAR_ZERO               = 'yearzero'
OPTION_FILENAME_YEAR_ZERO_SHORT         = 'z'
OPTION_FILENAME_YEAR_ZERO_DEFAULT       = 'YearZero.txt'


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
                      default=DEFAULT_DATE_REPORT,
                      help="The start date when the information was gathered (default: the previous execution)")

    parser.add_option(OPTION_SHORT + OPTION_DATE_REPORT_END_SHORT,
                      OPTION_LONG + OPTION_DATE_REPORT_END,
                      dest=OPTION_DATE_REPORT_END,
                      default=DEFAULT_DATE_REPORT_END,
                      help="The end date when the information was gathered (default: the previous execution)")

    parser.add_option(OPTION_SHORT + OPTION_DATE_PREVIOUS_REPORT_SHORT,
                      OPTION_LONG + OPTION_DATE_PREVIOUS_REPORT,
                      dest=OPTION_DATE_PREVIOUS_REPORT,
                      default=DEFAULT_DATE_PREVIOUS_REPORT,
                      help="The start date when the previous information was gathered (default: the second previous execution)")

    parser.add_option(OPTION_SHORT + OPTION_DATE_PREVIOUS_REPORT_END_SHORT,
                      OPTION_LONG + OPTION_DATE_PREVIOUS_REPORT_END,
                      dest=OPTION_DATE_PREVIOUS_REPORT_END,
                      default=DEFAULT_DATE_PREVIOUS_REPORT_END,
                      help="The end date when the previous information was gathered (default: the second previous execution)")

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
    
    isEmpty = True
    
    cursor = db.cursor()    
    cursor2 = db.cursor()
    cursor.execute("SELECT distinct(auname), auid FROM burp WHERE auyear = '0' AND rundate >=  '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "';")
    
    arYearZero = cursor.fetchone()
    while arYearZero is not None:
        if _is_reported(arYearZero[1]):
            cursor2.execute("SELECT auyear FROM burp WHERE auid = '" + arYearZero[1] + "' AND auyear > 0;")
            year = cursor2.fetchone()            
            if year is not None:
                fileZero.write("********* '" + arYearZero[0] + "' is now year 0, but was previously " + year[0] + "\n")
            else:
                fileZero.write(arYearZero[0] + "\n")
                
            isEmpty = False
            
        arYearZero = cursor.fetchone()
        
    if isEmpty:
        fileZero.write("Congratulations: every AU has an associated year!")
        
    fileZero.close()
    cursor2.close()
    cursor.close()


# List all AUs with inconsistent values for data...
def _find_inconsistent_information(db, options):
    fileInconsistent = open(options.inconsistent, 'w')
    
    isEmpty = True
    
    cursor        = db.cursor()    
    cursorMachine = db.cursor()
    cursor2       = db.cursor()
    cursor3       = db.cursor()
    
    cursor.execute("SELECT auname, auid, machinename, port FROM burp WHERE rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "' GROUP BY auid;")
    
    arAuid = cursor.fetchone()
    while arAuid is not None:        
        if _is_reported(arAuid[1]):
            
            # Verify that the years and names remain consistent across AUs.                        
            cursor2.execute("SELECT COUNT(DISTINCT(auyear)), COUNT(DISTINCT(auname)) FROM burp WHERE auid = '" + arAuid[1] + "' AND rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "';")
            countInformation = cursor2.fetchone()            
            if countInformation[0] > 1:                
                fileInconsistent.write("`" + arAuid[0] + "' has inconsistent years: \n")
                
                cursor3.execute("SELECT DISTINCT(auyear), machinename, port FROM burp WHERE auid = '" + arAuid[1]  + "' AND rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "';")
                year = cursor3.fetchone()
                while year is not None:
                    fileInconsistent.write("%s (on %s:%d) " % (year[0], year[1], year[2]))
                    year = cursor3.fetchone()
                
                fileInconsistent.write("\n\n")                
                isEmpty = False
                
            if countInformation[1] > 1:
                fileInconsistent.write("`" + arAuid[0] + "' has inconsistent AU Names: \n")

                cursor3.execute("SELECT DISTINCT(auname), machinename, port FROM burp WHERE auid = '" + arAuid[1]  + "' AND rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "';")
                name = cursor3.fetchone()
                while name is not None:
                    fileInconsistent.write("`%s' (on %s:%d) " % (name[0], name[1], name[2]))
                    name = cursor3.fetchone()
                
                fileInconsistent.write("\n\n")                
                isEmpty = False
                
            # Verify that no articles have had a successful crawl, but still have zero DOIs reported.
            cursor2.execute("SELECT aulastcrawlresult, numarticles FROM burp WHERE auid = '%s' AND rundate >= '%s' AND rundate <= '%s';" % (arAuid[1], str(options.reportdatestart), str(options.reportdateend)))
            crawledbutzero = cursor2.fetchone()
            while crawledbutzero is not None:
                if crawledbutzero[0] == "Successful" and crawledbutzero[1] == 0:                    
                    fileInconsistent.write("`%s' had a successful crawl, but still has zero DOIs reported.  Machines it occurred on: " % (arAuid[0], crawledbutzero[2], crawledbutzero[3]))
                    isEmpty = False
                    
                    cursorMachine.execute("SELECT machinename, port FROM BURP where auid = '%s' aulastcrawlresult = 'Successful' AND numarticles = 0 AND rundate >= '%s' AND rundate <= '%s'" % (arauid[1], str(options.reportdatestart), str(options.reportdateend)))
                    arMachineName = cursorMachine.fetchone()
                    while arMachineName is not None:
                        fileInconsistent.write("%s:%d " %(arMachineName[0], arMachineName[1])) 
                crawledbutzero = cursor2.fetchone()
                
            # Verify that the current article on one machine does not have fewer articles than any previous run.
            cursorMachine.execute("SELECT machinename, port FROM burp WHERE auid = '%s' AND rundate >= '%s' AND rundate <= '%s';" % (arAuid[1], str(options.reportdatestart), str(options.reportdateend)))
            arMachineName = cursorMachine.fetchone()
            while arMachineName is not None:
                cursor2.execute("SELECT numarticles FROM burp WHERE auid = '%s' AND machinename = '%s' AND port = %d AND rundate >= '%s' AND rundate <= '%s';" % (arAuid[1], arMachineName[0], arMachineName[1], str(options.reportdatestart), str(options.reportdateend)))
                currentNumArticles = cursor2.fetchone()
                cursor3.execute("SELECT MAX(numarticles), rundate FROM burp WHERE auid = '%s' AND machinename = '%s' AND port = %d;" % (arAuid[1], arMachineName[0], arMachineName[1]))
                bestNumArticles = cursor3.fetchone()
            
                if currentNumArticles[0] < bestNumArticles[0]:
                    fileInconsistent.write("`%s' (on %s:%d) has seen its number of articles decrease from %d (current run) to %d (on %s).\n" %(arAuid[0], arMachineName[0], arMachineName[1], currentNumArticles[0], bestNumArticles[0], bestNumArticles[1]))
                arMachineName = cursorMachine.fetchone()
             
        arAuid = cursor.fetchone()
        
    if isEmpty:
        fileInconsistent.write("Congratulations: no AU has inconsistent data!")
        
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
