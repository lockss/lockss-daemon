#! /usr/bin/env python

# A very simple Python program that inserts a new start date time
# if no start date has already been inserted.

import MySQLdb
import optparse

OPTION_STARTCHECK_VERSION          = '0.0.0'

OPTION_SHORT                    = '-'
OPTION_LONG                     = '--'

OPTION_DATABASE_PASSWORD               = 'dbpassword'
OPTION_DATABASE_PASSWORD_SHORT         = 'D'

def _make_command_line_parser():
    from optparse import OptionGroup, OptionParser

    parser = OptionParser(version=OPTION_STARTCHECK_VERSION)
    
    parser.add_option(OPTION_SHORT + OPTION_DATABASE_PASSWORD_SHORT,
                      OPTION_LONG + OPTION_DATABASE_PASSWORD,
                      dest=OPTION_DATABASE_PASSWORD,
                      help="The password for the database (required)")

    return parser


def _check_required_options(parser, options):
    if options.dbpassword is None:
        parser.error('%s%s/%s%s is required' % (OPTION_LONG, OPTION_DATABASE_PASSWORD, OPTION_SHORT, OPTION_DATABASE_PASSWORD_SHORT))


def _main_procedure():
    parser = _make_command_line_parser()
    (options, args) = parser.parse_args(values=parser.get_default_values())
    _check_required_options(parser, options)
    
    db = MySQLdb.connect(host="localhost", user="edwardsb", passwd=options.dbpassword, db="burp")
    
    cursor = db.cursor()
    
    cursor.execute("SELECT COUNT(*) FROM executions WHERE end IS NULL;")
    testrun = cursor.fetchone()
    
    if testrun[0] == 0:
        cursor.execute("INSERT INTO executions(start, end) VALUES (NOW(), NULL);")
        
    cursor.close()

if __name__ == '__main__':    
    _main_procedure()

    