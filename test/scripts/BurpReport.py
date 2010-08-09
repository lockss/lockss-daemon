#!/usr/bin/python
'''
Created on Dec 14, 2009

@author: edwardsb

This program generates the Burp report, in the format that Vicky requested.

[We would like...] a table that was organized with all the data in one spreadsheet, for example:
              PUBLISHER           PUBLISHER           PUBLISHER
YEAR      # articles ingested # articles ingested # articles ingested
YEAR      # articles ingested # articles ingested # articles ingested
YEAR     # articles ingested # articles ingested # articles ingested 
'''

from datetime import date, timedelta
import MySQLdb
import optparse
import os
import sys

# Constants
OPTION_BURP_VERSION             = '0.2.2'

OPTION_SHORT                    = '-'
OPTION_LONG                     = '--'

OPTION_DATABASE_PASSWORD        = 'dbpassword'
OPTION_DATABASE_PASSWORD_SHORT  = 'D'

OPTION_YEAR_CURRENT             = 'currentyear'
OPTION_YEAR_CURRENT_SHORT       = 'c'
DEFAULT_YEAR_CURRENT            = date.today().year

OPTION_YEAR_MINIMUM             = 'minimumyear'
OPTION_YEAR_MINIMUM_SHORT       = 'm'
DEFAULT_YEAR_MINIMUM            = 1883

OPTION_DATE_REPORT              = 'reportdatestart'
OPTION_DATE_REPORT_SHORT        = 'r'
DEFAULT_DATE_REPORT             = None

OPTION_DATE_REPORT_END          = 'reportdateend'
OPTION_DATE_REPORT_END_SHORT    = 'e'
DEFAULT_DATE_REPORT_END         = None

OPTION_FILENAME_SHORT           = 'f'
OPTION_FILENAME                 = 'filename'
DEFAULT_FILENAME                = 'current.csv'

OPTION_SUMMARY_SHORT            = 's'
OPTION_SUMMARY                  = 'summary'
DEFAULT_SUMMARY                 = 'summary.csv'

def _make_command_line_parser():
    from optparse import OptionGroup, OptionParser

    parser = OptionParser(version=OPTION_BURP_VERSION)

    # The database password
    parser.add_option(OPTION_SHORT + OPTION_DATABASE_PASSWORD_SHORT,
                      OPTION_LONG + OPTION_DATABASE_PASSWORD,
                      dest=OPTION_DATABASE_PASSWORD,
                      help="The password for the database (required)")
    
    # Minimum and current years
    parser.add_option(OPTION_SHORT + OPTION_YEAR_CURRENT_SHORT,
                      OPTION_LONG + OPTION_YEAR_CURRENT,
                      dest=OPTION_YEAR_CURRENT,
                      default=DEFAULT_YEAR_CURRENT,
                      help="The current year (default: %default)")
    parser.add_option(OPTION_SHORT + OPTION_YEAR_MINIMUM_SHORT,
                      OPTION_LONG + OPTION_YEAR_MINIMUM,
                      dest=OPTION_YEAR_MINIMUM,
                      default=DEFAULT_YEAR_MINIMUM,
                      help="The first year to report on (default: %default)")

    # The date for the report: the date that the 'burp.py' program was run.
    parser.add_option(OPTION_SHORT + OPTION_DATE_REPORT_SHORT,
                      OPTION_LONG + OPTION_DATE_REPORT,
                      dest=OPTION_DATE_REPORT,
                      default=DEFAULT_DATE_REPORT,
                      help="The start date the information was gathered (default: %default)")

    parser.add_option(OPTION_SHORT + OPTION_DATE_REPORT_END_SHORT,
                      OPTION_LONG + OPTION_DATE_REPORT_END,
                      dest=OPTION_DATE_REPORT_END,
                      default=DEFAULT_DATE_REPORT_END,
                      help="The end date the information was gathered (default: %default)")

    parser.add_option(OPTION_SHORT + OPTION_FILENAME_SHORT,
                      OPTION_LONG + OPTION_FILENAME,
                      dest=OPTION_FILENAME,
                      default=DEFAULT_FILENAME,
                      help="The filename for the main report (default: %default)")

    parser.add_option(OPTION_SHORT + OPTION_SUMMARY_SHORT,
                      OPTION_LONG + OPTION_SUMMARY,
                      dest=OPTION_SUMMARY,
                      default=DEFAULT_SUMMARY,
                      help="The filename for the summary (default: %default)")

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


def _print_summary_line(summary, publishername, publisherid, currentyear, pubyear, total):
    summary.write(publishername + "," + publisherid.upper() + ",")
    summary.write(str(pubyear[publisherid][currentyear]) + ",")
    summary.write(str(total[publisherid]) + "\n")


