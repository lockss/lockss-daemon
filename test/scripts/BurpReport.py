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

from datetime import date
import MySQLdb
import optparse
import os
import sys

# Constants
OPTION_BURP_VERSION             = '0.2.1'

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
DEFAULT_DATE_REPORT             = date.today()

OPTION_DATE_REPORT_END          = 'reportdateend'
OPTION_DATE_REPORT_END_SHORT    = 'e'
DEFAULT_DATE_REPORT_END         = date.today()

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

def _print_summary_line(summary, publishername, publisherid, currentyear, pubyear, total):
    summary.write(publishername + "," + publisherid.upper() + ",")
    summary.write(str(pubyear[publisherid][currentyear]) + ",")
    summary.write(str(total[publisherid]) + "\n")

def _main_procedure():
    parser = _make_command_line_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    _check_required_options(parser, options)

    # Initialize the hashes.
    # WARNING: If you update this list, you need to update the
    # summary report as well.
    publishers = ['aap', 'aip', 'ama', 'aps', 'acm', 'bep', 'bmc', 'eup', 'elsevier', 'iop', 'wiley', 'lup', 'npg', 'oup', 'rsc', 'sage', 'springer', 'tf', 'rup', 'rs']

    total = {}
    pubyear = {}

    for publisher in publishers:
        pubyear[publisher] = {}
        total[publisher] = 0

        for year in range(options.currentyear, options.minimumyear - 1, -1) + ["0"]:
            strYear = str(year)
            pubyear[publisher][strYear] = 0

    db = MySQLdb.connect(host="localhost", user="edwardsb", passwd=options.dbpassword, db="burp")

    cursorAuid = db.cursor()
    cursorAuid.execute("SELECT DISTINCT(auid) from burp WHERE rundate >= DATE('" + str(options.reportdatestart) + "') AND rundate <= DATE('" + str(options.reportdateend) + "') order by auid;")
    
    auid = cursorAuid.fetchone()
    while auid is not None:
        cursorArticles = db.cursor()
        cursorArticles.execute("SELECT MAX(numarticles), auyear FROM burp WHERE auid = \"" + auid[0] + "\" and rundate >= DATE('" + str(options.reportdatestart) + "') AND rundate <= DATE('" + str(options.reportdateend) +"');")
        articles = cursorArticles.fetchone()
        strYear = articles[1]
        if "-" in strYear:
            strYear = strYear[5:9]
        
        if (auid[0].find("ClockssHighWirePlugin") != -1) and (auid[0].find("aappublications") != -1):
            pubyear['aap'][strYear] += articles[0]
            total['aap'] += articles[0]
            
        if (auid[0].find("ClockssHighWirePlugin") != -1) and (auid[0].find("ama-assn") != -1):
            pubyear['ama'][strYear] += articles[0]
            total['ama'] += articles[0]
            
        if (auid[0].find("ClockssHighWirePlugin") != -1) and (auid[0].find("physiology%2Eorg") != -1):
            pubyear['aps'][strYear] += articles[0]
            total['aps'] += articles[0]
            
        if (auid[0].find("ClockssBerkeleyElectronicPressPlugin") != -1):
            pubyear['bep'][strYear] += articles[0]
            total['bep'] += articles[0]
            
        if (auid[0].find("ClockssNaturePublishingGroupPlugin") != -1):
            pubyear['npg'][strYear] += articles[0]
            total['npg'] += articles[0]
            
        if (auid[0].find("HighWire") != -1) and (auid[0].find("oxfordjournals") != -1):
            pubyear['oup'][strYear] += articles[0]
            total['oup'] += articles[0]
            
        if (auid[0].find("ClockssRoyalSocietyOfChemistryPlugin") != -1):
            pubyear['rsc'][strYear] += articles[0]
            total['rsc'] += articles[0]
            
        if (auid[0].find("HighWire") != -1) and (auid[0].find("sagepub") != -1):
            pubyear['sage'][strYear] += articles[0]
            total['sage'] += articles[0]
        
        auid = cursorAuid.fetchone()  

    # OUP has some AUIDs that are listed as year 0.  This code assumes that the
    # titles are split between 2008 and 2009 in the same proportion.
#    oup0 = float(pubyear['oup']["0"])

#    if oup0 > 0 and pubyear['oup']['2009'] > 0 and pubyear['oup']['2008'] > 0:
#        pubyear['oup']["2009"] += int(oup0 * (74.0 / 104.0) + 0.5)
#        pubyear['oup']["2008"] += int(oup0 * (30.0 / 104.0) + 0.5)
#        pubyear['oup']["0"] = 0

#    aps0 = float(pubyear['aps']["0"])

#    if aps0 > 0:
#        pubyear['aps']['2010'] += aps0
#        pubyear['aps']['0'] = 0

    # Output the main report.
    filename = open(options.filename, 'w')
    filename.write("Date of ingest," + str(options.reportdatestart) + "\n")
    filename.write("Date of report," + str(date.today()) + "\n")
    filename.write("year,")
    for publisher in publishers:
        filename.write(publisher.upper() + ",")
    # Yes, there's an extra comma at the end.  Same with everything else.
    # If that's a problem, we can fix it.
    filename.write("\n"); 

    for year in range(options.currentyear, options.minimumyear, -1) + ["0"]:
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
    _print_summary_line(summary, "Edinburgh University Press", "eup", currentyear, pubyear, total)
    _print_summary_line(summary, "Elsevier", "elsevier", currentyear, pubyear, total)
    _print_summary_line(summary, "IOP Publishing", "iop", currentyear, pubyear, total)
    _print_summary_line(summary, "John Wiley and Sons", "wiley", currentyear, pubyear, total)
    _print_summary_line(summary, "Liverpool University Press", "lup", currentyear, pubyear, total)
    _print_summary_line(summary, "Nature Publishing Group", "npg", currentyear, pubyear, total)
    _print_summary_line(summary, "Oxford University Press", "oup", currentyear, pubyear, total)
    _print_summary_line(summary, "RSC Publishing", "rsc", currentyear, pubyear, total)
    _print_summary_line(summary, "Sage", "sage", currentyear, pubyear, total)
    _print_summary_line(summary, "Springer", "springer", currentyear, pubyear, total)
    _print_summary_line(summary, "Taylor and Francis", "tf", currentyear, pubyear, total)
    _print_summary_line(summary, "The Rockefeller University Press", "rup", currentyear, pubyear, total)
    _print_summary_line(summary, "The Royal Society", "rs", currentyear, pubyear, total)


if __name__ == '__main__':    
    _main_procedure()
