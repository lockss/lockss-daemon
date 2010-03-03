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

OPTION_FILENAME_DISAPPEARING           = 'disappearing'
OPTION_FILENAME_DISAPPEARING_SHORT     = 'd'
OPTION_FILENAME_DISAPPEARING_DEFAULT   = 'Disappearing.txt'

OPTION_FILENAME_MULTIPLE_YEARS         = 'multiple'
OPTION_FILENAME_MULTIPLE_YEARS_SHORT   = 'm'
OPTION_FILENAME_MULTIPLE_YEARS_DEFAULT = 'Multiple.txt'

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

    parser.add_option(OPTION_SHORT + OPTION_FILENAME_MULTIPLE_YEARS_SHORT,
                      OPTION_LONG + OPTION_FILENAME_MULTIPLE_YEARS,
                      dest = OPTION_FILENAME_MULTIPLE_YEARS,
                      default = OPTION_FILENAME_MULTIPLE_YEARS_DEFAULT,
                      help = "The filename for the report of AUs with multiple years.  (Default: %default)")
    
    parser.add_option(OPTION_SHORT + OPTION_FILENAME_YEAR_ZERO_SHORT,
                      OPTION_LONG + OPTION_FILENAME_YEAR_ZERO,
                      dest = OPTION_FILENAME_YEAR_ZERO,
                      default = OPTION_FILENAME_YEAR_ZERO_DEFAULT,
                      help = "The filename for the report of AUs with a zero year.  (Default: %default)")
    
    parser.add_option(OPTION_SHORT + OPTION_FILENAME_DISAPPEARING_SHORT,
                      OPTION_LONG + OPTION_FILENAME_DISAPPEARING,
                      dest = OPTION_FILENAME_DISAPPEARING,
                      default = OPTION_FILENAME_MULTIPLE_YEARS_DEFAULT,
                      help = "The filename for the report of disappearing AUs.  (Default: %default)")
    
    # The date for the report: the date that the 'burp.py' program was run.
    parser.add_option(OPTION_SHORT + OPTION_DATE_REPORT_SHORT,
                      OPTION_LONG + OPTION_DATE_REPORT,
                      dest=OPTION_DATE_REPORT,
                      default=DEFAULT_DATE_REPORT,
                      help="The start date the information was gathered (default: the previous execution)")

    parser.add_option(OPTION_SHORT + OPTION_DATE_REPORT_END_SHORT,
                      OPTION_LONG + OPTION_DATE_REPORT_END,
                      dest=OPTION_DATE_REPORT_END,
                      default=DEFAULT_DATE_REPORT_END,
                      help="The end date the information was gathered (default: the previous execution)")

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
    cursor.execute("SELECT distinct(auname), auid FROM burp WHERE auyear = '0' AND rundate >= DATE( '" + str(options.reportdatestart) + "') AND rundate <= DATE('" + str(options.reportdateend) + "');")
    
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


# List all AUs with multiple years    
def _find_multiple_years(db, options):
    fileMultiple = open(options.multiple, 'w')
    
    isEmpty = True
    
    cursor = db.cursor()    
    cursor2 = db.cursor()
    cursor3 = db.cursor()
    
    cursor.execute("SELECT DISTINCT(auname), auid FROM burp WHERE rundate >= DATE( '" + str(options.reportdatestart) + "') AND rundate <= DATE('" + str(options.reportdateend) + "');")
    
    arAuid = cursor.fetchone()
    while arAuid is not None:        
        if _is_reported(arAuid[1]):                        
            cursor2.execute("SELECT COUNT(DISTINCT(auyear)) FROM burp WHERE auid = '" + arAuid[1] + "' AND rundate >= DATE( '" + str(options.reportdatestart) + "') AND rundate <= DATE('" + str(options.reportdateend) + "');")
            countYear = cursor2.fetchone()            
            if countYear[0] > 1:                
                fileMultiple.write("'" + arAuid[0] + "' has multiple years: ")
                
                cursor3.execute("SELECT DISTINCT(auyear) FROM burp WHERE auid = '" + arAuid[1]  + "' AND rundate >= DATE( '" + str(options.reportdatestart) + "') AND rundate <= DATE('" + str(options.reportdateend) + "');")
                year = cursor3.fetchone()
                while year is not None:
                    fileMultiple.write(year[0] + " ")
                    year = cursor3.fetchone()
                
                fileMultiple.write("\n")                
                isEmpty = False
            
        arAuid = cursor.fetchone()
        
    if isEmpty:
        fileMultiple.write("Congratulations: no AUs have multiple years!")
        
    fileMultiple.close()
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
    _find_multiple_years(db, options)


if __name__ == '__main__':    
    _main_procedure()