def _main_procedure():
    parser = _make_command_line_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    _check_required_options(parser, options)

    db = MySQLdb.connect(host="localhost", user="edwardsb", passwd=options.dbpassword, db="burp")
    _update_required_options(db, options)

    # Initialize the hashes.
    # WARNING: If you update this list, you need to update three places:
    # 1. The list of publishers in the 'while auid is not None' loop.
    # 2. The summary report
    # 3. In BurpCheck.py, the _is_reported() method.
    publishers = ['aap',
                  'aip',
                  'ama',
                  'aps',
                  'acm',
                  'bep',
                  'bmc',
                  'cap',
                  'eup',
                  'elsevier',
                  'gtv',
                  'iop',
                  'wiley',
                  'lup',
                  'npg',
                  'oup',
                  'rup',
                  'rsc',
                  'rsp',
                  'sage',
                  'springer',
                  'ssr',
                  'tf']

    total = {}
    pubyear = {}

    for publisher in publishers:
        pubyear[publisher] = {}
        total[publisher] = 0

        for year in range(options.currentyear, options.minimumyear - 1, -1) + ["0"]:
            strYear = str(year)
            pubyear[publisher][strYear] = 0
    
    cursorAuid = db.cursor()
    cursorAuid.execute("SELECT DISTINCT(auid) from burp WHERE rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) + "' order by auid;")
    
    auidrow = cursorAuid.fetchone()
    while auidrow is not None:
        auid = auidrow[0]
        cursorArticles = db.cursor()
        cursorArticles.execute("SELECT MAX(numarticles), auyear FROM burp WHERE auid = \"" + auid + "\" and rundate >= '" + str(options.reportdatestart) + "' AND rundate <= '" + str(options.reportdateend) +"';")
        articles = cursorArticles.fetchone()
        strYear = articles[1]
        if "-" in strYear:
            strYear = strYear[5:9]
        
        # If you change this list, be sure to change the equivalent
        # list in BurpCheck.py.
        
        # aap
        if ('ClockssHighWirePlugin' in auid or 'ClockssHighWirePressH20Plugin' in auid) and ('aappublications' in auid):
            pubyear['aap'][strYear] += articles[0]
            total['aap'] += articles[0]

	    # aip

        # ama            
        if ('ClockssHighWirePlugin' in auid or 'ClockssHighWirePressH20Plugin' in auid) and ('ama-assn' in auid):
            pubyear['ama'][strYear] += articles[0]
            total['ama'] += articles[0]
 
        # aps           
        if ('ClockssHighWirePlugin' in auid or 'ClockssHighWirePressH20Plugin' in auid) and ('physiology%2Eorg' in auid):
            pubyear['aps'][strYear] += articles[0]
            total['aps'] += articles[0]

	    # acm

        # bep            
        if 'ClockssBerkeleyElectronicPressPlugin' in auid:
            pubyear['bep'][strYear] += articles[0]
            total['bep'] += articles[0]
            
        # bmc

        # cap
        if 'ClockssCoActionPublishingPlugin' in auid:
            pubyear['cap'][strYear] += articles[0]
            total['cap'] += articles[0]

        # eup
        if 'ClockssEdinburghUniversityPressPlugin' in auid:
            pubyear['eup'][strYear] += articles[0]
            total['eup'] += articles[0]

        # elsevier
        
        # gtv
        if 'ClockssGeorgThiemeVerlagPlugin' in auid:
            pubyear['gtv'][strYear] += articles[0]
            total['gtv'] += articles[0]	    

        # iop
        
        # wiley
        
        # lup
        
        # npg
        if 'ClockssNaturePublishingGroupPlugin' in auid:
            pubyear['npg'][strYear] += articles[0]
            total['npg'] += articles[0]
        
        # oup
        if ('HighWire' in auid) and ('oxfordjournals' in auid):
            pubyear['oup'][strYear] += articles[0]
            total['oup'] += articles[0]

        # rup
        if ('ClockssHighWirePressH20Plugin' in auid) and ('rupress%2Eorg' in auid):
            pubyear['rup'][strYear] += articles[0]
            total['rup'] += articles[0]
            
        # rsc
        if 'ClockssRoyalSocietyOfChemistryPlugin' in auid:
            pubyear['rsc'][strYear] += articles[0]
            total['rsc'] += articles[0]
            
        # rsp
        if ('HighWire' in auid) and ('royalsocietypublishing' in auid):
            pubyear['rsp'][strYear] += articles[0]
            total['rsp'] += articles[0]

        # sage
        if ('HighWire' in auid) and ('sagepub' in auid):
            pubyear['sage'][strYear] += articles[0]
            total['sage'] += articles[0]

        # springer
        
        # ssr
        if ('HighWire' in auid) and ('biolreprod%2Eorg' in auid):
            pubyear['ssr'][strYear] += articles[0]
            total['ssr'] += articles[0]

        # tf

        auidrow = cursorAuid.fetchone()  

    # Verify our numbers!
    for publisher in publishers:
        testTotal = 0
        
        for year in range(options.currentyear, options.minimumyear - 1, -1) + ["0"]:
            strYear = str(year)
            testTotal += pubyear[publisher][strYear]

        if total[publisher] != testTotal:
            print "ERROR: Publisher " + publisher + " doesn't have the right total."


    # Post the report to the database
    for publisher in publishers:
        for year in range(options.currentyear, options.minimumyear - 1, -1) + [0]:
            strYear = str(year)
            cursorAuid.execute("INSERT INTO burpreport(rundate, publisher, auyear, numarticles) VALUES (NOW(), \"%s\", %d, %d);" %
                               (publisher, year, pubyear[publisher][strYear]))

    # Output the main report.
    filename = open(options.filename, 'w')
    filename.write("Dates of ingest," + str(options.reportdatestart) + " - " + str(options.reportdateend) + "\n")
    filename.write("Date of report," + str(date.today()) + "\n")
    filename.write("year,")
    for publisher in publishers:
        filename.write(publisher.upper() + ",")
    # Yes, there's an extra comma at the end.  Same with everything else.
    # If that's a problem, we can fix it.
    filename.write("\n"); 

    for year in range(options.currentyear, options.minimumyear - 1, -1) + ["0"]:
        strYear = str(year)
        printYear = strYear
        if (year == options.currentyear):
            printYear = "Current Ingest " + strYear
        if (year == "0"):
            printYear = "Data entry in progress -- Year TBD"
        filename.write( printYear + "," )

        for publisher in publishers:
            filename.write(str(pubyear[publisher][strYear]) + ",")
        filename.write("\n")

    filename.write("Back Ingest Total,")
    for publisher in publishers:
        filename.write(str(total[publisher]) + ",")
    filename.write("\n")

    # Output the new summary report.
    # WARNING: I don't have a simple way to keep track of names compared
    # against publishers.  You need to update these in two places: the
    # list called 'publishers' and here.
    
    currentyear = str(options.currentyear)
    summary = open(options.summary, "w")

    summary.write("Official Publisher Name,Publisher ID, Total Ingest for " + str(currentyear) + ", Total Ingest For all time\n")
    
    _print_summary_line(summary, "American Academy of Pediatrics", "aap", currentyear, pubyear, total)
    _print_summary_line(summary, "American Institute of Physics", "aip", currentyear, pubyear, total)
    _print_summary_line(summary, "American Medical Association", "ama", currentyear, pubyear, total)
    _print_summary_line(summary, "American Physiological Society", "aps", currentyear, pubyear, total)
    _print_summary_line(summary, "Association for Computing Machinery", "acm", currentyear, pubyear, total)
    _print_summary_line(summary, "Berkeley Electronic Press", "bep", currentyear, pubyear, total)
    _print_summary_line(summary, "BioMed Central", "bmc", currentyear, pubyear, total)
    _print_summary_line(summary, "Co-Action Publishing", "cap", currentyear, pubyear, total)
    _print_summary_line(summary, "Edinburgh University Press", "eup", currentyear, pubyear, total)
    _print_summary_line(summary, "Elsevier", "elsevier", currentyear, pubyear, total)
    _print_summary_line(summary, "Georg Thieme Verlag", "gtv", currentyear, pubyear, total)
    _print_summary_line(summary, "IOP Publishing", "iop", currentyear, pubyear, total)
    _print_summary_line(summary, "John Wiley and Sons", "wiley", currentyear, pubyear, total)
    _print_summary_line(summary, "Liverpool University Press", "lup", currentyear, pubyear, total)
    _print_summary_line(summary, "Nature Publishing Group", "npg", currentyear, pubyear, total)
    _print_summary_line(summary, "Oxford University Press", "oup", currentyear, pubyear, total)
    _print_summary_line(summary, "Rockefeller University Press", "rup", currentyear, pubyear, total)
    _print_summary_line(summary, "RSC Publishing", "rsc", currentyear, pubyear, total)
    _print_summary_line(summary, "Royal Society Publishing", "rsp", currentyear, pubyear, total)
    _print_summary_line(summary, "SAGE Publications", "sage", currentyear, pubyear, total)
    _print_summary_line(summary, "Springer", "springer", currentyear, pubyear, total)
    _print_summary_line(summary, "Society for the Study of Reproduction", "ssr", currentyear, pubyear, total)
    _print_summary_line(summary, "Taylor and Francis", "tf", currentyear, pubyear, total)


if __name__ == '__main__':    
    _main_procedure()
