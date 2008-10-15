#!/usr/bin/python

# $Id: tdbproc.py,v 1.1 2008-10-15 00:59:53 thib_gc Exp $
#
# Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

from optparse import OptionParser

from tdbconst import *

OPTION_LONG        = '--'
OPTION_SHORT       = '-'

OPTION_STYLE_SHORT = 's'

def __make_command_line_parser():
    '''Builds a new command line option parser'''
    parser = OptionParser()
#    parser.add_option(OPTION_SHORT + OPTION_DEBUG_SHORT,
#                      OPTION_LONG + OPTION_DEBUG,
#                      dest=OPTION_DEBUG,
#                      action='store_true',
#                      help='show debug output')
#    parser.set_default(OPTION_DEBUG, False)
#    parser.add_option(OPTION_SHORT + OPTION_LEVEL_SHORT,
#                      OPTION_LONG + OPTION_LEVEL,
#                      dest=OPTION_LEVEL,
#                      action='store',
#                      choices=LEVELS,
#                      help='output level (default: %default)')
#    parser.add_option(OPTION_SHORT + OPTION_QUIET_SHORT,
#                      OPTION_LONG + OPTION_QUIET,
#                      dest=OPTION_QUIET,
#                      action='store_true',
#                      help='be quiet')
#    parser.set_default(OPTION_QUIET, False)
    parser.add_option(OPTION_SHORT + OPTION_STYLE_SHORT,
                      OPTION_LONG + OPTION_STYLE,
                      dest=OPTION_STYLE,
                      action='store',
                      choices=STYLES,
                      help='output style (default: %default)')
    parser.set_default(OPTION_STYLE, STYLE_DEFAULT)
    return parser

def __dispatch(tdb, options):
    if options.style in [ STYLE_XML, STYLE_XML_ENTRIES, STYLE_XML_LEGACY ]:
        from tdbxml import tdb_to_xml
        tdb_to_xml(tdb, options)
    else:
        print 'no output'
        

if __name__ == '__main__':
    from tdbparse import TdbScanner, TdbParser
    from sys import stdin, argv
    parser = __make_command_line_parser()
    (options, argv[1:]) = parser.parse_args(values=parser.get_default_values())
    scanner = TdbScanner(stdin)
    parser = TdbParser(scanner)
    tdb = parser.parse()
    __dispatch(tdb, options)
